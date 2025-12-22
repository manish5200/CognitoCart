package com.manish.smartcart.repository;

import com.manish.smartcart.model.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {

    // Allows us to show a specific user their historical orders
     List<Order> findByUserId(Long userId);

    // Find all orders for a user, newest first
    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);
}
