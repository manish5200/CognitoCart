package com.manish.smartcart.dto.customer;

import com.manish.smartcart.dto.order.OrderResponse;

import java.math.BigDecimal;
import java.util.List;

public class CustomerDashboardDTO {

    private Long totalOrders;
    private BigDecimal totalSpent;
    private OrderResponse latestOrder; // For the "Buy it again" or "Track" card
    private List<OrderResponse> recentOrders;

    public CustomerDashboardDTO() {}
    public CustomerDashboardDTO(Long totalOrders, BigDecimal totalSpent, OrderResponse latestOrder, List<OrderResponse> recentOrders) {
        this.totalOrders = totalOrders;
        this.totalSpent = totalSpent;
        this.latestOrder = latestOrder;
        this.recentOrders = recentOrders;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public OrderResponse getLatestOrder() {
        return latestOrder;
    }

    public void setLatestOrder(OrderResponse latestOrder) {
        this.latestOrder = latestOrder;
    }

    public List<OrderResponse> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<OrderResponse> recentOrders) {
        this.recentOrders = recentOrders;
    }
}
