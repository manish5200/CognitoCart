package com.manish.smartcart.dto.feedback;

import lombok.*;

/**
 * Represents the full star-rating breakdown for a product.

 * CONCEPT: Instead of re-computing this every time from the reviews list
 * in Java (slow, loads all rows), we ask the database to aggregate it for us
 * with a single GROUP BY query. One DB call vs. potentially thousands of rows.

 * Example response:
 * {
 *   "productId": 42,
 *   "totalReviews": 150,
 *   "averageRating": 4.3,
 *   "fiveStars": 90,
 *   "fourStars": 35,
 *   "threeStars": 15,
 *   "twoStars": 7,
 *   "oneStar": 3
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingDistributionDTO {

    private Long productId;

    private String productName;

    // The current live average rating (e.g., 4.3)
    private Double averageRating;

    // Total number of reviews submitted
    private Integer totalReviews;

    // Count of reviews per star level
    private long fiveStars;
    private long fourStars;
    private long threeStars;
    private long twoStars;
    private long oneStar;
}
