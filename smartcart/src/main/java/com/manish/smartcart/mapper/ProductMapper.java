package com.manish.smartcart.mapper;

import com.manish.smartcart.dto.product.ProductRequest;
import com.manish.smartcart.dto.product.ProductResponse;
import com.manish.smartcart.dto.product.ProductSearchDTO;
import com.manish.smartcart.model.product.Category;
import com.manish.smartcart.model.product.Product;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Component
public class ProductMapper {

    public ProductResponse toProductResponse(Product product) {

        if (product == null)
            return null;

        ProductResponse productResponse = new ProductResponse();
        productResponse.setId(product.getId());
        productResponse.setProductName(product.getProductName());
        productResponse.setDescription(product.getDescription());
        productResponse.setPrice(product.getPrice());
        productResponse.setSku(product.getSku());
        productResponse.setStockQuantity(product.getStockQuantity());
        productResponse.setAverageRating(product.getAverageRating());
        productResponse.setTotalReviews(product.getTotalReviews());
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
            productResponse.setCategoryName(product.getCategory().getName());
        } else {
            productResponse.setCategoryName("Uncategorized");
        }
        return productResponse;
    }

    public Product toProduct(ProductRequest productRequest) {
        Product product = new Product();
        product.setProductName(productRequest.getProductName());
        product.setPrice(productRequest.getPrice());
        product.setDescription(productRequest.getDescription());
        product.setStockQuantity(productRequest.getStockQuantity());
        // Handle Images and Tags safely
        if (productRequest.getImageUrls() != null)
            product.setImageUrls(productRequest.getImageUrls());
        if (productRequest.getTags() != null)
            product.setTags(productRequest.getTags());

        return product;
    }
}
