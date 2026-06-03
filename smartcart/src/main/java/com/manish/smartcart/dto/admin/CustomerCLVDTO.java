package com.manish.smartcart.dto.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Data
public class CustomerCLVDTO {
    private Long userId;
    private String customerName;
    private Long totalOrders;
    private BigDecimal totalSpent;
    private BigDecimal avgOrderValue; // Computed here, not in the DB
    private LocalDateTime lastOrderDate;

    public CustomerCLVDTO(Long userId, String customerName,
                          Long totalOrders, BigDecimal totalSpent,
                          LocalDateTime lastOrderDate){
        this.userId = userId;
        this.customerName = customerName;
        this.totalOrders = totalOrders;
        this.totalSpent = totalSpent;
        this.lastOrderDate = lastOrderDate;

        // Safe division: if a customer somehow has 0 orders in DB, avoid crash
        this.avgOrderValue = (totalOrders != null && totalOrders > 0)
                ? totalSpent.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

}
