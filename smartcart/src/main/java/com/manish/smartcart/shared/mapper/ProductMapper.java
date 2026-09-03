package com.manish.smartcart.shared.mapper;

import com.manish.smartcart.product.dto.ProductMediaResponse;
import com.manish.smartcart.product.dto.ProductRequest;
import com.manish.smartcart.product.dto.ProductResponse;
import com.manish.smartcart.product.model.Product;
import com.manish.smartcart.product.model.ProductInsights;
import com.manish.smartcart.product.model.embeddable.ProductSEO;
import com.manish.smartcart.product.model.embeddable.ProductWarranty;
import com.manish.smartcart.seller.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final SellerProfileRepository sellerProfileRepository;

    public ProductResponse toProductResponse(Product product) {

        if (product == null)
            return null;

        ProductResponse productResponse = new ProductResponse();
        productResponse.setProductPublicId(product.getPublicId());  // UUID — the only public identifier
        productResponse.setProductCode(product.getProductCode());    // Human-readable code e.g. PRD-YYYYMMDD-XXXXX
        productResponse.setSlug(product.getSlug());
        productResponse.setProductName(product.getProductName());
        productResponse.setDescription(product.getDescription());
        productResponse.setPrice(product.getPrice());
        productResponse.setDiscountPrice(product.getDiscountPrice());
        productResponse.setAverageRating(product.getAverageRating());
        productResponse.setTotalReviews(product.getTotalReviews());
        productResponse.setTotalSold(product.getTotalSold());
        productResponse.setTags(product.getTags() != null
                ? new HashSet<>(product.getTags())
                : new HashSet<>());

        // --- Recursive Category Mapping ---
        // We extract the name from the Category entity associated with the product
        if (product.getCategory() != null) {
            productResponse.setCategoryId(product.getCategory().getId());
            productResponse.setCategoryName(product.getCategory().getName());
        } else {
            productResponse.setCategoryName("Uncategorized");
        }
        ProductInsights insights = product.getInsights(); // after adding @OneToOne to Product
        if (insights != null) {
            productResponse.setAiSummary(insights.getAiSummary());
            productResponse.setInsightLastGenerated(insights.getLastGenerated());
        }
        
        // --- Vendor (Seller) Mapping ---
        if (product.getSellerId() != null) {
            sellerProfileRepository.findById(product.getSellerId()).ifPresent(seller -> {
                productResponse.setStoreName(seller.getStoreName());
                productResponse.setBusinessAddress(seller.getBusinessAddress());
            });
        }

        // --- Media Gallery Mapping (V4.2) ---
        if (product.getMediaGallery() != null && !product.getMediaGallery().isEmpty()) {
            List<ProductMediaResponse> mediaResponses = product.getMediaGallery().stream()
                    .map(media -> ProductMediaResponse.builder()
                            .mediaPublicId(media.getPublicId()) // STRICT 3-ID RULE ENFORCED
                            .mediaUrl(media.getMediaUrl())
                            .mediaType(media.getMediaType())
                            .isPrimary(media.isPrimary())
                            .sortOrder(media.getSortOrder())
                            .altText(media.getAltText())
                            .build())
                    .toList();
            productResponse.setMediaGallery(mediaResponses);
        } else {
            productResponse.setMediaGallery(new ArrayList<>());
        }

        return productResponse;
    }

    public Product toProduct(ProductRequest productRequest) {
        Product product = new Product();

        // 1. Basic Fields
        product.setProductName(productRequest.getProductName());
        product.setPrice(productRequest.getPrice());
        if(productRequest.getDiscountPrice() != null) {
            product.setDiscountPrice(productRequest.getDiscountPrice());
        }
        product.setDescription(productRequest.getDescription());
        if (productRequest.getTags() != null) {
            product.setTags(productRequest.getTags());
        }

        // 2. NEW V4.2 DOMAIN FIELDS
        product.setCountryOfOrigin(productRequest.getCountryOfOrigin());
        product.setCondition(productRequest.getCondition());
        product.setProductType(productRequest.getProductType());

        // 3. JSONB Attributes Map
        if (productRequest.getAttributes() != null) {
            product.setAttributes(productRequest.getAttributes());
        }

        // 4. EMBEDDABLE: WARRANTY
        if (productRequest.getWarranty() != null) {
            ProductWarranty warranty = new ProductWarranty();
            warranty.setWarrantyType(productRequest.getWarranty().getWarrantyType());
            warranty.setWarrantyDuration(productRequest.getWarranty().getWarrantyDuration());
            warranty.setWarrantyDurationUnit(productRequest.getWarranty().getWarrantyDurationUnit());
            product.setWarranty(warranty);
        }

        // 5. EMBEDDABLE: SEO
        if (productRequest.getSeo() != null) {
            ProductSEO seo = new ProductSEO();
            seo.setSeoTitle(productRequest.getSeo().getSeoTitle());
            seo.setMetaDescription(productRequest.getSeo().getMetaDescription());
            product.setSeo(seo);
        }

        // NOTE: Variants and Media are managed by ProductService.createProduct()
        return product;
    }
}
