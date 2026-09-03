package com.manish.smartcart.product.service;

import com.manish.smartcart.infrastructure.storage.CloudinaryService;
import com.manish.smartcart.product.dto.ProductRequest;
import com.manish.smartcart.product.dto.ProductResponse;
import com.manish.smartcart.product.dto.ProductSearchDTO;
import com.manish.smartcart.infrastructure.ai.EmbeddingService;
import com.manish.smartcart.product.dto.SemanticSearchResponse;
import com.manish.smartcart.product.model.*;
import com.manish.smartcart.product.repository.ProductModerationHistoryRepository;
import com.manish.smartcart.seller.repository.SellerProfileRepository;
import com.manish.smartcart.shared.enums.KycStatus;
import com.manish.smartcart.shared.enums.product.ActorType;
import com.manish.smartcart.shared.enums.product.LifecycleStatus;
import com.manish.smartcart.shared.enums.product.ModerationAction;
import com.manish.smartcart.shared.enums.product.ProductApprovalStatus;
import com.manish.smartcart.shared.exception.ProductStateTransitionException;
import com.manish.smartcart.shared.mapper.ProductMapper;
import com.manish.smartcart.product.repository.CategoryRepository;
import com.manish.smartcart.product.repository.ProductRepository;
import com.manish.smartcart.product.repository.ProductVariantRepository;
import com.manish.smartcart.product.repository.specifications.ProductSpecifications;
import com.manish.smartcart.shared.util.VectorAttributeConverter;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.user.model.SellerProfile;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final EmbeddingService embeddingService;
    private final SellerProfileRepository sellerProfileRepository;
    private final ProductModerationHistoryRepository productModerationHistoryRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * ACTIVITY: Onboarding (Creation)
     * Handles Slug and SKU generation automatically.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product-slug", allEntries = true)
    })
    public ProductResponse createProduct(ProductRequest productRequest, Long currentSellerId) {

        // 1. KYC SECURITY LOCK (EDGE CASE: Block unverified sellers)
        SellerProfile seller = sellerProfileRepository.findById(currentSellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller Profile" + "id #" + currentSellerId));

        if (seller.getKycStatus() != KycStatus.VERIFIED) {
            throw new BusinessLogicException("KYC Enforcement: Your profile is currently "
                    + seller.getKycStatus() + ". You must be VERIFIED to list products.");
        }

        // 2. Map core fields (Mapper will handle standard fields)
        Product product = productMapper.toProduct(productRequest);
        product.setSellerId(currentSellerId);

        // 3. Resolve Category
        Category category = null;
        if (productRequest.getCategoryPublicId() != null) {
            category = categoryRepository.findByPublicId(productRequest.getCategoryPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        }

        // 4. Generate Concurrency-Safe Slug
        product.setSlug(generateUniqueSlug(productRequest.getProductName()));

        // 5. State Machine Initialization
        if (Boolean.TRUE.equals(productRequest.getIsDraft())) {
            product.setLifecycleStatus(LifecycleStatus.DRAFT);
            product.setIsAvailable(false);
        } else {
            // If they didn't mark it as a draft, push it into the moderation queue
            product.setLifecycleStatus(LifecycleStatus.INACTIVE);
            product.setApprovalStatus(ProductApprovalStatus.PENDING_REVIEW);
            product.setIsAvailable(false); // Must remain false until Admin sets to APPROVED
        }

        // 6. Save Product Parent First (Required to get DB ID for variants and
        // embeddings)
        Product savedProduct = productRepository.save(product);

        // 7. Dynamic Variant Processing
        if (productRequest.getVariants() == null || productRequest.getVariants().isEmpty()) {
            throw new BusinessLogicException("A product must contain at least one variant (e.g. 'Standard').");
        }

        List<ProductVariant> variantsToSave = new ArrayList<>();
        String catName = category != null ? category.getName() : "PRD";

        for (var vReq : productRequest.getVariants()) {
            // Use provided SKU or auto-generate a collision-proof one
            String sku = (vReq.getSku() == null || vReq.getSku().isBlank())
                    ? generateUniqueSku(catName)
                    : vReq.getSku();

            // Edge Case: If seller provided a SKU, ensure it doesn't already exist globally
            if (vReq.getSku() != null && productVariantRepository.existsBySku(vReq.getSku())) {
                throw new BusinessLogicException("The SKU '" + vReq.getSku() + "' is already in use.");
            }

            ProductVariant variant = ProductVariant.builder()
                    .product(savedProduct)
                    .sku(sku)
                    .stockQuantity(vReq.getStockQuantity())
                    .reservedQuantity(0) // Always 0 on creation
                    .lowStockThreshold(vReq.getLowStockThreshold())
                    .priceModifier(vReq.getPriceModifier() != null ? vReq.getPriceModifier() : BigDecimal.ZERO)
                    .compareAtPrice(vReq.getCompareAtPrice())
                    .attributes(vReq.getAttributes())
                    .weight(vReq.getWeight())
                    .lengthCm(vReq.getLengthCm())
                    .widthCm(vReq.getWidthCm())
                    .heightCm(vReq.getHeightCm())
                    .sortOrder(vReq.getSortOrder())
                    .isActive(true)
                    .build();
            variantsToSave.add(variant);
        }

        productVariantRepository.saveAll(variantsToSave);
        log.info("Saved {} variants for product ID {}", variantsToSave.size(), savedProduct.getId());

        try {
            String textToEmbed = buildEmbeddingText(savedProduct);
            float[] embedding = embeddingService.generateEmbedding(textToEmbed);

            // Convert float[] → "[0.021,-0.455,...]" using our VectorAttributeConverter
            String vectorString = new VectorAttributeConverter()
                    .convertToDatabaseColumn(embedding);
            // Native UPDATE with explicit CAST — bypasses JPA's VARCHAR binding
            productRepository.updateEmbedding(savedProduct.getId(), vectorString);
            log.info("✅ Embedding saved for product ID {}", savedProduct.getId());

        } catch (Exception e) {
            // CONCEPT: We catch and log but do NOT re-throw.
            // If OpenAI is temporarily down, product creation still succeeds.
            // The embedding column will be NULL — our backfill scheduler (future) fills it
            // later.
            log.warn("⚠️ Could not generate embedding for product '{}': {}",
                    savedProduct.getProductName(), e.getMessage());
        }

        return productMapper.toProductResponse(savedProduct);
    }

    /**
     * ACTIVITY: Update Product
     * Updates basic product details.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product-slug", allEntries = true)
    })
    public ProductResponse updateProduct(UUID productPublicId, ProductRequest productRequest, Long currentSellerId) {
        Product product = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productPublicId));

        if (!product.getSellerId().equals(currentSellerId)) {
            throw new BusinessLogicException("Access Denied: You do not have permission to modify this product.");
        }

        // SECURITY CHECK: Lock edits if under active admin review
        if (product.getApprovalStatus() == ProductApprovalStatus.PENDING_REVIEW) {
            throw new BusinessLogicException(
                    "Cannot update product while it is actively under Admin review. Please wait for approval/rejection.");
        }
        if (product.getLifecycleStatus() == LifecycleStatus.ARCHIVED) {
            throw new ProductStateTransitionException("Archived products are locked and cannot be modified.");
        }

        // Apply Basic Fields
        if (productRequest.getProductName() != null)
            product.setProductName(productRequest.getProductName());
        if (productRequest.getDescription() != null)
            product.setDescription(productRequest.getDescription());
        if (productRequest.getPrice() != null)
            product.setPrice(productRequest.getPrice());
        if (productRequest.getDiscountPrice() != null)
            product.setDiscountPrice(productRequest.getDiscountPrice());
        if (productRequest.getTags() != null)
            product.setTags(productRequest.getTags());

        // Apply V4.2 Domain Fields
        if (productRequest.getCountryOfOrigin() != null)
            product.setCountryOfOrigin(productRequest.getCountryOfOrigin());
        if (productRequest.getCondition() != null)
            product.setCondition(productRequest.getCondition());
        if (productRequest.getProductType() != null)
            product.setProductType(productRequest.getProductType());
        if (productRequest.getAttributes() != null)
            product.setAttributes(productRequest.getAttributes());

        if (productRequest.getCategoryPublicId() != null) {
            Category category = categoryRepository.findByPublicId(productRequest.getCategoryPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with Public ID: " + productRequest.getCategoryPublicId()));
            product.setCategory(category);
        }

        // Note: Variant/Media updates bypass this method. They have their own dedicated
        // endpoints (ProductVariantController).

        Product savedProduct = productRepository.save(product);
        return productMapper.toProductResponse(savedProduct);
    }

    /**
     * ACTIVITY: Admin Moderation
     * Admins review products in PENDING_REVIEW and update their status (Approve,
     * Reject, or Request Changes).
     * This securely writes an immutable record to the ProductModerationHistory
     * ledger for audit purposes.
     *
     * @param productPublicId The public UUID of the product.
     * @param newStatus       The moderation decision (APPROVED, REJECTED,
     *                        REQUIRES_CHANGES).
     * @param reason          The justification for rejection or required changes.
     * @param adminId         The ID of the administrator performing the action.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product-slug", allEntries = true)
    })
    public void moderateProduct(UUID productPublicId, ProductApprovalStatus newStatus, String reason, Long adminId) {
        log.info("Admin {} initiating moderation action '{}' for product {}", adminId, newStatus, productPublicId);

        Product product = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> {
                    log.error("Moderation failed: Product {} not found.", productPublicId);
                    return new ResourceNotFoundException("Product not found with ID: " + productPublicId);
                });

        // 1. Validation: Force admins to leave a paper trail if denying a product
        if ((newStatus == ProductApprovalStatus.REJECTED || newStatus == ProductApprovalStatus.REQUIRES_CHANGES)
                && (reason == null || reason.isBlank())) {
            log.warn("Moderation blocked: Admin {} attempted to reject product {} without providing a reason.", adminId,
                    product.getId());
            throw new BusinessLogicException(
                    "A detailed reason must be provided when rejecting or requesting changes to a product.");
        }

        ProductApprovalStatus oldStatus = product.getApprovalStatus();
        product.setApprovalStatus(newStatus);

        // 2. State Machine Auto-Transitions based on decision
        if (newStatus == ProductApprovalStatus.APPROVED) {
            product.setLifecycleStatus(LifecycleStatus.ACTIVE);
            product.setIsAvailable(true); // Product goes live immediately
            log.info("Product '{}' (ID: {}) approved and activated.", product.getProductName(), product.getId());
        } else {
            product.setLifecycleStatus(LifecycleStatus.INACTIVE);
            product.setIsAvailable(false); // Hard-hide from storefront
            log.info("Product '{}' (ID: {}) hidden from storefront due to status: {}", product.getProductName(),
                    product.getId(), newStatus);
        }

        productRepository.save(product);

        // 3. Map the approval status to the ledger ModerationAction enum
        ModerationAction actionToLog = ModerationAction.REQUESTED_CHANGES;
        if (newStatus == ProductApprovalStatus.APPROVED)
            actionToLog = ModerationAction.APPROVED;
        if (newStatus == ProductApprovalStatus.REJECTED)
            actionToLog = ModerationAction.REJECTED;

        // 4. Immutable Audit Ledger Entry
        ProductModerationHistory history = ProductModerationHistory.builder()
                .product(product)
                .adminId(adminId)
                .actorType(ActorType.ADMIN) // Strict Enum
                .action(actionToLog) // Strict Enum
                .approvalStatusFrom(oldStatus) // Strict Enum
                .approvalStatusTo(newStatus) // Strict Enum
                .reason(reason)
                .build();

        productModerationHistoryRepository.save(history);
        log.info("Successfully recorded moderation history ledger for product ID: {}", product.getId());
    }

    /**
     * ACTIVITY: Seller Submission
     * Sellers submit DRAFT or REQUIRES_CHANGES products into the Admin Moderation
     * Queue.
     *
     * @param productPublicId The public UUID of the product being submitted.
     * @param sellerId        The ID of the authenticated seller making the request.
     */
    @Transactional
    public void submitForReview(UUID productPublicId, Long sellerId) {
        log.info("Seller {} attempting to submit product {} for moderation review.", sellerId, productPublicId);

        Product product = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> {
                    log.error("Submission failed: Product {} not found.", productPublicId);
                    return new ResourceNotFoundException("Product not found with ID: " + productPublicId);
                });

        // 1. Ownership Validation
        if (!product.getSellerId().equals(sellerId)) {
            log.warn("Security Alert: Seller {} attempted to submit product {} which they do not own.", sellerId,
                    product.getId());
            throw new BusinessLogicException("Access Denied: You do not own this product.");
        }

        // 2. Edge Case Guard: Block sellers from re-submitting Active or permanently
        // Rejected products
        if (product.getLifecycleStatus() != LifecycleStatus.DRAFT
                && product.getApprovalStatus() != ProductApprovalStatus.REQUIRES_CHANGES) {
            log.warn("Invalid transition: Seller {} tried to submit product {} which is in status {}/{}",
                    sellerId, product.getId(), product.getLifecycleStatus(), product.getApprovalStatus());
            throw new ProductStateTransitionException(
                    "Only DRAFT or REQUIRES_CHANGES products can be submitted to the moderation queue.");
        }

        ProductApprovalStatus oldStatus = product.getApprovalStatus();

        // 3. Update state to place it into the queue
        product.setLifecycleStatus(LifecycleStatus.INACTIVE);
        product.setApprovalStatus(ProductApprovalStatus.PENDING_REVIEW);
        product.setIsAvailable(false); // Ensure it remains hidden until an admin approves it

        productRepository.save(product);

        // 4. Log to immutable ledger
        ProductModerationHistory history = ProductModerationHistory.builder()
                .product(product)
                .adminId(null) // Action taken by seller, so no admin ID
                .actorType(ActorType.SELLER) // Strict Enum
                .action(ModerationAction.SUBMITTED) // Strict Enum
                .approvalStatusFrom(oldStatus) // Strict Enum
                .approvalStatusTo(ProductApprovalStatus.PENDING_REVIEW) // Strict Enum
                .reason("Seller submitted product for administrative review.")
                .build();

        productModerationHistoryRepository.save(history);
        log.info("Product '{}' (ID: {}) successfully queued for admin review by seller {}.", product.getProductName(),
                product.getId(), sellerId);
    }

    /**
     * ACTIVITY: Visibility Control
     * Toggle availability for a product (e.g., if it's discontinued).
     * True --> false or false -> true
     * Logic ensures only the owner or an admin can hide/show the product.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product-slug", allEntries = true)
    })
    public void toggleAvailability(UUID productPublicId, Long currentSellerId, boolean isAdmin) {
        Product product = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productPublicId));

        // Security Check: Unauthorized if not Admin and not the Owner
        if (!isAdmin && !product.getSellerId().equals(currentSellerId)) {
            throw new BusinessLogicException("Access Denied: You do not have permission to modify this product.");
        }
        product.setIsAvailable(!product.getIsAvailable());
        productRepository.save(product);
    }

    /**
     * Stock management has moved to the Variant layer.
     * Use ProductVariantService.updateVariantStock(variantId, quantityChange,
     * sellerId, isAdmin)
     * which operates on ProductVariant.stockQuantity — the correct inventory field.
     *
     * @deprecated Since product-variant migration. Use variant-level stock
     *             management instead.
     */
    @Deprecated
    public Product updateStock(Long productId, Integer quantityChange, Long currentSellerId, boolean isAdmin) {
        throw new UnsupportedOperationException(
                "Stock management has moved to the variant layer. " +
                        "Use PATCH /api/v1/products/{productId}/variants/{variantId}/stock instead.");
    }

    /**
     * ACTIVITY: Discovery (Basic Retrieval)
     * Phase 1: Simple retrieval methods. We will add Pagination & Filtering later.
     */

    @Transactional(readOnly = true)
    // Removed @Cacheable for paginated dynamic fetching
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(productMapper::toProductResponse);
    }

    /**
     * ACTIVITY: Discovery (Fetch by Category)
     * Returns DTOs (not raw entities) so Redis can serialize them safely.
     */
    @Transactional(readOnly = true)
    // Removed @Cacheable for paginated dynamic fetching
    public Page<ProductResponse> getProductsByCategoryIds(List<Long> categoryId, Pageable pageable) {
        return productRepository.findByCategoryIdIn(categoryId, pageable)
                .map(productMapper::toProductResponse);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // NEW: Phase 11 Admin Moderation Dashboard Methods
    // ════════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsPendingReview(Pageable pageable) {
        return productRepository.findByApprovalStatus(ProductApprovalStatus.PENDING_REVIEW, pageable)
                .map(productMapper::toProductResponse);
    }

    @Transactional(readOnly = true)
    public List<com.manish.smartcart.product.dto.ProductModerationHistoryResponse> getProductModerationHistory(UUID productPublicId) {
        Product product = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productPublicId));

        return productModerationHistoryRepository.findByProductIdOrderByCreatedAtDesc(product.getId())
                .stream()
                .map(history -> com.manish.smartcart.product.dto.ProductModerationHistoryResponse.builder()
                        .actorType(history.getActorType().name())
                        .action(history.getAction().name())
                        .approvalStatusFrom(history.getApprovalStatusFrom() != null ? history.getApprovalStatusFrom().name() : null)
                        .approvalStatusTo(history.getApprovalStatusTo().name())
                        .reason(history.getReason())
                        .createdAt(history.getCreatedAt())
                        .build())
                .toList();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // UPDATE — semanticSearch now returns scored results + supports filters
    // ════════════════════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public SemanticSearchResponse semanticSearch(
            String query, int limit,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating) {

        // Step 1: Convert the user's plain English query into a 384-dimensional vector
        // Same model used when products were indexed → comparable vector space
        // Format float[] → "[0.021,-0.455,...]" for the native SQL CAST
        float[] queryVector = embeddingService.generateEmbedding(query);
        String vectorString = new VectorAttributeConverter().convertToDatabaseColumn(queryVector);

        // Step 2: Fetch ranked results (with optional price/rating filters)
        // Each row is [product columns..., distance]
        boolean hasFilter = minPrice != null || maxPrice != null || minRating != null;

        List<Object[]> rawResults = hasFilter
                ? productRepository.findBySimilarityWithFilters(vectorString, limit, minPrice, maxPrice, minRating)
                : productRepository.findBySimilarity(vectorString, limit);

        // Step 3: Convert raw rows → RankedProduct DTOs with relevance scores
        List<SemanticSearchResponse.RankedProduct> ranked = new ArrayList<>();
        for (Object[] row : rawResults) {
            // The last column in the SELECT is the cosine distance
            // distance = 0.0 means identical, 1.0 means completely unrelated
            double distance = ((Number) row[row.length - 1]).doubleValue();

            // Convert distance → similarity score (higher = better match)
            // similarity = 1 - distance (so distance 0.05 → similarity 0.95 = 95%)
            double similarity = Math.max(0.0, 1.0 - distance);

            // Map the SQL row back to a Product entity, then to a DTO
            // We reload from the repository to get fully hydrated Hibernate entity
            Long productId = ((Number) row[0]).longValue();

            productRepository.findById(productId).ifPresent(product -> {
                ranked.add(SemanticSearchResponse.RankedProduct.builder()
                        .product(productMapper.toProductResponse(product))
                        .relevanceScore(similarity)
                        .relevanceLabel(String.format("%.0f%%", similarity * 100))
                        .rank(ranked.size() + 1)
                        .build());
            });

        }
        return SemanticSearchResponse.builder()
                .query(query)
                .totalFound(ranked.size())
                .results(ranked)
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // NEW — backfill embeddings for all products that are missing one
    // ════════════════════════════════════════════════════════════════════════════
    /**
     * WHY: Products created before the HuggingFace system was live have
     * NULL in the embedding column. They are completely invisible to AI search.
     * This method fetches all of them and generates their embeddings now.
     * <p>
     * Called by: POST /api/v1/products/admin/reindex (manual trigger)
     */
    @Transactional
    public int reindexMissingEmbeddings() {
        List<Product> unindexed = productRepository.findAllWithNullEmbedding();
        log.info("Starting embedding backfill for {} products", unindexed.size());

        int successCount = 0;

        for (Product product : unindexed) {
            try {
                // Build the rich embedding text (same format as new products)
                String text = buildEmbeddingText(product);
                float[] embedding = embeddingService.generateEmbedding(text);
                String vectorString = new VectorAttributeConverter().convertToDatabaseColumn(embedding);
                productRepository.updateEmbedding(product.getId(), vectorString);
                successCount++;
                log.info("✅ Reindexed product '{}' (id={})", product.getProductName(), product.getId());
            } catch (Exception e) {
                // Don't stop the whole batch if one product fails
                log.warn("⚠️ Failed to reindex '{}': {}", product.getProductName(), e.getMessage());
            }
        }
        log.info("Reindex complete: {}/{} products indexed", successCount, unindexed.size());
        return successCount;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "product-slug", key = "#p0")
    public ProductResponse getProductByIdentifier(String identifier) {
        Product product;
        try {
            java.util.UUID publicId = java.util.UUID.fromString(identifier);
            product = productRepository.findByPublicId(publicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found for publicId: " + identifier));
        } catch (IllegalArgumentException e) {
            product = productRepository.findBySlug(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found for slug: " + identifier));
        }
        return productMapper.toProductResponse(product);
    }

    /**
     * Delete product by using product id.
     * Done by seller or Admin only
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product-slug", allEntries = true)
    })
    public void deleteProduct(UUID productPublicId, Long authenticatedUserId, boolean isAdmin) {
        // Combined exists and find into one call for efficiency
        Product product = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productPublicId));

        if (!isAdmin && !product.getSellerId().equals(authenticatedUserId)) {
            throw new BusinessLogicException("Access Denied: Only owners or admins can delete products.");
        }
        productRepository.deleteById(product.getId());
    }

    // Filter the product using specification
    @Transactional(readOnly = true)
    public Page<ProductResponse> getFilteredProduct(ProductSearchDTO searchDTO, Pageable pageable) {

        // 1. Initialize an empty Specification (the "base" query)
        Specification<Product> specs = (root, query, cb) -> cb.conjunction();

        // 2. Dynamically "chain" filters only if they are provided in the DTO
        if (searchDTO.getCategory() != null && !searchDTO.getCategory().isEmpty()) {
            // 1. Find the parent category by name (as sent in search)
            Category parentCategory = categoryRepository.findByNameIgnoreCase(searchDTO.getCategory())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + searchDTO.getCategory()));
            // 2. Use your CategoryService to get the whole tree of IDs
            List<Long> categoryIds = categoryService.getAllChildCategoryIds(parentCategory.getId());
            // 3. Filter products that belong to ANY of these IDs
            specs = specs.and(ProductSpecifications.hasCategoryIdIn(categoryIds));
        }

        if (searchDTO.getMinPrice() != null) {
            specs = specs.and(ProductSpecifications.hasPriceGreaterThan(searchDTO.getMinPrice()));
        }

        if (searchDTO.getMaxPrice() != null) {
            specs = specs.and(ProductSpecifications.hasPriceLessThan(searchDTO.getMaxPrice()));
        }

        if (searchDTO.getMinRating() != null) {
            specs = specs.and(ProductSpecifications.hasMinRating(searchDTO.getMinRating()));
        }

        if (searchDTO.getKeyword() != null && !searchDTO.getKeyword().isEmpty()) {
            specs = specs.and(ProductSpecifications.hasKeyword(searchDTO.getKeyword()));
        }

        // 3. Execute the dynamic query with pagination
        Page<Product> productPage = productRepository.findAll(specs, pageable);

        // 4. Transform entities to DTOs for the frontend
        return productPage.map(productMapper::toProductResponse);
    }

    /**
     * ACTIVITY: Upload Product Image (Direct CDN)
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product-slug", allEntries = true)
    })
    public Map<String, String> uploadProductImage(UUID productPublicId, MultipartFile file, Long sellerId){

        Product product = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getSellerId().equals(sellerId)) {
            throw new BusinessLogicException("Access Denied: You do not own this product.");
        }

        String imageUrl = cloudinaryService.upload(file, "products");
        String cdnPublicId = cloudinaryService.extractPublicId(imageUrl);

        int nextSortOrder = 0;
        boolean isPrimary = true;

        if(product.getMediaGallery() != null && !product.getMediaGallery().isEmpty()) {
            isPrimary = false;
            nextSortOrder = product.getMediaGallery().stream()
                    .mapToInt(ProductMedia::getSortOrder)
                    .max().orElse(-1) + 1;
        }

        ProductMedia newMedia = ProductMedia.builder()
                .product(product)
                .mediaUrl(imageUrl)
                .mediaType("IMAGE")
                .isPrimary(isPrimary)
                .sortOrder(nextSortOrder)
                .build();

        product.getMediaGallery().add(newMedia);

        // Security Routing
        if(product.getApprovalStatus() == ProductApprovalStatus.APPROVED){
            product.setApprovalStatus(ProductApprovalStatus.PENDING_REVIEW);
            product.setLifecycleStatus(LifecycleStatus.DRAFT);
            product.setIsAvailable(false);

            ProductModerationHistory history = ProductModerationHistory.builder()
                    .product(product)
                    .adminId(0L)
                    .actorType(ActorType.SYSTEM)
                    .action(ModerationAction.SUBMITTED)
                    .approvalStatusFrom(ProductApprovalStatus.APPROVED)
                    .approvalStatusTo(ProductApprovalStatus.PENDING_REVIEW)
                    .reason("Automated security trigger: Seller uploaded new media.")
                    .build();
            productModerationHistoryRepository.save(history);
        }

        productRepository.save(product);
        return Map.of("imageUrl", imageUrl, "publicId", cdnPublicId);
    }

    /**
     * ACTIVITY: Delete Product Image
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "product-slug", allEntries = true)
    })
    public void deleteProductImage(UUID productPublicId, String cdnPublicId, Long actorId, boolean isAdmin) {

        Product product = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!(isAdmin || product.getSellerId().equals(actorId))) {
            throw new BusinessLogicException("Access Denied: You must be the owner or an Admin to delete this image.");
        }

        ProductMedia mediaToRemove = product.getMediaGallery().stream()
                .filter(media -> media.getMediaUrl().contains(cdnPublicId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Image not found on this product."));

        boolean wasPrimary = mediaToRemove.isPrimary();

        cloudinaryService.delete(cdnPublicId);
        product.getMediaGallery().remove(mediaToRemove);

        if(wasPrimary && !product.getMediaGallery().isEmpty()) {
            product.getMediaGallery().stream()
                    .min(Comparator.comparingInt(ProductMedia::getSortOrder))
                    .ifPresent(newPrimary -> newPrimary.setPrimary(true));
        }

        if(product.getApprovalStatus() == ProductApprovalStatus.APPROVED){
            product.setIsAvailable(false);
            product.setLifecycleStatus(LifecycleStatus.DRAFT);

            ProductModerationHistory.ProductModerationHistoryBuilder historyBuilder =
                    ProductModerationHistory.builder()
                            .product(product)
                            .approvalStatusFrom(ProductApprovalStatus.APPROVED);

            if(isAdmin){
                product.setApprovalStatus(ProductApprovalStatus.REQUIRES_CHANGES);
                historyBuilder.adminId(actorId)
                        .actorType(ActorType.ADMIN)
                        .action(ModerationAction.REQUESTED_CHANGES)
                        .approvalStatusTo(ProductApprovalStatus.REQUIRES_CHANGES)
                        .reason("Admin deleted a non-compliant image. Seller must fix and resubmit.");
            }else{
                product.setApprovalStatus(ProductApprovalStatus.PENDING_REVIEW);
                historyBuilder.adminId(0L)
                        .actorType(ActorType.SYSTEM)
                        .action(ModerationAction.SUBMITTED) // <-- FIXED HERE
                        .approvalStatusTo(ProductApprovalStatus.PENDING_REVIEW)
                        .reason("Automated security trigger: Seller deleted media from a live product.");
            }
            productModerationHistoryRepository.save(historyBuilder.build());
        }
        productRepository.save(product);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HELPER — builds a rich descriptive sentence for the AI embedding
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * WHY THIS MATTERS:
     * "all-MiniLM-L6-v2" (our AI model) understands meaning through context.
     * A thin input like "Prox BT-500" gives the model almost no clues.
     * A rich sentence like:
     * "Prox BT-500 - Premium noise-cancelling headphones.
     * Category: Electronics > Headphones. Brand: Prox.
     * Tags: wireless, Bluetooth, noise-cancelling."
     * gives the model enough context to understand WHAT this product is,
     * resulting in much more accurate semantic matches.
     */
    private String buildEmbeddingText(Product product) {
        StringBuilder sb = new StringBuilder();

        // Product name is the most important signal — put it first
        sb.append(product.getProductName()).append(" - ");

        // Description adds semantic depth (what the product does / who it's for)
        if (product.getDescription() != null && !product.getDescription().isBlank()) {
            sb.append(product.getDescription()).append(". ");
        }

        // Category name helps the model understand the product domain
        // e.g. "Electronics" vs "Clothing" creates completely different vector regions
        if (product.getCategory() != null) {
            sb.append("Category: ").append(product.getCategory().getName()).append(". ");
        }

        // Brand is a strong semantic anchor (e.g. "Sony" = premium audio)
        if (product.getBrand() != null && !product.getBrand().isBlank()) {
            sb.append("Brand: ").append(product.getBrand()).append(". ");
        }

        // Tags are keyword boosters (short, high-signal terms)
        if (product.getTags() != null && !product.getTags().isEmpty()) {
            sb.append("Tags: ").append(String.join(", ", product.getTags())).append(".");
        }

        return sb.toString().trim();
    }

    /**
     * DOMAIN LOGIC: Slug Concurrency Generator
     * Edge Case Guard: Two sellers upload "iPhone 15" at the exact same
     * millisecond.
     * We convert to "iphone-15", check existsBySlug. If true, auto-increment to
     * "iphone-15-1", "iphone-15-2".
     * Prevents unhandled DB constraint violations from bubbling to the frontend.
     */
    private String generateUniqueSlug(String productName) {
        String baseSlug = productName.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        String slug = baseSlug;
        int counter = 1;

        while (productRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
            // Edge Case: Infinite loops if DB has 10,000 generic "shirt" products.
            // Fallback to UUID entropy after 100 collisions to guarantee O(1) resolution.
            if (counter > 100) {
                slug = baseSlug + "-" + UUID.randomUUID().toString().substring(0, 5);
                break;
            }
        }
        return slug;
    }

    /**
     * DOMAIN LOGIC: Auto-SKU Generator
     * Used when a seller leaves the SKU blank for a variant.
     * Generates a unique, collision-proof stock keeping unit.
     */
    private String generateUniqueSku(String categoryName) {
        String prefix = (categoryName != null && categoryName.length() >= 3)
                ? categoryName.substring(0, 3).toUpperCase()
                : "PRD";
        String sku;
        do {
            sku = prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (productVariantRepository.existsBySku(sku));
        return sku;
    }

    /**
     * DOMAIN LOGIC: Strict State Transition Matrix
     * Enforces the V4.2 Moderation Pipeline.
     * Prevents sellers from bypassing the admin review process.
     */
    private void validateStateTransition(Product product, LifecycleStatus newStatus, boolean isAdmin) {
        LifecycleStatus currentStatus = product.getLifecycleStatus();
        ProductApprovalStatus approvalStatus = product.getApprovalStatus();

        if (currentStatus == newStatus) {
            return; // no change
        }

        if (newStatus == LifecycleStatus.ACTIVE) {
            // Edge Case: Seller tries to forcefully activate a product that an admin
            // explicitly rejected.
            if (approvalStatus == ProductApprovalStatus.REJECTED
                    || approvalStatus == ProductApprovalStatus.REQUIRES_CHANGES) {
                log.warn("Security Alert: Seller {} attempted to activate a REJECTED product {}", product.getSellerId(),
                        product.getId());
                throw new ProductStateTransitionException("Cannot activate product. Current status is: "
                        + approvalStatus + ". Please resolve admin feedback first.");
            }

            // Edge Case: Seller tries to activate a product that is currently waiting in
            // the admin queue.
            if (approvalStatus == ProductApprovalStatus.PENDING_REVIEW) {
                throw new ProductStateTransitionException(
                        "Product is still under review. Please wait for admin approval.");
            }
        }
        // Edge Case: Only admins can un-archive a product. If a seller deleted it, it
        // stays deleted.
        if (currentStatus == LifecycleStatus.ARCHIVED && !isAdmin) {
            throw new ProductStateTransitionException(
                    "Archived products are locked and cannot be modified by sellers.");
        }
    }
}
