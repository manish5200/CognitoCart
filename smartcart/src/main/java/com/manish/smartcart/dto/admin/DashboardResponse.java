package com.manish.smartcart.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

//Response for Admin Dashboard
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private BigDecimal totalRevenue;
    private Long successfulOrders;
    private Long canceledOrders;
    //The new "Actionable" list
    //Products with stock < 5
    private List<LowStockResponse> lowStockAlerts;
    private List<TopProductDTO> topSellingProducts; // New field
}
