package com.manish.smartcart.service.order;

import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.mapper.OrderMapper;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    /**
     * Paginated order history for a customer.
     *
     * CONCEPT — Two-query pagination:
     * Query 1: Get paginated ORDER IDs only (fast, no joins, perfect page counts)
     * Query 2: Fetch full order data (with items) for only THIS page's IDs
     * This avoids the N+1 problem AND gets accurate totalElements.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrderHistoryForUser(Long userId, Pageable pageable) {

        // Step 1: Get paginated order IDs from the database (no join, perfect pagination)
        Page<Long> orderIdPage = orderRepository.findOrderIdsByUserId(userId, pageable);
        if (orderIdPage.isEmpty()) {
            return Page.empty(pageable);
        }
        // Step 2: Fetch complete order data (with items) only for this page's IDs
        List<Order> orders = orderRepository.findOrdersWithItemsByIds(orderIdPage.getContent());
        // Map to response DTOs
        List<OrderResponse> responses = orders.stream()
                .map(orderMapper::toOrderResponse)
                .toList();

        // This ensures correct pagination metadata (totalElements, totalPages) in the response
        return new PageImpl<>(
                responses,
                pageable,
                orderIdPage.getTotalElements());
    }
}
