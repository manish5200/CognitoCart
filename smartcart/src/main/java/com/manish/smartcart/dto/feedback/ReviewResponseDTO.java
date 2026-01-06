package com.manish.smartcart.dto.feedback;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class ReviewResponseDTO {
    private Long productId;
    private String productName;
    private Double averageRating;
    private Integer rating;
    private String comment;

}
