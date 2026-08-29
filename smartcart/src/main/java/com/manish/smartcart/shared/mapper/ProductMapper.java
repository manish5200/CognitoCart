package com.manish.smartcart.shared.mapper;

import com.manish.smartcart.product.dto.ProductRequest;
import com.manish.smartcart.product.dto.ProductResponse;
import com.manish.smartcart.product.model.Product;
import com.manish.smartcart.product.model.ProductInsights;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;

@Component
public class ProductMapper {

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
        // NOTE: sku and stockQuantity live on ProductVariant, not Product.
        // The product response intentionally omits them at this layer.
        // Variant-level detail is returned by GET /api/v1/products/{id}/variants
        productResponse.setAverageRating(product.getAverageRating());
        productResponse.setTotalReviews(product.getTotalReviews());
        productResponse.setTotalSold(product.getTotalSold());
        // Copy into plain Java collections — CRITICAL for Redis serialization.
        // Hibernate's PersistentSet/PersistentBag is session-bound and cannot be
        // serialized by Jackson after the Hibernate session is closed.
        productResponse.setImageUrls(product.getImageUrls() != null
                ? new ArrayList<>(product.getImageUrls())
                : new ArrayList<>());
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
        return productResponse;
    }

    public Product toProduct(ProductRequest productRequest) {
        Product product = new Product();
        product.setProductName(productRequest.getProductName());
        product.setPrice(productRequest.getPrice());
        if(productRequest.getDiscountPrice() != null) {
            product.setDiscountPrice(productRequest.getDiscountPrice());
        }
        product.setDescription(productRequest.getDescription());
        // NOTE: stockQuantity lives on ProductVariant — NOT set here.
        // ProductService.createProduct() will create the default variant separately.
        // Handle Images and Tags safely
        if (productRequest.getImageUrls() != null)
            product.setImageUrls(productRequest.getImageUrls());
        if (productRequest.getTags() != null)
            product.setTags(productRequest.getTags());

        return product;
    }
}
