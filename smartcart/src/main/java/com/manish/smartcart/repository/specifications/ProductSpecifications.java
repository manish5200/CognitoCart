package com.manish.smartcart.repository.specifications;

import com.manish.smartcart.model.product.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

public class ProductSpecifications {

    // Filter by Category Name (Joining the Category table)
    public static Specification<Product> hasCategoryIdIn(List<Long> categoryIds){
        return (root,query,cb) ->
                categoryIds == null || categoryIds.isEmpty() ? null :
                        root.get("category").get("id").in(categoryIds);
    }

    // Filter by Price Range (Max Price)
    public static Specification<Product>hasPriceLessThan(BigDecimal maxPrice){
        return (root,query,cb) ->
                maxPrice == null ? null : cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    // Filter by Price Range (Min Price)
    public static Specification<Product>hasPriceGreaterThan(BigDecimal minPrice){
        return (root,query,cb) ->
                minPrice == null ? null : cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    // Filter by the Denormalized Rating (Created on Day 8)
    public static Specification<Product>hasMinRating(Double minRating){
        return (root,query,cb) ->
                minRating == null ? null : cb.greaterThanOrEqualTo(root.get("averageRating"), minRating);
    }

    // Keyword Search (Searching in Name OR Description)
    public static Specification<Product>hasKeyword(String keyword){
        return (root,query,cb) -> {

            if (keyword == null || keyword.isEmpty()) return null;
            String searchPattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("productName")), searchPattern),
                    cb.like(cb.lower(root.get("description")), searchPattern)
            );
        };
    }

}
