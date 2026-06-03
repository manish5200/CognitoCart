package com.manish.smartcart.service;

import com.manish.smartcart.dto.seller.SellerDashboardResponse;
import com.manish.smartcart.dto.seller.SellerProductAnalyticsResponse;
import com.manish.smartcart.dto.seller.SellerProductQualityDTO;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.model.user.SellerProfile;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Handles seller-specific business logic including the dashboard stats.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final SellerProfileRepository sellerProfileRepository;

    /**
     * Build a comprehensive dashboard for the authenticated seller.
     * All stats are scoped strictly to this seller's products and orders.
     */
    @Transactional(readOnly = true)
    public SellerDashboardResponse getDashboard(Long sellerId) {
        log.info("Building seller dashboard for sellerId={}", sellerId);

        // ── Store identity ────────────────────────────────────────────────
        SellerProfile profile = sellerProfileRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller profile not found for id: " + sellerId));

        // ── Product Stats ─────────────────────────────────────────────────
        long totalProducts = productRepository.countBySellerIdAndNotDeleted(sellerId);
        long available = productRepository.countAvailableBySellerIdAndNotDeleted(sellerId);
        long outOfStock = productRepository.countOutOfStockBySellerId(sellerId);

        // ── Revenue ───────────────────────────────────────────────────────
        BigDecimal totalRevenue = orderRepository.calculateSellerRevenue(sellerId);
        BigDecimal pendingRevenue = orderRepository.calculateSellerPendingRevenue(sellerId);

        // ── Order Stats ───────────────────────────────────────────────────
        long totalOrders = orderRepository.countOrdersBySellerId(sellerId);
        // Pending = paid but not yet delivered (CONFIRMED, PACKED, SHIPPED)
        long pendingOrders = orderRepository.countOrdersBySellerIdAndStatus(sellerId, OrderStatus.CONFIRMED)
                + orderRepository.countOrdersBySellerIdAndStatus(sellerId, OrderStatus.PACKED)
                + orderRepository.countOrdersBySellerIdAndStatus(sellerId, OrderStatus.SHIPPED);
        long deliveredOrders = orderRepository.countOrdersBySellerIdAndStatus(sellerId, OrderStatus.DELIVERED);

        // ── Top 5 Products ────────────────────────────────────────────────
        List<Object[]> rawTop = productRepository.findTopProductsBySellerId(
                sellerId, PageRequest.of(0, 5));

        List<SellerDashboardResponse.TopProductResponse> topProducts = rawTop.stream()
                .map(row -> {
                    Product product = (Product) row[0];
                    long unitsSold = ((Number) row[1]).longValue();
                    BigDecimal revenue = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
                    return SellerDashboardResponse.TopProductResponse.builder()
                            .productId(product.getId())
                            .productName(product.getProductName())
                            .slug(product.getSlug())
                            .unitsSold(unitsSold)
                            .revenue(revenue)
                            .build();
                })
                .toList();

        return SellerDashboardResponse.builder()
                .storeName(profile.getStoreName())
                .kycStatus(profile.getKycStatus().name())
                .totalProducts(totalProducts)
                .availableProducts(available)
                .outOfStockProducts(outOfStock)
                .totalRevenue(totalRevenue)
                .pendingRevenue(pendingRevenue)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .deliveredOrders(deliveredOrders)
                .topProducts(topProducts)
                .build();
    }

    // ─── 3D: PRODUCT QUALITY ANALYTICS ──────────────────────────────────────
    /**
     * Fetches product quality scores for the authenticated seller and
     * computes summary KPI counts so the frontend can render dashboard cards.
     *
     * Why compute counts in Java instead of 3 separate DB queries?
     * We already have the full product list in memory. Streaming it once
     * with a Java stream costs nothing extra. 3 extra DB round-trips for
     * COUNT queries would add unnecessary network latency.
     */
    public SellerProductAnalyticsResponse getProductQualityAnalytics(Long sellerId){
        List<SellerProductQualityDTO> products =
                orderRepository.getProductQualityBySeller(sellerId);

        // Compute summary counts in a single stream pass — O(n), no extra queries
        long critical = products.stream().filter(p -> "CRITICAL".equals(p.getQualityScore())).count();
        long warning   = products.stream().filter(p -> "WARNING".equals(p.getQualityScore())).count();
        long excellent = products.stream().filter(p -> "EXCELLENT".equals(p.getQualityScore())).count();

        return new SellerProductAnalyticsResponse(products, critical, warning, excellent);
    }
}
