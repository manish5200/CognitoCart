package com.manish.smartcart.model.order;

import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.enums.PaymentStatus;
import com.manish.smartcart.enums.ReturnType;
import com.manish.smartcart.model.base.BaseEntity;
import com.manish.smartcart.model.user.Users;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@SuperBuilder
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<OrderItem>();

    private LocalDateTime orderDate;

    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // --- PHASE 1: COUPON SNAPSHOT (IMMUTABLE) ---
    private String couponCode;

    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // --- PHASE 1.5: DELIVERY SNAPSHOT ---
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    // --- PAYMENT STATUS (independent of fulfillment lifecycle) ---
    // PENDING → Razorpay order created, waiting for payment
    // PAID    → Signature verified or webhook confirmed
    // FAILED  → Webhook reported payment.failed
    // REFUNDED → Phase 2: refund issued
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;


    //PAYMENT GATEWAY (RAZORPAY) ---
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    //SHIPPING SNAPSHOT (IMMUTABLE) ---
    //These fields "freeze" the data at the moment of checkout
    private String shippingFullName;
    private String shippingPhone;
    private String shippingStreetAddress;
    private String shippingCity;
    private String shippingState;
    private String shippingZipCode;
    private String shippingCountry;

    // ─── POST-DELIVERY / RETURN FIELDS (V25 Migrations) ───────────────

    /** WHY customer is requesting action: "DEFECTIVE", "WRONG_ITEM", "CHANGED_MIND" */
    private String returnReason;

    /** Customer's optional explanation in their own words */
    private String returnDescription;

    /** When the customer submitted the request — used for audit trail */
    private LocalDateTime returnRequestedAt;

    /**
     * When the carrier marked this order as DELIVERED.
     * Set by ShipmentService.processLogisticsUpdate() when status = DELIVERED.
     * CRITICAL: return window deadline = deliveredAt + policy.returnWindowDays
     */
    private LocalDateTime deliveredAt;

    /**
     * Frozen JSON snapshot of ProductReturnPolicy at checkout time.
     * WHY: If seller changes/deletes policy tomorrow, this order still
     * honors the policy the customer saw when they paid.
     * Same concept as priceAtPurchase in OrderItem.
     */
    @Column(columnDefinition = "jsonb")
    private String returnPolicySnapshot;

    /**
     * What the customer REQUESTED: RETURN, REPLACEMENT, or EXCHANGE.
     * Distinct from PolicyType (seller's rule) — this is customer's intent.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "return_request_type")
    private ReturnType returnRequestType;

}
