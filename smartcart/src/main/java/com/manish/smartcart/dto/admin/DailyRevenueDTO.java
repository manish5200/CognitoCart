package com.manish.smartcart.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class DailyRevenueDTO {
    private LocalDate date;      // X-Axis (e.g., 2026-03-20)
    private BigDecimal revenue;  // Y-Axis (e.g., 450.00)

    // JPA Projection Constructor: CAST(o.orderDate AS date) returns java.sql.Date
    public DailyRevenueDTO(java.sql.Date sqlDate, BigDecimal revenue) {
        this.date = sqlDate != null ? sqlDate.toLocalDate() : null;
        this.revenue = revenue;
    }
    
    // Fallback if Hibernate 6 resolves it naturally to LocalDate
    public DailyRevenueDTO(LocalDate date, BigDecimal revenue) {
        this.date = date;
        this.revenue = revenue;
    }
}
