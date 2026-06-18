package com.manish.smartcart.dto.product;

import com.manish.smartcart.enums.ApprovalStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Clean, decoupled response payload. Hides internal IDs and database logic
 * from the frontend, delivering only what the UI needs to render the item.
 */

@Data
@Builder
public class FlashSaleItemResponse {

    private Long id;
    private Long eventId;
    private String eventName;
    private Long variantId;
    private String sku; // Allows frontend to render variant details easily
    private BigDecimal discountPercentage;
    private Integer maxUnits;
    private Integer maxUnitsPerUser;
    private Integer usedUnits;
    private ApprovalStatus approvalStatus;

}
