package com.manish.smartcart.sale.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload submitted by the Seller to opt in a specific Variant to an Admin Event.
 * Includes strict Bean Validation to prevent malicious inputs.
 */

@Data
public class FlashSaleItemRequest {

    @NotNull(message = "Platform Event ID is required")
    private UUID saleEventPublicId;

    @NotNull(message = "Variant ID is required")
    private UUID variantPublicId;
    @NotNull(message = "Discount percentage is required")

    @Min(value = 5, message = "Sellers must offer at least a 5% discount to participate")
    @Max(value = 90, message = "Maximum allowed discount is 90%")
    private BigDecimal discountPercentage;

    @NotNull(message = "Max units allocated for sale is required")
    @Min(value = 1, message = "Sellers must allocate at least 1 unit to the sale")
    private Integer maxUnits;

    @Min(value = 1, message = "Must allow at least 1 unit per user")
    private Integer maxUnitsPerUser = 1;
}
