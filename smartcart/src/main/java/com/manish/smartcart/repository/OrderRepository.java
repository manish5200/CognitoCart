package com.manish.smartcart.repository;

import com.manish.smartcart.dto.admin.*;
import com.manish.smartcart.dto.seller.SellerProductQualityDTO;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.model.user.Users;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Plain lookup — used by webhook (no mapper call needed)
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    // Eager fetch with JOIN FETCH — used by /verify which calls OrderMapper
    // and needs orderItems to be loaded before the Hibernate session closes
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.razorpayOrderId = :razorpayOrderId")
    Optional<Order> findByRazorpayOrderIdWithItems(@Param("razorpayOrderId") String razorpayOrderId);

    // Order has @ManyToOne to Users, so use o.user.id in JPQL
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId")
    List<Order> findByUserId(@Param("userId") Long userId);

    Long countByOrderStatus(OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.orderDate DESC")
    Optional<Order> findFirstByUserIdOrderByOrderDateDesc(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId")
    Page<Order> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.orderItems oi " +
            "WHERE o.user.id = :userId AND oi.product.id = :productId AND o.orderStatus = :status")
    boolean existsByUserIdAndOrderItems_Product_IdAndOrderStatus(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("status") OrderStatus orderStatus);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.orderStatus = 'DELIVERED'")
    BigDecimal calculateRevenue();

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.user.id = :userId AND o.orderStatus = 'DELIVERED'")
    BigDecimal calculateTotalSpentByUser(@Param("userId") Long userId);

    @Query(value = "SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.user.id = :userId",
           countQuery = "SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Page<Order> findByUserIdAndOrderItems(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems " +
            "WHERE o.orderStatus = :status AND o.orderDate < :threshold")
    List<Order> findByOrderStatusAndOrderDateBeforeWithItems(
            @Param("status") OrderStatus orderStatus,
            @Param("threshold") LocalDateTime orderDateBefore);



    // ── Seller Dashboard Queries ────────────────────────────────────────────
    /**
     * Revenue from DELIVERED orders that contain at least one of the seller's
     * products
     */
    @Query("SELECT COALESCE(SUM(oi.priceAtPurchase * oi.quantity), 0) " +
            "FROM OrderItem oi JOIN oi.order o " +
            "WHERE oi.product.sellerId = :sellerId AND o.orderStatus = 'DELIVERED'")
    BigDecimal calculateSellerRevenue(@Param("sellerId") Long sellerId);

    /** Revenue from in-flight orders (paid but not yet delivered) */
    @Query("SELECT COALESCE(SUM(oi.priceAtPurchase * oi.quantity), 0) " +
            "FROM OrderItem oi JOIN oi.order o " +
            "WHERE oi.product.sellerId = :sellerId AND o.orderStatus IN ('CONFIRMED', 'PACKED', 'SHIPPED')")
    BigDecimal calculateSellerPendingRevenue(@Param("sellerId") Long sellerId);

    /**
     * Count of all distinct orders containing at least one of the seller's products
     */
    @Query("SELECT COUNT(DISTINCT o.id) FROM Order o JOIN o.orderItems oi " +
            "WHERE oi.product.sellerId = :sellerId")
    long countOrdersBySellerId(@Param("sellerId") Long sellerId);

    /** Count of orders in a specific status containing seller's products */
    @Query("SELECT COUNT(DISTINCT o.id) FROM Order o JOIN o.orderItems oi " +
            "WHERE oi.product.sellerId = :sellerId AND o.orderStatus = :status")
    long countOrdersBySellerIdAndStatus(@Param("sellerId") Long sellerId,
            @Param("status") OrderStatus status);

    Long user(Users user);

    // CONCEPT: Spring Data generates → SELECT COUNT(*) FROM orders WHERE user_id = ?
    // Far more efficient than loading all rows just to call .size() on them.
    long countByUserId(Long userId);

    // ── Seller Dashboard Queries ────────────────────────────────────────────
    // COMPLEX JPQL: Groups orders by Date and sums the revenue.
    // Highly optimized DB-level aggregation for rendering charts.
    @Query("SELECT new com.manish.smartcart.dto.admin.DailyRevenueDTO(CAST(o.orderDate AS date), SUM(o.totalAmount)) " +
            "FROM Order o " +
            "WHERE o.paymentStatus = 'PAID' AND o.orderDate >= :startDate " +
            "GROUP BY CAST(o.orderDate AS date) " +
            "ORDER BY CAST(o.orderDate AS date) ASC")
    List<DailyRevenueDTO> getDailyRevenueTrend(@Param("startDate") java.time.LocalDateTime startDate);

    // Eagerly fetches Order details AND Items AND Products in a single SQL query
    // This is required for our async RabbitMQ worker to safely map the Order to OrderResponse
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);


    // Step 1: Get a paginated slice of ORDER IDs only — no joins, perfect for pagination
    @Query(value = "SELECT o.id FROM Order o WHERE o.user.id = :userId ORDER BY o.orderDate DESC",
            countQuery = "SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Page<Long> findOrderIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    // Step 2: Fetch full order data WITH items for a specific set of IDs
// IN clause is safe here because it's bounded to the page size (e.g., 10 IDs max)
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product " +
            "WHERE o.id IN :orderIds ORDER BY o.orderDate DESC")
    List<Order> findOrdersWithItemsByIds(@Param("orderIds") List<Long> orderIds);

    /**
     * ULTRA-PRODUCTION: True Data Streaming.
     * Uses a cursor to fetch 500 rows at a time, keeping JVM memory footprint near zero.
     * Note: Uses org.hibernate.jpa.HibernateHints for Spring Boot 3 / Hibernate 6 compatibility.
     */
    @QueryHints(value = {
            @QueryHint(name = HibernateHints.HINT_FETCH_SIZE, value = "500"),
            @QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "false"),
            @QueryHint(name = HibernateHints.HINT_READ_ONLY, value = "true")
    })
    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi WHERE oi.product.sellerId = :sellerId AND o.orderStatus = :status")
    Stream<Order> streamBySellerIdAndOrderStatus(
            @Param("sellerId") Long sellerId,
            @Param("status") OrderStatus status);

    @Query("select distinct o from Order o left join fetch " +
            "o.orderItems oi left join fetch oi.product where o.orderStatus IN :statuses")
    List<Order>findByOrderStatusInWithItems(@Param("statuses") List<OrderStatus> statuses);


    // 3 ─── PLATFORM INTELLIGENCE QUERIES (INDUSTRY STANDARD) ───────────────

    // 1. Gross Revenue (Total collected from all PAID orders, including those later refunded)
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus IN ('PAID', 'REFUNDED')")
    BigDecimal calculatePlatformGrossRevenue();

    // 2. Lost Revenue (Money we had to give back because of refunds)
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = 'REFUNDED'")
    BigDecimal calculatePlatformLostRevenue();

    // 3. Return Reason Insights (DTO Projection - mapping DB math directly to our Java Object)
    @Query("SELECT new com.manish.smartcart.dto.admin.ReturnReasonStats(" +
            "o.returnReason, COUNT(o), SUM(o.totalAmount)) " +
            "FROM Order o " +
            "WHERE o.returnReason IS NOT NULL " +
            "GROUP BY o.returnReason" +
            " ORDER BY COUNT(o) DESC")
    List<ReturnReasonStats>getReturnReasonInsights();

    // 4. Return Funnel Stats
    @Query("SELECT COUNT(o) FROM Order o WHERE o.returnRequestedAt IS NOT NULL")
    Long countTotalReturnRequests();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus IN ('REFUNDED', 'RETURNED', 'REPLACEMENT_SHIPPED')")
    Long countApprovedReturns();

    // ─── 3B: REVENUE BY CATEGORY ────────────────────────────────────────────

    /**
     * Aggregates DELIVERED order revenue, grouped by product category.

     * Why DELIVERED only? Because PAID → CONFIRMED orders haven't completed
     * the business transaction yet. Revenue is only "real" when the product
     * is physically in the customer's hands.

     * Why oi.priceAtPurchase instead of p.price?
     * Products change price over time. priceAtPurchase is the FROZEN snapshot
     * from the moment of checkout — this is the actual money that was charged.
     * Using live product price would give wrong historical numbers.

     * DTO Projection: We use 'new CategoryRevenueDTO(...)' so Hibernate
     * constructs our Java object directly from DB math — no entity loading needed.
     */
    @Query("SELECT new com.manish.smartcart.dto.admin.CategoryRevenueDTO(" +
            "c.id, c.name, COUNT(DISTINCT o.id), SUM(oi.quantity), " +
            "SUM(oi.priceAtPurchase * oi.quantity)) " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "JOIN oi.product p " +
            "JOIN p.category c " +
            "WHERE o.orderStatus = 'DELIVERED' " +
            "GROUP BY c.id, c.name " +
            "ORDER BY SUM(oi.priceAtPurchase * oi.quantity) DESC")
    List<CategoryRevenueDTO>getRevenueByCategoryStats();

    // ─── 3C: CUSTOMER INTELLIGENCE QUERIES ──────────────────────────────────

    /**
     * CLV Query — Top customers ranked by lifetime spend.

     * Why DELIVERED only?
     * We count money that has FULLY changed hands. A PAID order still in transit
     * isn't "earned" revenue — the customer could cancel. DELIVERED = done deal.

     * Why Pageable instead of hardcoding LIMIT 10?
     * The controller passes "top=10" or "top=20" dynamically.
     * Pageable lets the admin decide how deep they want to look — no code change needed.

     * COUNT(o) = number of completed orders by that customer.
     * SUM(o.totalAmount) = every rupee they've ever spent.
     * MAX(o.orderDate) = when they last bought — critical for churn detection.
     */
    @Query("SELECT new com.manish.smartcart.dto.admin.CustomerCLVDTO(" +
            "o.user.id, o.user.fullName, COUNT(o), SUM(o.totalAmount), MAX(o.orderDate)) " +
            "FROM Order o " +
            "WHERE o.orderStatus = 'DELIVERED' " +
            "GROUP BY o.user.id, o.user.fullName " +
            "ORDER BY SUM(o.totalAmount) DESC")
    List<CustomerCLVDTO> getTopCustomersByLifetimeValue(Pageable pageable);

    /**
     * Churn Risk Query — Customers who've gone quiet.

     * The HAVING clause is the key: it filters AFTER grouping.
     * Regular WHERE runs before grouping (on individual rows).
     * HAVING runs AFTER grouping (on the result of COUNT/SUM/MAX).

     * HAVING MAX(o.orderDate) < :churnThreshold means:
     * "Give me only groups (customers) whose most recent order
     * was before the cutoff date" — exactly what we want.

     * We include PAID/CONFIRMED too so recently-active customers aren't flagged.
     * ORDER BY ASC = longest-absent customers listed first (highest priority).
     */
    @Query("SELECT new com.manish.smartcart.dto.admin.CustomerChurnRiskDTO(" +
            "o.user.id, o.user.fullName, MAX(o.orderDate), SUM(o.totalAmount)) " +
            "FROM Order o " +
            "WHERE o.orderStatus IN ('DELIVERED', 'PAID', 'CONFIRMED') " +
            "GROUP BY o.user.id, o.user.fullName " +
            "HAVING MAX(o.orderDate) < :churnThreshold " +
            "ORDER BY MAX(o.orderDate) ASC")
    List<CustomerChurnRiskDTO> getChurnRiskCustomers(
            @Param("churnThreshold") LocalDateTime churnThreshold);

    // ─── 3D: SELLER PRODUCT QUALITY SCORE ───────────────────────────────────
    /**
     * Groups order items by product for a specific seller, counting total
     * orders and returns for each product.
     *
     * The CASE WHEN trick:
     * SQL can't directly "count only rows where X is true" — that's a filter.
     * But CASE WHEN returns a value per row, and SUM adds them up.
     * CASE WHEN returnRequestType IS NOT NULL THEN 1 ELSE 0 means:
     *   → returned item contributes 1 to the sum
     *   → normal item contributes 0 to the sum
     * Result: totalReturns = count of items that were returned. Elegant and fast.
     *
     * ORDER BY totalReturns DESC puts worst-performing products first
     * so sellers see their biggest problems at the top of the list.
     */

    @Query("SELECT new com.manish.smartcart.dto.seller.SellerProductQualityDTO(" +
            "p.id, p.productName, COUNT(oi), " +
            "SUM(CASE WHEN o.returnRequestType IS NOT NULL THEN 1L ELSE 0L END)) " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "JOIN oi.product p " +
            "WHERE p.sellerId = :sellerId " +
            "GROUP BY p.id, p.productName " +
            "ORDER BY SUM(CASE WHEN o.returnRequestType IS NOT NULL THEN 1L ELSE 0L END) DESC")
    List<com.manish.smartcart.dto.seller.SellerProductQualityDTO> getProductQualityBySeller(
            @Param("sellerId") Long sellerId);
}


