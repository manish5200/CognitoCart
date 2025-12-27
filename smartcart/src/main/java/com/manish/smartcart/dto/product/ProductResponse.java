package com.manish.smartcart.dto.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class ProductResponse {
    private Long id;
    private String productName;
    private String description;
    private BigDecimal price;
    private String sku;
    private Integer stockQuantity;
    private String categoryName;
    private Set<String> tags;
    private Double averageRating;
    private Integer totalReviews;
    private List<String> imageUrls;

    public ProductResponse() {}

    public ProductResponse(Long id, String productName, BigDecimal price,
                           String description, String sku, Integer stockQuantity,
                           String categoryName, Set<String>tags, Double averageRating,
                           Integer totalReviews, List<String> imageUrls) {
        this.id = id;
        this.productName = productName;
        this.price = price;
        this.description = description;
        this.sku = sku;
        this.stockQuantity = stockQuantity;
        this.categoryName = categoryName;
        this.tags = tags;
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.imageUrls = imageUrls;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Integer totalReviews) {
        this.totalReviews = totalReviews;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}
