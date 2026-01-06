package com.manish.smartcart.service;

import com.manish.smartcart.dto.customer.CustomerDashboardDTO;
import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.mapper.OrderMapper;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.repository.OrderRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.List;

@Service
@AllArgsConstructor
public class CustomerService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public CustomerDashboardDTO getCustomerDashboard(Long userId, int pageNumber, int pageSize){
            // 1. Get total order count
            // We use the simple list size or a count query
        Long totalOrder = (long) orderRepository.findByUserId(userId).size();

        // 2. Get total money spent (using our custom DELIVERED query)
        BigDecimal totalSpent = orderRepository.calculateTotalSpentByUser(userId);

        // 3. Get the absolute latest order (for the 'Track' card)
        OrderResponse latestOrderResponse = orderRepository.findFirstByUserIdOrderByOrderDateDesc(userId)
                .map(orderMapper::toOrderResponse)
                .orElse(null);

        // 4. Get the orders for the dashboard preview(set page number or page size)
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("orderDate").descending());
        List<Order>recentOrderList = orderRepository.findByUserId(userId, pageable).getContent();

        List<OrderResponse>recentOrdersResponse = recentOrderList.stream()
                .map(orderMapper::toOrderResponse)
                .toList();

        // 5. Build and return the final DTO
        return new CustomerDashboardDTO(
           totalOrder,
           totalSpent !=null ? totalSpent : BigDecimal.ZERO,
                latestOrderResponse,
                recentOrdersResponse
        );
    }

}
