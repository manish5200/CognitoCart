package com.manish.smartcart.dto.seller;

import lombok.Data;

@Data
public class SellerProductQualityDTO {

    private Long productId;
    private String productName;
    private Long totalOrders;
    private Long totalReturns;
    private Double returnRatePercentage;

    // Computed quality badge — same pattern Amazon Seller Central uses
    // to classify product health. Frontend maps these to green/yellow/red badges.
    private String qualityScore;

    /**
     * JPQL DTO Projection Constructor.
     *
     * The DB sends us raw COUNT values. We compute the rate and score in Java
     * because business rules change often (e.g., next month the CRITICAL
     * threshold might move from 30% to 25%). Changing a Java constant is
     * instant — changing a DB stored procedure is a deployment risk.
     *
     * Why totalOrders > 0 check?
     * A brand-new product has 0 orders. 0/0 = ArithmeticException = server crash.
     * We default to 0.0 to keep the dashboard alive even for new products.
     */
    public SellerProductQualityDTO(
            Long productId, String productName,
            Long totalOrders, Long totalReturns) {
        this.productId = productId;
        this.productName = productName;
        this.totalOrders = totalOrders;
        this.totalReturns = totalReturns;

        // Compute return rate safely
        if(totalOrders != null && totalOrders > 0){
            this.returnRatePercentage = ((double)totalReturns / totalOrders) * 100;
        }else{
            this.returnRatePercentage = 0.0;
        }

        // Assign quality score — thresholds are business rules, not DB logic
        if (this.returnRatePercentage < 5.0) {
            this.qualityScore = "EXCELLENT";
        } else if (this.returnRatePercentage < 15.0) {
            this.qualityScore = "GOOD";
        } else if (this.returnRatePercentage < 30.0) {
            this.qualityScore = "WARNING";
        } else {
            this.qualityScore = "CRITICAL"; // Seller needs to act immediately
        }

    }
}
