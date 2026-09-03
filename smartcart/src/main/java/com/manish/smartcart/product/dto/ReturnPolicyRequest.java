package com.manish.smartcart.product.dto;

import com.manish.smartcart.shared.enums.product.PolicyType;
import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class ReturnPolicyRequest {
    /**
     * Set productPublicId to apply policy to a specific product.
     * Set categoryPublicId to apply policy to an entire category.
     * EXACTLY ONE must be provided — validated in service layer.
     */
    private java.util.UUID productPublicId;
    private java.util.UUID categoryPublicId;

    @NotNull(message = "policyType is required. "
            + "Valid: RETURN_AND_EXCHANGE, RETURN_ONLY, EXCHANGE_ONLY, REPLACEMENT_ONLY, NON_RETURNABLE")
    private PolicyType policyType;

    @Min(value = 0, message = "returnWindowDays must be 0 or more")
    @Max(value = 30, message = "returnWindowDays cannot exceed 30 days")
    private int returnWindowDays;

    private boolean returnAllowed;
    private boolean exchangeAllowed;
    private boolean replacementAllowed;
    private boolean pickupAvailable;
}
