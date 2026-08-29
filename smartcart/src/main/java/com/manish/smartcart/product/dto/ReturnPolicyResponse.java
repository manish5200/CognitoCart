package com.manish.smartcart.product.dto;

import com.manish.smartcart.shared.enums.PolicyType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReturnPolicyResponse {
    private Long policyId;
    private java.util.UUID policyPublicId;

    // Product-level fields (null if category-level)
    private Long productId;
    private String productName;

    // Category-level fields (null if product-level)
    private Long categoryId;
    private String categoryName;

    private PolicyType policyType;
    private int returnWindowDays;
    private boolean returnAllowed;
    private boolean exchangeAllowed;
    private boolean replacementAllowed;
    private boolean pickupAvailable;

    /** "PRODUCT" or "CATEGORY" — for easy frontend display */
    private String scope;
}
