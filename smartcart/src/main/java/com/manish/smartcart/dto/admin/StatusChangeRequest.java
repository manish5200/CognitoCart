package com.manish.smartcart.dto.admin;

import com.manish.smartcart.enums.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StatusChangeRequest {

    @NotNull
    @NotBlank(message = "Order ID is required")
    private Long orderId;

    @NotNull
    private String orderStatus;

    public StatusChangeRequest() {}

    public StatusChangeRequest(Long orderId, String orderStatus) {
        this.orderId = orderId;
        this.orderStatus = orderStatus;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
}
