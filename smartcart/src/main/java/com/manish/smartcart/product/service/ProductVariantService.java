package com.manish.smartcart.product.service;

import com.manish.smartcart.product.dto.InventoryAdjustmentRequest;
import com.manish.smartcart.product.dto.ProductVariantRequest;
import com.manish.smartcart.product.dto.ProductVariantResponse;
import com.manish.smartcart.infrastructure.storage.CloudinaryService;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.shared.exception.InsufficientStockException;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import com.manish.smartcart.product.model.Product;
import com.manish.smartcart.product.model.ProductVariant;
import com.manish.smartcart.product.repository.ProductRepository;
import com.manish.smartcart.product.repository.ProductVariantRepository;
import com.manish.smartcart.shared.util.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * PUBLIC GET: Retrieve all active variants for a product
     */
    @Transactional(readOnly = true)
    public List<ProductVariantResponse>getPublicVariants(UUID productPublicId){
        Long productId = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productPublicId))
                .getId();
        return productVariantRepository.findByProductIdAndIsActiveTrue(productId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    /**
     * SELLER POST: Add a new variant (e.g., "Size XL")
     */
    @Transactional
    public ProductVariantResponse addProductVariant(UUID productPublicId, ProductVariantRequest request,
                                                    Long currentSellerId){

        Long productId = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productPublicId))
                .getId();

        Product product = validateSellerOwnership(productId, currentSellerId);

        String resolvedSku = (request.getSku() == null || request.getSku().isBlank())
                ? "SKU-" + UUID.randomUUID().toString().substring(0,8).toUpperCase()
                : request.getSku();
        if(productVariantRepository.existsBySku(resolvedSku)){
            throw new BusinessLogicException("SKU already exists: " + resolvedSku);
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(resolvedSku)
                .priceModifier(request.getPriceModifier())
                .stockQuantity(request.getStockQuantity())
                .reservedQuantity(0)
                .lowStockThreshold(request.getLowStockThreshold())
                .attributes(request.getAttributes())
                .weight(request.getWeight())
                .lengthCm(request.getLengthCm())
                .widthCm(request.getWidthCm())
                .heightCm(request.getHeightCm())
                .sortOrder(request.getSortOrder())
                .isActive(true)
                .build();
        log.info("Seller {} successfully added new variant SKU: {} to Product {}", currentSellerId, resolvedSku, productId);
        return toResponse(productVariantRepository.save(variant));
    }

    /**
     * SELLER PUT: Update price, stock, or attributes
     */
    @Transactional
    public ProductVariantResponse updateProductVariant(UUID productPublicId,
                                                       UUID variantPublicId,
                                                       ProductVariantRequest request,
                                                       Long currentSellerId){
        Long productId = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productPublicId))
                .getId();
        Long variantId = productVariantRepository.findByPublicId(variantPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantPublicId))
                .getId();
        validateSellerOwnership(productId, currentSellerId);

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

        if(!variant.getProduct().getId().equals(productId)){
            throw new BusinessLogicException("Variant does not belong to the specified product.");
        }

        variant.setPriceModifier(request.getPriceModifier());
        variant.setStockQuantity(request.getStockQuantity());
        variant.setLowStockThreshold(request.getLowStockThreshold());
        variant.setAttributes(request.getAttributes());
        variant.setWeight(request.getWeight());
        variant.setLengthCm(request.getLengthCm());
        variant.setWidthCm(request.getWidthCm());
        variant.setHeightCm(request.getHeightCm());
        variant.setSortOrder(request.getSortOrder());

        log.info("Seller {} updated variant {}. New Stock: {}, Price Modifier: {}",
                currentSellerId, variantId, request.getStockQuantity(), request.getPriceModifier());
        return toResponse(productVariantRepository.save(variant));
    }

    /**
     * SELLER POST: Upload SKU-specific image
     */
    @Transactional
    public String uploadVariantImage(UUID productPublicId, UUID variantPublicId,
                                     MultipartFile file, Long currentSellerId){

        Long productId = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productPublicId))
                .getId();
        Long variantId = productVariantRepository.findByPublicId(variantPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantPublicId))
                .getId();
        validateSellerOwnership(productId, currentSellerId);
        FileValidator.validateImage(file);

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

        if(!variant.getProduct().getId().equals(productId)){
            throw new BusinessLogicException("Variant does not belong to the specified product.");
        }

        String imageUrl = cloudinaryService.upload(file, "product-variants");
        variant.setVariantImageUrl(imageUrl);
        productVariantRepository.save(variant);

        log.info("Seller {} uploaded CDN image for variant {}. URL: {}", currentSellerId, variantId, imageUrl);
        return imageUrl;
    }

    /**
     * SELLER PATCH: Toggle Active Status (Soft Delete)
     */
    public void toggleVariantStatus(UUID productPublicId, UUID variantPublicId, Long currentSellerId){

        Long productId = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productPublicId))
                .getId();

        Long variantId = productVariantRepository.findByPublicId(variantPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantPublicId))
                .getId();

        validateSellerOwnership(productId, currentSellerId);

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

        variant.setActive(!variant.isActive());
        productVariantRepository.save(variant);
    }
    // ─── HELPER METHODS ─────────────────────────────────────────────────────────

    private Product validateSellerOwnership(Long productId, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        if(!product.getSellerId().equals(sellerId)){
            throw new BusinessLogicException("Access Denied: You do not own this product.");
        }
        return product;
    }

    private ProductVariantResponse toResponse(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .productId(variant.getProduct().getId())
                .sku(variant.getSku())
                .priceModifier(variant.getPriceModifier())
                .stockQuantity(variant.getStockQuantity())
                .reservedQuantity(variant.getReservedQuantity())
                .availableStock(variant.getAvailableStock())
                .lowStockThreshold(variant.getLowStockThreshold())
                .attributes(variant.getAttributes())
                .weight(variant.getWeight())
                .lengthCm(variant.getLengthCm())
                .widthCm(variant.getWidthCm())
                .heightCm(variant.getHeightCm())
                .variantImageUrl(variant.getVariantImageUrl())
                .sortOrder(variant.getSortOrder())
                .isActive(variant.isActive())
                .displayLabel(variant.getDisplayLabel())
                .build();
    }

    /*
     * SELLER: Atomically adjust inventory using a signed delta.
     *
     * Uses a single UPDATE statement to prevent the Lost Update race condition.
     * @see ProductVariantRepository#atomicAdjustStock
     *
     * @param variantPublicId  UUID of the variant (external identifier)
     * @param request          Contains signed delta (±) and audit reason
     * @param sellerId         Internal ID of the authenticated seller (for IDOR check)
     * @throws BusinessLogicException    if seller does not own this variant
     * @throws InsufficientStockException if adjustment would result in negative stock
     */
    public ProductVariantResponse adjustStock(UUID variantPublicId,
                                              InventoryAdjustmentRequest request,
                                              Long sellerId){

        // 1. Resolve external UUID → internal Long PK (3-ID system pattern)
        ProductVariant variant = productVariantRepository.findByPublicId(variantPublicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Variant not found: " + variantPublicId));

        // 2. IDOR Check: verify the authenticated seller owns this product.
        //    Returns 404-style ResourceNotFoundException to avoid confirming
        //    the existence of variants the caller doesn't own (security best practice).
        if(!variant.getProduct().getSellerId().equals(sellerId)){
            throw new BusinessLogicException(
                    "Access Denied: You do not own variant " + variantPublicId);
        }

        // 3. Execute atomic SQL delta.
        //    Returns 0 if the WHERE guard (stock + delta >= 0) rejected the update.
        int rowsAffected = productVariantRepository
                .atomicAdjustStock(variant.getId(), request.getAdjustment());

        if(rowsAffected == 0){
            throw new InsufficientStockException(
                    "Stock adjustment of " + request.getAdjustment()
                            + " would result in negative stock for variant " + variantPublicId
                            + ". Current stock: " + variant.getStockQuantity());
        }

        // 4. Audit log — structured for log aggregation (Loki/ELK)
        log.info("[INVENTORY_DELTA] variantId={} sellerId={} delta={} reason={} note={}",
                variant.getId(),
                sellerId,
                request.getAdjustment(),
                request.getReason(),
                request.getNote());
        // 5. Re-fetch for accurate response (L1 cache was cleared by clearAutomatically=true)
        return toResponse(productVariantRepository.findById(variant.getId())
                .orElseThrow());
    }
}
