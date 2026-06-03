package com.manish.smartcart.dto.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
public class CustomerChurnRiskDTO {

    private Long userId;
    private String customerName;
    private LocalDateTime lastOrderDate;
    private Long daysSinceLastOrder; // Computed in Java — not from DB
    private BigDecimal lifetimeValue;
    private String riskLevel; // HOT_RISK, WARM_RISK, COLD_RISK

    /**
     * JPQL DTO Projection Constructor.
     * daysSinceLastOrder is computed at response-build time so it's always
     * accurate to "right now" — a DB-computed value would be stale by the
     * next second. ChronoUnit.DAYS gives us whole days, not fractions.
     *
     * Risk levels mirror what Swiggy/Zomato use for re-engagement campaigns:
     *  HOT_RISK  = 30–60 days gone  → send discount coupon NOW
     *  WARM_RISK = 60–90 days gone  → send "we miss you" email
     *  COLD_RISK = 90+ days gone    → last chance win-back campaign
     */
    public CustomerChurnRiskDTO(Long userId, String customerName,
                                LocalDateTime lastOrderDate, BigDecimal lifetimeValue) {
        this.userId = userId;
        this.customerName = customerName;
        this.lastOrderDate = lastOrderDate;
        this.lifetimeValue = lifetimeValue;
        this.daysSinceLastOrder = ChronoUnit.DAYS.between(lastOrderDate, LocalDateTime.now());

        // Segment by urgency so the frontend can color-code the dashboard
        if(this.daysSinceLastOrder <= 60){
            this.riskLevel = "HOT_RISK";
        }else if(this.daysSinceLastOrder <= 90){
            this.riskLevel = "WARM_RISK";
        }else{
            this.riskLevel = "COLD_RISK";
        }
    }
}
