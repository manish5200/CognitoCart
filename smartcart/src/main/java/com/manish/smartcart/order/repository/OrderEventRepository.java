package com.manish.smartcart.order.repository;

import com.manish.smartcart.order.model.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    /**
     * Fetches all events for an order in chronological order (oldest first).
     * ASC sort is critical — a timeline must always read top-to-bottom in time.
     * Uses the idx_order_events_order_id index for O(log n) lookup.
     */
    List<OrderEvent> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
