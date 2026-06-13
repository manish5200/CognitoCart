package com.manish.smartcart.service;

import com.manish.smartcart.dto.product.ProductRequest;
import com.manish.smartcart.dto.product.ProductResponse;
import com.manish.smartcart.dto.product.ProductSearchDTO;
import com.manish.smartcart.mapper.ProductMapper;
import com.manish.smartcart.model.product.Category;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.model.product.ProductVariant;
import com.manish.smartcart.repository.CategoryRepository;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.repository.ProductVariantRepository;
import com.manish.smartcart.repository.specifications.ProductSpecifications;
import com.manish.smartcart.util.VectorAttributeConverter;
import com.manish.smartcart.exception.BusinessLogicException;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import com.manish.smartcart.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

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
            String tagsText = savedProduct.getTags() != null
                    ? String.join(" ", savedProduct.getTags()) : "";
            String textToEmbed = savedProduct.getProductName() + " " +
                    (savedProduct.getDescription() != null ? savedProduct.getDescription() : "") +
                    " " + tagsText;

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
    public void toggleAvailability(Long productId, Long currentSellerId, boolean isAdmin) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

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

    /**
     * ACTIVITY: Discovery (Fetch by Slug)
     * Returns DTO so Redis can safely serialize/deserialize without Hibernate
     * session.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "product-slug", key = "#p0")
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for slug: " + slug));
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
    public void deleteProduct(Long id, Long authenticatedUserId, boolean isAdmin) {
        // Combined exists and find into one call for efficiency
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        if (!isAdmin && !product.getSellerId().equals(authenticatedUserId)) {
            throw new BusinessLogicException("Access Denied: Only owners or admins can delete products.");
        }
        productRepository.deleteById(id);
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
}
