package com.manish.smartcart.dto.feedback;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * The public-facing shape of a single review.
 * Designed to render a review "card" in the UI — contains everything
 * the frontend needs: who, what rating, what comment, and when.
 *
 * CONCEPT: A DTO (Data Transfer Object) is NOT your database entity.
 * It's a tailored view of your data for a specific use case.
 * We never expose the raw entity to the client because it may contain
 * sensitive internal fields or Hibernate proxy objects.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDTO {

    // The unique ID of this specific review (needed for delete operations from the frontend)
    private Long reviewId;

    // The reviewer's display name — what appears on the review card
    private String reviewerName;

    // The star rating this specific user gave (1-5)
    private Integer rating;

    // The text body of the review
    private String comment;

    // CONCEPT: @JsonFormat ensures the date is serialized as a readable string
    // (e.g., "2025-04-10 14:30:00") instead of a raw timestamp array like [2025, 4, 10, 14, 30].
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewedAt;

    // True if this review was submitted after a confirmed 'DELIVERED' order.
    // Allows the UI to show a "✅ Verified Purchase" badge — same as Amazon.
    private boolean verifiedPurchase;
}
