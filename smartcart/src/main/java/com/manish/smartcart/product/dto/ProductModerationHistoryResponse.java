package com.manish.smartcart.product.dto;

import com.manish.smartcart.shared.enums.product.ActorType;
import com.manish.smartcart.shared.enums.product.ModerationAction;
import com.manish.smartcart.shared.enums.product.ProductApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductModerationHistoryResponse {
    private String actorType;
    private String action;
    private String approvalStatusFrom;
    private String approvalStatusTo;
    private String reason;
    private LocalDateTime createdAt;
}
