package com.manish.smartcart.dto.feedback;

import lombok.*;

/**
 * The public-facing shape of a single review.
 * Designed to render a review "card" in the UI — contains everything
 * the frontend needs: who, what rating, what comment, and when.

 * CONCEPT: A DTO (Data Transfer Object) is NOT your database entity.
 * It's a tailored view of your data for a specific use case.
 * We never expose the raw entity to the client because it may contain
 * sensitive internal fields or Hibernate proxy objects.
 */

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class ReviewResponseDTO {

    private Long reviewId;
    private Long productId;
    private String productName;
    private Double averageRating;
    private Integer rating;
    private String comment;

}
