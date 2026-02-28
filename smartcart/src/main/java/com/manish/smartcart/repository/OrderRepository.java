package com.manish.smartcart.repository;

import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.model.order.Order;
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

        @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.user.id = :userId ORDER BY o.orderDate DESC")
        List<Order> findByUserIdAndOrderItems(@Param("userId") Long userId);

        @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems " +
                        "WHERE o.orderStatus = :status AND o.orderDate < :threshold")
        List<Order> findByOrderStatusAndOrderDateBeforeWithItems(
                        @Param("status") OrderStatus orderStatus,
                        @Param("threshold") LocalDateTime orderDateBefore);
}
