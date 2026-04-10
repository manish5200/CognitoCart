package com.manish.smartcart.mapper;


import com.manish.smartcart.dto.feedback.ReviewResponseDTO;
import com.manish.smartcart.model.feedback.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {
    public ReviewResponseDTO toReviewResponseDTO(Review review){
           return ReviewResponseDTO.builder()
                   .reviewId(review.getId())
                   // Show the reviewer's first name only — common UX pattern (privacy-aware)
                   // e.g., "Manish Kumar Singh" becomes "Manish K."
                   .reviewerName(abbreviateLastName(review.getUser().getFullName()))
                   .rating(review.getRating())
                   .comment(review.getComment())
                   .reviewedAt(review.getCreatedAt())
                   .verifiedPurchase(true)
                   .build();
    }

    /**
     * Privacy helper: "Manish Kumar Singh" → "Manish K."
     * Avoids exposing full names publicly on the storefront.
     */
    private String abbreviateLastName(String fullName) {
        if(fullName == null || fullName.isBlank()) return "Anonymous";
        String[] parts = fullName.trim().split("\\s+");
        if(parts.length == 1) return parts[0];

        // Show first name + first letter of last name + period
        return parts[0] + " " + parts[parts.length - 1].charAt(0)+".";
    }
}
