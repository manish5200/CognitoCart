package com.manish.smartcart.repository;

import com.manish.smartcart.dto.admin.*;
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

/**
 * Primary repository for Order aggregates.
 * Employs tailored fetch strategies (Lazy/Eager/Stream) and DTO projections
 * to handle high-volume transactional reads and complex analytical reporting.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ─── STANDARD LOOKUPS ─────────────────────────────────────────────────────

    /** Lightweight lookup for webhook processing. Excludes heavy associations. */
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    /**
     * Eagerly fetches items and variants to prevent N+1 queries during
     * payment verification and synchronous stock deduction workflows.
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.variant WHERE o.razorpayOrderId = :razorpayOrderId")
    Optional<Order> findByRazorpayOrderIdWithItems(@Param("razorpayOrderId") String razorpayOrderId);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId")
    List<Order> findByUserId(@Param("userId") Long userId);

    Long countByOrderStatus(OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.orderDate DESC")
    Optional<Order> findFirstByUserIdOrderByOrderDateDesc(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId")
    Page<Order> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Verified Purchase Gatekeeper.
     * Business Rule: Validates if a user has purchased ANY variant of a specific master product.
     * Required by ReviewService to prevent fraudulent or unverified product reviews.
     */
    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.orderItems oi " +
            "WHERE o.user.id = :userId AND oi.variant.product.id = :productId AND o.orderStatus = :status")
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


    // ─── SELLER DASHBOARD QUERIES ─────────────────────────────────────────────

    /** Calculates realized revenue from completed (DELIVERED) orders containing seller's SKUs. */
    @Query("SELECT COALESCE(SUM(oi.priceAtPurchase * oi.quantity), 0) " +
            "FROM OrderItem oi JOIN oi.order o " +
            "WHERE oi.variant.product.sellerId = :sellerId AND o.orderStatus = 'DELIVERED'")
    BigDecimal calculateSellerRevenue(@Param("sellerId") Long sellerId);

    /** Calculates in-flight revenue (PAID but not yet DELIVERED) to forecast upcoming payouts. */
    @Query("SELECT COALESCE(SUM(oi.priceAtPurchase * oi.quantity), 0) " +
            "FROM OrderItem oi JOIN oi.order o " +
            "WHERE oi.variant.product.sellerId = :sellerId AND o.orderStatus IN ('CONFIRMED', 'PACKED', 'SHIPPED')")
    BigDecimal calculateSellerPendingRevenue(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(DISTINCT o.id) FROM Order o JOIN o.orderItems oi " +
            "WHERE oi.variant.product.sellerId = :sellerId")
    long countOrdersBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(DISTINCT o.id) FROM Order o JOIN o.orderItems oi " +
            "WHERE oi.variant.product.sellerId = :sellerId AND o.orderStatus = :status")
    long countOrdersBySellerIdAndStatus(@Param("sellerId") Long sellerId,
                                        @Param("status") OrderStatus status);

    Long user(Users user);

    /** Uses DB-level COUNT aggregation, bypassing entity instantiation for performance. */
    long countByUserId(Long userId);

    /**
     * Time-series aggregation for charting.
     * Groups transactional data by day to offload intensive grouping logic from the JVM to the DB.
     */
    @Query("SELECT new com.manish.smartcart.dto.admin.DailyRevenueDTO(CAST(o.orderDate AS date), SUM(o.totalAmount)) " +
            "FROM Order o " +
            "WHERE o.paymentStatus = 'PAID' AND o.orderDate >= :startDate " +
            "GROUP BY CAST(o.orderDate AS date) " +
            "ORDER BY CAST(o.orderDate AS date) ASC")
    List<DailyRevenueDTO> getDailyRevenueTrend(@Param("startDate") LocalDateTime startDate);

    /**
     * Deep Eager Fetch for async workers.
     * Required by message brokers (e.g., RabbitMQ) to safely map the aggregate root outside a Hibernate session.
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.variant.product WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);


    // ─── PAGINATION OPTIMIZATION (Two-Step Fetch) ─────────────────────────────

    /** Step 1: Extracts a lightweight, paginated slice of ID references. */
    @Query(value = "SELECT o.id FROM Order o WHERE o.user.id = :userId ORDER BY o.orderDate DESC",
            countQuery = "SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Page<Long> findOrderIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    /** Step 2: Hydrates the specific slice with full associations, avoiding expensive Cartesian products. */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.variant.product " +
            "WHERE o.id IN :orderIds ORDER BY o.orderDate DESC")
    List<Order> findOrdersWithItemsByIds(@Param("orderIds") List<Long> orderIds);


    // ─── HIGH-VOLUME DATA STREAMING ───────────────────────────────────────────

    /**
     * Cursor-based data stream for bulk exports and reporting.
     * Evades OutOfMemoryErrors by keeping the JVM footprint to batch sizes of 500.
     * Utilizes Hibernate 6 specific hints for read-only, cache-bypassing traversal.
     */
    @QueryHints(value = {
            @QueryHint(name = HibernateHints.HINT_FETCH_SIZE, value = "500"),
            @QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "false"),
            @QueryHint(name = HibernateHints.HINT_READ_ONLY, value = "true")
    })
    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi WHERE oi.variant.product.sellerId = :sellerId AND o.orderStatus = :status")
    Stream<Order> streamBySellerIdAndOrderStatus(
            @Param("sellerId") Long sellerId,
            @Param("status") OrderStatus status);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.variant.product WHERE o.orderStatus IN :statuses")
    List<Order> findByOrderStatusInWithItems(@Param("statuses") List<OrderStatus> statuses);


    // ─── PLATFORM INTELLIGENCE (Direct DTO Projections) ───────────────────────

    /** Total collected liquidity across the platform, including capital later refunded. */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus IN ('PAID', 'REFUNDED')")
    BigDecimal calculatePlatformGrossRevenue();

    /** Capital leakage due to processed refunds. */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = 'REFUNDED'")
    BigDecimal calculatePlatformLostRevenue();

    /** Aggregates return requests by reason code to identify systemic product or fulfillment issues. */
    @Query("SELECT new com.manish.smartcart.dto.admin.ReturnReasonStats(" +
            "o.returnReason, COUNT(o), SUM(o.totalAmount)) " +
            "FROM Order o " +
            "WHERE o.returnReason IS NOT NULL " +
            "GROUP BY o.returnReason" +
            " ORDER BY COUNT(o) DESC")
    List<ReturnReasonStats> getReturnReasonInsights();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.returnRequestedAt IS NOT NULL")
    Long countTotalReturnRequests();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus IN ('REFUNDED', 'RETURNED', 'REPLACEMENT_SHIPPED')")
    Long countApprovedReturns();

    /**
     * Evaluates category performance based purely on finalized (DELIVERED) orders.
     * Business Rule: Calculates yield using frozen checkout prices (priceAtPurchase)
     * rather than active catalog prices to guarantee historical financial accuracy.
     * Note: Bridges through the Variant entity to aggregate at the Master Product Category level.
     */
    @Query("SELECT new com.manish.smartcart.dto.admin.CategoryRevenueDTO(" +
            "c.id, c.name, COUNT(DISTINCT o.id), SUM(oi.quantity), " +
            "SUM(oi.priceAtPurchase * oi.quantity)) " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "JOIN oi.variant v " +
            "JOIN v.product p " +
            "JOIN p.category c " +
            "WHERE o.orderStatus = 'DELIVERED' " +
            "GROUP BY c.id, c.name " +
            "ORDER BY SUM(oi.priceAtPurchase * oi.quantity) DESC")
    List<CategoryRevenueDTO> getRevenueByCategoryStats();

    /**
     * Customer Lifetime Value (CLV) Calculation.
     * Ranks cohorts by finalized spend (DELIVERED only, omitting in-flight capital risk).
     * Accepts Pageable to allow dynamic depth queries from the administrative controllers.
     */
    @Query("SELECT new com.manish.smartcart.dto.admin.CustomerCLVDTO(" +
            "o.user.id, o.user.fullName, COUNT(o), SUM(o.totalAmount), MAX(o.orderDate)) " +
            "FROM Order o " +
            "WHERE o.orderStatus = 'DELIVERED' " +
            "GROUP BY o.user.id, o.user.fullName " +
            "ORDER BY SUM(o.totalAmount) DESC")
    List<CustomerCLVDTO> getTopCustomersByLifetimeValue(Pageable pageable);

    /**
     * Automated Churn Detection.
     * Identifies historically active users whose most recent purchase predates the defined threshold.
     * Includes active orders (PAID/CONFIRMED) to prevent false-positive churn flags on recent buyers.
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

    /**
     * Product Quality and Defect Matrix.
     * Leverages conditional aggregation (CASE WHEN) within the DB engine to evaluate
     * return frequency versus total order volume per master product, surfacing
     * problematic inventory for specific sellers.
     */
    @Query("SELECT new com.manish.smartcart.dto.seller.SellerProductQualityDTO(" +
            "p.id, p.productName, COUNT(oi), " +
            "SUM(CASE WHEN o.returnRequestType IS NOT NULL THEN 1L ELSE 0L END)) " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "JOIN oi.variant v " +
            "JOIN v.product p " +
            "WHERE p.sellerId = :sellerId " +
            "GROUP BY p.id, p.productName " +
            "ORDER BY SUM(CASE WHEN o.returnRequestType IS NOT NULL THEN 1L ELSE 0L END) DESC")
    List<com.manish.smartcart.dto.seller.SellerProductQualityDTO> getProductQualityBySeller(
            @Param("sellerId") Long sellerId);
}