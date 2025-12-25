package com.manish.smartcart.dto.feedback;

public class ReviewResponseDTO {
    private Long productId;
    private String productName;
    private Double averageRating;
    private Integer rating;
    private String comment;

    public ReviewResponseDTO() {}

    public ReviewResponseDTO(Long productId, String productName, Double averageRating, Integer rating, String comment) {
        this.productId = productId;
        this.productName = productName;
        this.averageRating = averageRating;
        this.rating = rating;
        this.comment = comment;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
