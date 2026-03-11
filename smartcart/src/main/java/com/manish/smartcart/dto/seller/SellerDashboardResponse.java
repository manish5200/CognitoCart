package com.manish.smartcart.dto.seller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full dashboard response for a seller.
 * All stats are scoped to the authenticated seller's own products and orders.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerDashboardResponse {

    // ── Store Identity ─────────────────────────────────────────────────────
    private String storeName;
    private String kycStatus;

    // ── Product Stats ──────────────────────────────────────────────────────
    private long totalProducts; // All active (non-deleted) products
    private long availableProducts; // is_available = true
    private long outOfStockProducts; // stock_quantity = 0

    // ── Revenue ────────────────────────────────────────────────────────────
    private BigDecimal totalRevenue; // From DELIVERED orders only
    private BigDecimal pendingRevenue; // From PLACED + PROCESSING orders

    // ── Order Stats ────────────────────────────────────────────────────────
    private long totalOrders; // All orders containing seller's products
    private long pendingOrders;
    private long deliveredOrders;

    // ── Top Products ───────────────────────────────────────────────────────
    private List<TopProductResponse> topProducts; // Top 5 by units sold

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductResponse {
        private Long productId;
        private String productName;
        private String slug;
        private long unitsSold;
        private BigDecimal revenue;
    }
}
