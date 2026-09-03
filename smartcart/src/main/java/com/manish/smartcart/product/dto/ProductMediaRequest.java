package com.manish.smartcart.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Handles incoming media uploads during product creation.
 * Preserves the exact UI sorting sequence and flags the primary thumbnail.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductMediaRequest {

    @NotBlank(message = "Media URL cannot be empty")
    private String mediaUrl;

    private String publicId; // For Cloudinary/AWS integration

    @Builder.Default
    private String mediaType = "IMAGE";

    @Builder.Default
    private boolean isPrimary = false;

    private int sortOrder;

    private String altText;
}
