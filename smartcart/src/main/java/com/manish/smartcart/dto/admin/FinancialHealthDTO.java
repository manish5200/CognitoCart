package com.manish.smartcart.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class FinancialHealthDTO {

    private BigDecimal grossRevenue;
    private BigDecimal lostRevenueToRefunds;
    private BigDecimal netRevenue;

    // Percentages are fine as Double, since they are just for display/ratios, not accounting
    private Double refundRatePercentage;
}
