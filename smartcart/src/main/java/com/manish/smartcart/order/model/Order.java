package com.manish.smartcart.order.model;

import com.manish.smartcart.shared.enums.OrderStatus;
import com.manish.smartcart.shared.enums.PaymentStatus;
import com.manish.smartcart.shared.enums.ReturnReason;
import com.manish.smartcart.shared.enums.ReturnType;
import com.manish.smartcart.shared.model.BaseEntity;
import com.manish.smartcart.shared.util.HumanIdGenerator;
import com.manish.smartcart.user.model.Users;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@SequenceGenerator(name = "entity_seq", sequenceName = "order_seq", allocationSize = 50)
public class Order extends BaseEntity {

    // ─── TIER 3: HUMAN ID ────────────────────────────────────────────────────────
    // Customer-readable order number printed on invoices and shown in the "My Orders" page.
    // Format: ORD-YYYYMMDD-XXXXXX (e.g. ORD-20260703-K7P2MQ)
    // 32-char safe charset (no I/O/0/1) eliminates phone-call ambiguity errors.
    // Generated once on first INSERT via @PrePersist. Never modified thereafter.
    @Column(name = "order_number", unique = true, nullable = false, length = 33)
    private String orderNumber;



    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

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
    @Enumerated(EnumType.STRING)
    @Column(name = "return_reason", length = 30)
    private ReturnReason returnReason;

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
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String returnPolicySnapshot;

    /**
     * What the customer REQUESTED: RETURN, REPLACEMENT, or EXCHANGE.
     * Distinct from PolicyType (seller's rule) — this is customer's intent.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "return_request_type")
    private ReturnType returnRequestType;

    /**
     * Stores CDN URLs for image proof if the item is defective or wrong.
     */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> returnProofImages = new ArrayList<>();

    // ─── LIFECYCLE HOOKS ─────────────────────────────────────────────────────────
    /*
     * Auto-generates the human-readable order number on first DB insert.
     * Guard clause (null check) is mandatory: prevents accidental overwrite
     * if the entity is ever passed through a second persist in a unit test.
     */
    @PrePersist
    private void generateHumanId(){
        if(this.orderNumber == null){
            this.orderNumber = HumanIdGenerator.generate("ORD");
        }
    }

}
