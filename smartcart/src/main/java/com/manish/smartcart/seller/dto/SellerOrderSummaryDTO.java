package com.manish.smartcart.seller.dto;

import com.manish.smartcart.shared.enums.OrderStatus;
import com.manish.smartcart.shared.enums.ReturnType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Seller's scoped view of a marketplace order.
 * <p>
 * PRIVACY CONTRACT:
 * - myItems: ONLY this seller's items. Competitor items in the same cart are never exposed.
 * - customerFirstName: First name only — enough for packing slip, not personal contact.
 * - shippingCity/State/Zip: For logistics estimation, NOT full street address.
 * - myItemsTotal: Revenue from THIS seller's items only, with proportional discount applied.
 * <p>
 * REVENUE ACCURACY:
 * If a platform coupon discounts the whole order, the discount is prorated across
 * sellers proportionally to their item value share. This ensures the seller's
 * revenue figure reflects actual settlement amount, not inflated item prices.
 */
@Data
@Builder
public class SellerOrderSummaryDTO{

    // ─── ORDER IDENTITY ───────────────────────────────────────────────────────
    private UUID orderPublicId;
    private String orderNumber;              // ORD-20260816-XXXXX (for packing slips)
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;

    // ACTION REQUIRED flag — true when order is CONFIRMED and needs packing
    // Drives UI badge: "⚠️ 5 orders need action"
    private boolean actionRequired;

    // ─── CUSTOMER (Partial — privacy-compliant) ───────────────────────────────
    private String customerFirstName;        // For packing slip: "Packing for Manish"
    private String shippingCity;
    private String shippingState;
    private String shippingZipCode;


    // ─── SELLER'S ITEMS ONLY ──────────────────────────────────────────────────
    private List<SellerOrderItemDTO> myItems;
    private int myItemsCount;                // Total unit count (for packing label)
    private BigDecimal myItemsGrossTotal;    // Sum before discount proration
    private BigDecimal myItemsNetTotal;      // After proportional coupon discount — actual revenue
    private BigDecimal myDiscountShare;      // Seller's portion of coupon discount (for transparency)

    // ─── RETURN REQUEST (visible to seller if customer raised one) ────────────
    // Seller needs to know: "Customer wants a return on this order"
    private ReturnType returnRequestType;    // RETURN / REPLACEMENT / EXCHANGE
    private LocalDateTime returnRequestedAt;

    // ─── TRACKING ─────────────────────────────────────────────────────────────
    private String trackingNumber;           // null if not yet shipped
    private String trackingUrl;
    private String courierName;
    private LocalDate estimatedDeliveryDate;


    /**
     * One item line — uses frozen checkout snapshots exclusively.
     * Safe to display even if the product was deleted from the catalog after order.
     */
    @Data
    @Builder
    public static class SellerOrderItemDTO{
        private String productName;          // Frozen: productNameSnapshot
        private String variantLabel;         // Frozen: variantLabelSnapshot ("Blue / XL")
        private String sku;                  // Frozen: skuSnapshot (for warehouse routing)
        private Integer quantity;
        private BigDecimal priceAtPurchase;
        private BigDecimal lineTotal;        // priceAtPurchase × quantity
        private String imageUrl;             // Frozen: imageUrlSnapshot
    }
}
