package com.manish.smartcart.product.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class ProductMediaResponse {

    // ─── 3-ID SYSTEM ENFORCED ────────────────────────────────────────────────
    // Never expose the internal Long ID to the frontend. Use the BaseEntity UUID.
    private UUID mediaPublicId;

    private String mediaUrl;
    private String mediaType;
    private Boolean isPrimary;
    private Integer sortOrder;
    private String altText;
}
