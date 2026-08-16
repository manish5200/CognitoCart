package com.manish.smartcart.seller.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Rich paginated response wrapper for seller's order list.
 *
 * WHY a custom wrapper instead of raw Page<T>:
 * Spring's Page<T> returns pagination metadata but no business context.
 * Sellers need status counts and revenue summaries alongside the order list
 * — fetched in one call, not three separate API hits.
 *
 * This enables a sidebar like Amazon Seller Central:
 *   ┌─────────────────────────┐
 *   │ ⚠️  Confirmed: 12        │
 *   │ 📦 Packed: 3            │
 *   │ 🚚 Shipped: 28          │
 *   │ ✅ Delivered: 145       │
 *   └─────────────────────────┘
 */
@Data
@Builder
public class SellerOrderPageResponse {

    // ─── Paginated order list ─────────────────────────────────────────────────
    private List<SellerOrderSummaryDTO> orders;

    // ─── Pagination metadata ──────────────────────────────────────────────────
    private int currentPage;
    private int pageSize;
    private long totalOrders;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    // ─── Status breakdown (for sidebar badges / action prompts) ───────────────
    // e.g., { "CONFIRMED": 12, "SHIPPED": 28, "DELIVERED": 145 }
    private Map<String, Long> statusCounts;

 
    // ─── Revenue snapshot for this filtered view ──────────────────────────────
    private BigDecimal filteredViewRevenue; // Net revenue of orders shown in current filter
    private long actionRequiredCount;       // Orders needing seller action (CONFIRMED status)
}
