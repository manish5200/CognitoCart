package com.manish.smartcart.product.dto;

import com.manish.smartcart.shared.enums.product.ProductApprovalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Payload sent by Admins when moderating a product.
 */
@Data
public class ProductModerationRequest {

    @NotNull(message = "Approval status action is required")
    private ProductApprovalStatus action; // e.g., APPROVED, REJECTED, REQUIRES_CHANGES

    // Required if REJECTED or REQUIRES_CHANGES
    private String reason;
}
