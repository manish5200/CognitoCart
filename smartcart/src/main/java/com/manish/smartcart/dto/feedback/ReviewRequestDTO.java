package com.manish.smartcart.dto.feedback;

import jakarta.validation.constraints.*;

public class ReviewRequestDTO {

    @NotNull(message = "Rating is required")
    @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 600, message = "Comment is too long")
    private String comment;

    public ReviewRequestDTO() {}

    public ReviewRequestDTO(Integer rating, String comment) {
        this.rating = rating;
        this.comment = comment;
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
