package com.manish.smartcart.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PlatformIntelligenceResponse{

    private FinancialHealthDTO financialHealth;

    // Funnel metrics
    private Long totalReturnRequests;
    private Double adminApprovalRate;

    // Insights
    private List<ReturnReasonStats> topReturnReasons;
}
