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
public interface OrderRepository extends JpaRepository<Order,Long> {

    // Allows us to show a specific user their historical orders
     List<Order> findByUserId(Long userId);

    Long countByOrderStatus(OrderStatus status);

    //CUSTOMER DASHBOARD
    // Get the most recent order for a user
    Optional<Order> findFirstByUserIdOrderByOrderDateDesc(Long userId);


    // Get paginated history for a user
    Page<Order> findByUserId(Long userId, Pageable pageable);


    boolean existsByUserIdAndOrderItems_Product_IdAndOrderStatus(Long userId,
                                                                 Long productId,
                                                                 OrderStatus orderStatus);

    //ADMIN DASHBOARD
    @Query("SELECT SUM(o.total) FROM Order o WHERE o.orderStatus = 'DELIVERED'")
    BigDecimal calculateRevenue();

    // Sum total spent by a specific user (DELIVERED ONLY)
    @Query("SELECT SUM(o.total) FROM " +
            "Order o WHERE" +
            " o.user.id = :userId " +
            "AND o.orderStatus = 'DELIVERED'")
    BigDecimal calculateTotalSpentByUser(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems " +
            "WHERE o.user.id=:userId ORDER BY o.orderDate DESC")
    List<Order>findByUserIdAndOrderItems(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems " +
            "WHERE o.orderStatus = :status AND o.orderDate < :threshold")
    List<Order> findByOrderStatusAndOrderDateBeforeWithItems(@Param("status") OrderStatus orderStatus,
                                                             @Param("threshold") LocalDateTime orderDateBefore);
}
