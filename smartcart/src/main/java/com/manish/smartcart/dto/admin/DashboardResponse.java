package com.manish.smartcart.dto.admin;

import java.math.BigDecimal;
import java.util.List;

//Response for Admin Dashboard
public class DashboardResponse {

    private BigDecimal totalRevenue;
    private Long successfulOrders;
    private Long canceledOrders;
    //The new "Actionable" list
    //Products with stock < 5
    private List<LowStockResponse> lowStockAlerts;
    private List<TopProductDTO> topSellingProducts; // New field

    public DashboardResponse() {}

    public DashboardResponse(BigDecimal totalRevenue, Long successfulOrders, Long canceledOrders, List<LowStockResponse> lowStockAlerts,  List<TopProductDTO> topSellingProducts) {
        this.totalRevenue = totalRevenue;
        this.successfulOrders = successfulOrders;
        this.canceledOrders = canceledOrders;
        this.lowStockAlerts = lowStockAlerts;
        this.topSellingProducts = topSellingProducts;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Long getSuccessfulOrders() {
        return successfulOrders;
    }

    public void setSuccessfulOrders(Long successfulOrders) {
        this.successfulOrders = successfulOrders;
    }

    public Long getCanceledOrders() {
        return canceledOrders;
    }

    public void setCanceledOrders(Long canceledOrders) {
        this.canceledOrders = canceledOrders;
    }

    public List<LowStockResponse> getLowStockAlerts() {
        return lowStockAlerts;
    }

    public void setLowStockAlerts(List<LowStockResponse> lowStockAlerts) {
        this.lowStockAlerts = lowStockAlerts;
    }

    public List<TopProductDTO> getTopSellingProducts() {
        return topSellingProducts;
    }
    public void setTopSellingProducts(List<TopProductDTO> topSellingProducts) {
        this.topSellingProducts = topSellingProducts;
    }
}
