package com.manish.smartcart.product.service;

import com.manish.smartcart.product.dto.ProductRequest;
import com.manish.smartcart.product.dto.ProductResponse;
import com.manish.smartcart.product.dto.ProductSearchDTO;
import com.manish.smartcart.infrastructure.ai.EmbeddingService;
import com.manish.smartcart.product.dto.SemanticSearchResponse;
import com.manish.smartcart.seller.repository.SellerProfileRepository;
import com.manish.smartcart.shared.enums.KycStatus;
import com.manish.smartcart.shared.mapper.ProductMapper;
import com.manish.smartcart.product.model.Category;
import com.manish.smartcart.product.model.Product;
import com.manish.smartcart.product.model.ProductVariant;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
                .orElseThrow(() ->
                        new ResourceNotFoundException("Seller Profile" + "id #" + currentSellerId));

        if(seller.getKycStatus() != KycStatus.VERIFIED){
            throw new BusinessLogicException("KYC Enforcement: Your profile is currently "
                    + seller.getKycStatus() + ". You must be VERIFIED to list products.");
        }

        Product product = productMapper.toProduct(productRequest);

        // 2. Assign the seller ID from the authenticated user
        product.setSellerId(currentSellerId);

        // 3. Resolve the Category Link (Crucial Fix)
        if (productRequest.getCategoryId() != null) {
            // Fetch the separate Category entity from its repository
            Category category = categoryRepository.findById(productRequest.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with ID: " + productRequest.getCategoryId()));

            // Map the full entity to the Product's @ManyToOne field
            product.setCategory(category);
        }
        // 4. Generate SEO-friendly slug: "Apple iPhone 15" -> "apple-iphone-15"
        // Only a-z, 0-9 are kept; everything else becomes a dash.
        // Trailing/leading dashes are stripped for clean URLs.
        // Uniqueness is enforced by the DB @Column(unique=true) constraint.
        String slug = productRequest.getProductName()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-") // collapse multiple specials into one dash
                .replaceAll("^-+|-+$", ""); // strip leading/trailing dashes
        product.setSlug(slug);

        // 5. SKU is now a Variant-level concern.
        // We resolve it below AFTER the Product is saved (need the product ID first).
        // The SKU string is carried in a local variable for use in the default variant.
        String resolvedSku = (productRequest.getSku() == null || productRequest.getSku().isBlank())
                ? "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                : productRequest.getSku();

        // STEP 1: Save the product first without the embedding.
        // JPA handles all regular columns (price, name, stock, etc.) cleanly here.
        Product savedProduct = productRepository.save(product);


        // DEFAULT VARIANT CREATION — The "Simple Product" Pattern
        // Every product needs at least one purchasable variant to be checkout-eligible.
        // For simple products (no size/color options), we silently create a single
        // "Standard" variant so the checkout flow has a uniform code path.
        // Sellers can add more variants later via POST /api/v1/products/{id}/variants
        ProductVariant productVariant = ProductVariant.builder()
                .product(savedProduct)
                .sku(resolvedSku)
                .stockQuantity(productRequest.getStockQuantity() != null
                ? productRequest.getStockQuantity() : 0)
                .reservedQuantity(0)
                .priceModifier(java.math.BigDecimal.ZERO)  // No modifier — use product base price
                .attributes(new java.util.LinkedHashMap<>(
                        java.util.Map.of("Type", "Standard"))) // Signals a default/simple variant
                .isActive(true)
                .sortOrder(0)
                .build();

        productVariantRepository.save(productVariant);
        log.info("Default variant created (SKU={}) for product ID {}", resolvedSku, savedProduct.getId());

        // STEP 2: Generate the embedding and write it via a separate native UPDATE.
        // CONCEPT: We do this AFTER save() because we need the product's DB-generated ID.
        // We use a native UPDATE with CAST(:value AS vector) because JPA would otherwise
        // bind the string as VARCHAR which PostgreSQL's vector column rejects.
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
            // The embedding column will be NULL — our backfill scheduler (future) fills it later.
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
    public ProductResponse updateProduct(java.util.UUID productPublicId, ProductRequest productRequest, Long currentSellerId) {
        Product product = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productPublicId));

        if (!product.getSellerId().equals(currentSellerId)) {
            throw new BusinessLogicException("Access Denied: You do not have permission to modify this product.");
        }

        if (productRequest.getProductName() != null) {
            product.setProductName(productRequest.getProductName());
        }
        if (productRequest.getDescription() != null) {
            product.setDescription(productRequest.getDescription());
        }
        if (productRequest.getPrice() != null) {
            product.setPrice(productRequest.getPrice());
        }
        if (productRequest.getDiscountPrice() != null) {
            product.setDiscountPrice(productRequest.getDiscountPrice());
        }
        if (productRequest.getTags() != null) {
            product.setTags(productRequest.getTags());
        }
        if (productRequest.getCategoryId() != null) {
            Category category = categoryRepository.findById(productRequest.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + productRequest.getCategoryId()));
            product.setCategory(category);
        }

        Product savedProduct = productRepository.save(product);
        return productMapper.toProductResponse(savedProduct);
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
    public void toggleAvailability(java.util.UUID productPublicId, Long currentSellerId, boolean isAdmin) {
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
     * Use ProductVariantService.updateVariantStock(variantId, quantityChange, sellerId, isAdmin)
     * which operates on ProductVariant.stockQuantity — the correct inventory field.
     *
     * @deprecated Since product-variant migration. Use variant-level stock management instead.
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
    // UPDATE — semanticSearch now returns scored results + supports filters
    // ════════════════════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public SemanticSearchResponse semanticSearch(
            String query, int limit,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double minRating){

        // Step 1: Convert the user's plain English query into a 384-dimensional vector
        // Same model used when products were indexed → comparable vector space
        //Format float[] → "[0.021,-0.455,...]" for the native SQL CAST
        float[] queryVector = embeddingService.generateEmbedding(query);
        String vectorString = new VectorAttributeConverter().convertToDatabaseColumn(queryVector);

        // Step 2: Fetch ranked results (with optional price/rating filters)
        // Each row is [product columns..., distance]
        boolean hasFilter = minPrice != null || maxPrice != null ||minRating != null;

        List<Object[]> rawResults = hasFilter ?
                productRepository.findBySimilarityWithFilters(vectorString, limit, minPrice, maxPrice, minRating)
                : productRepository.findBySimilarity(vectorString, limit);

        // Step 3: Convert raw rows → RankedProduct DTOs with relevance scores
        List<SemanticSearchResponse.RankedProduct> ranked = new ArrayList<>();
        for(int i = 0; i < rawResults.size(); i++) {
            Object[] row = rawResults.get(i);

            // The last column in the SELECT is the cosine distance
            // distance = 0.0 means identical, 1.0 means completely unrelated
            double distance = ((Number) row[row.length - 1]).doubleValue();

            // Convert distance → similarity score (higher = better match)
            // similarity = 1 - distance  (so distance 0.05 → similarity 0.95 = 95%)
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
     * Called by: POST /api/v1/products/admin/reindex  (manual trigger)
     */
    @Transactional
    public int reindexMissingEmbeddings(){
        List<Product> unindexed = productRepository.findAllWithNullEmbedding();
        log.info("Starting embedding backfill for {} products", unindexed.size());

        int successCount = 0;

        for(Product product : unindexed){
            try {
                // Build the rich embedding text (same format as new products)
                String text = buildEmbeddingText(product);
                float[] embedding = embeddingService.generateEmbedding(text);
                String vectorString = new VectorAttributeConverter().convertToDatabaseColumn(embedding);
                productRepository.updateEmbedding(product.getId(), vectorString);
                successCount++;
                log.info("✅ Reindexed product '{}' (id={})", product.getProductName(), product.getId());
            }catch (Exception e){
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
    public void deleteProduct(java.util.UUID productPublicId, Long authenticatedUserId, boolean isAdmin) {
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


    // ════════════════════════════════════════════════════════════════════════════
    // HELPER — builds a rich descriptive sentence for the AI embedding
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * WHY THIS MATTERS:
     * "all-MiniLM-L6-v2" (our AI model) understands meaning through context.
     * A thin input like "Prox BT-500" gives the model almost no clues.
     * A rich sentence like:
     *   "Prox BT-500 - Premium noise-cancelling headphones.
     *    Category: Electronics > Headphones. Brand: Prox.
     *    Tags: wireless, Bluetooth, noise-cancelling."
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

}
