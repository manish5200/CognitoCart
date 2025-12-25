package com.manish.smartcart.mapper;


import com.manish.smartcart.dto.feedback.ReviewResponseDTO;
import com.manish.smartcart.model.feedback.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {
    public ReviewResponseDTO toReviewResponseDTO(Review review){
        ReviewResponseDTO reviewResponseDTO = new ReviewResponseDTO();
        reviewResponseDTO.setProductId(review.getProduct().getId());
        reviewResponseDTO.setProductName(review.getProduct().getProductName());
        reviewResponseDTO.setAverageRating(review.getProduct().getAverageRating());
        reviewResponseDTO.setRating(review.getRating());
        reviewResponseDTO.setComment(review.getComment());
        return reviewResponseDTO;
    }
}
