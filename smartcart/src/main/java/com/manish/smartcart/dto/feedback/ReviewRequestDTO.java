package com.manish.smartcart.dto.feedback;

import jakarta.validation.constraints.*;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class ReviewRequestDTO {

    @NotNull(message = "Rating is required")
    @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 600, message = "Comment is too long")
    private String comment;

}
