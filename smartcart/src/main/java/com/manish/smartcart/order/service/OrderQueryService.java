package com.manish.smartcart.order.service;

import com.manish.smartcart.order.dto.OrderResponse;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import com.manish.smartcart.shared.mapper.OrderMapper;
import com.manish.smartcart.order.model.Order;
import com.manish.smartcart.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    /**
     * Paginated order history for a customer.
     * <p>
     * CONCEPT — Two-query pagination:
     * Query 1: Get paginated ORDER IDs only (fast, no joins, perfect page counts)
     * Query 2: Fetch full order data (with items) for only THIS page's IDs
     * This avoids the N+1 problem AND gets accurate totalElements.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrderHistoryForUser(Long userId, Pageable pageable) {

        // Step 1: Get paginated order IDs from the database (no join, perfect
        // pagination)
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

        // This ensures correct pagination metadata (totalElements, totalPages) in the
        // response
        return new PageImpl<>(
                responses,
                pageable,
                orderIdPage.getTotalElements());
    }

    /*
     * The Smart Resolver Pattern.
     * Automatically routes the lookup to the correct index based on the string format.
     * <p>
     * WHY THIS IS PRO:
     * Instead of having two separate endpoints (one for UUIDs, one for Human IDs),
     * we build a unified resolving layer. Stripe uses this exact pattern for their 'pi_' and 'ch_' tokens.
     */
    @Transactional(readOnly = true)
    public Order resolveOrder(String identifier){

        // 1. UUID Pattern Match (8-4-4-4-12 hex format)
        // Used by our React/Next.js frontend
        if(identifier.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")){
            return orderRepository.findByPublicId(UUID.fromString(identifier))
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found for UUID: " + identifier));
        }

        // 2. Human ID Fallback (ORD-YYYYMMDD-XXXXXX format)
        // Used by Customer Support portals and email tracking links
        return orderRepository.findByOrderNumber(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found for number: " + identifier));
    }
}
