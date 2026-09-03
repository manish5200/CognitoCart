package com.manish.smartcart.order.dto;

import com.manish.smartcart.shared.enums.OrderStatus;
import com.manish.smartcart.shared.enums.PaymentStatus;
import com.manish.smartcart.shared.enums.ReturnReason;
import com.manish.smartcart.shared.enums.ReturnType;
import com.manish.smartcart.shared.enums.product.PolicyType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class OrderResponse {
    private java.util.UUID orderPublicId;
    private String email;
    private String customerName;
    private UUID customerPublicId;

    private String orderNumber; // Human ID (e.g. ORD-YYYYMMDD-XXXXXX)

    private String shippingPhone;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;     // <-- NEW FIELD HERE
    private OrderStatus status;
    private PaymentStatus paymentStatus;  // ← PENDING / PAID / FAILED / REFUNDED
    private String shippingAddress;

    // Contains tracking data IF the order has been shipped. Will be null otherwise.
    private ShipmentTrackingDTO shipmentTracking;

    // Used by Frontend to initialize Razorpay checkout overlay
    private String razorpayOrderId;
    private String razorpayKeyId;
    // List of the items we ordered
    private List<OrderItemDTO> items;

    private ReturnType returnRequestType;    // RETURN / REPLACEMENT / EXCHANGE
    private ReturnReason returnReason;             // "DEFECTIVE", "WRONG_ITEM", etc.
    private LocalDateTime returnRequestedAt; // When request was submitted

    @Setter
    @Getter
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class OrderItemDTO {
        // Getters and Setters
        private String productName;
        private Integer quantity;
        private BigDecimal priceAtPurchase; // The "Frozen" price
        private BigDecimal subtotal; // quantity * priceAtPurchase

        // Return policy snapshot at the time of order / current
        private PolicyType policyType;
        private Integer returnWindowDays;

        public OrderItemDTO(String productName, Integer quantity, BigDecimal priceAtPurchase) {
            this.productName = productName;
            this.quantity = quantity;
            this.priceAtPurchase = priceAtPurchase;
            // Automatically calculate subtotal for the frontend
            this.subtotal = priceAtPurchase.multiply(BigDecimal.valueOf(quantity));
        }

    }

}
