package com.manish.smartcart.repository;

import com.manish.smartcart.dto.admin.DailyRevenueDTO;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.model.user.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

}
