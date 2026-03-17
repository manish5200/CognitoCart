package com.manish.smartcart.service;

import com.manish.smartcart.dto.customer.CustomerDashboardDTO;
import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.mapper.OrderMapper;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.repository.ShipmentRepository;
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
    private final ShipmentRepository shipmentRepository;

    public CustomerDashboardDTO getCustomerDashboard(Long userId, int pageNumber, int pageSize){
        /*1. Get total order count
         * ❌ OLD: loads ALL orders into memory just to count
         * Long totalOrder = (long) orderRepository.findByUserId(userId).size();
         * ✅ NEW: single COUNT(*) SQL query — O(1) DB operation regardless of order count
        */
        Long totalOrder = orderRepository.countByUserId(userId);

        // 2. Get total money spent (using our custom DELIVERED query)
        BigDecimal totalSpent = orderRepository.calculateTotalSpentByUser(userId);

        // 3. Get the absolute latest order (for the 'Track' card)
        OrderResponse latestOrderResponse = orderRepository.findFirstByUserIdOrderByOrderDateDesc(userId)
                .map(order -> {
                    OrderResponse response = orderMapper.toOrderResponse(order);
                    // CONCEPT: ifPresent = null-safe, only enriches if shipment exists
                    shipmentRepository.findByOrder_Id(order.getId())
                            .ifPresent(shipment -> orderMapper.mapShipment(response, shipment));
                    return response;
                })
                .orElse(null);


        // 4. Get the orders for the dashboard preview(set page number or page size)
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("orderDate").descending());

        List<Order>recentOrderList = orderRepository.findByUserId(userId, pageable).getContent();

        List<OrderResponse> recentOrdersResponse = recentOrderList.stream()
                .map(order -> {
                    OrderResponse response = orderMapper.toOrderResponse(order);
                    shipmentRepository.findByOrder_Id(order.getId())
                            .ifPresent(shipment -> orderMapper.mapShipment(response, shipment));
                    return response;
                })
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
