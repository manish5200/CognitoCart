package com.manish.smartcart.user.service;

import com.manish.smartcart.user.dto.CustomerDashboardDTO;
import com.manish.smartcart.order.dto.OrderResponse;
import com.manish.smartcart.shared.mapper.OrderMapper;
import com.manish.smartcart.order.model.Order;
import com.manish.smartcart.order.repository.OrderRepository;
import com.manish.smartcart.order.repository.ShipmentRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
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
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("orderDate").descending());
        Page<Long> orderIdsPage = orderRepository.findOrderIdsByUserId(userId, pageable);
        List<Order> recentOrderList = orderIdsPage.isEmpty() ?
                List.of() :
                orderRepository.findOrdersWithItemsByIds(orderIdsPage.getContent());
        // Process recent orders mapping
        List<OrderResponse> recentOrdersResponse = recentOrderList.stream()
                .map(order -> {
                    OrderResponse response = orderMapper.toOrderResponse(order);
                    shipmentRepository.findByOrder_Id(order.getId())
                            .ifPresent(shipment -> orderMapper.mapShipment(response, shipment));
                    return response;
                })
                .toList();
        // Safe "Latest Order" extraction:
        // If we are on Page 0, the latest order is literally the first item in recentOrdersResponse.
        // If we are on another page (and recentOrderList didn't fetch the absolute latest), we fall back.
        // Safe "Latest Order" extraction:
        // Declared explicitly final so Java knows it will only be assigned once.
        final OrderResponse finalLatestOrderResponse;
        if (pageNumber == 0 && !recentOrdersResponse.isEmpty()) {
            finalLatestOrderResponse = recentOrdersResponse.get(0);
        } else {
            // Fallback for pages > 0: Fetch JUST the single latest order ID, then hydrate it.
            Page<Long> latestIdPage = orderRepository.findOrderIdsByUserId(userId, PageRequest.of(0, 1, Sort.by("orderDate").descending()));
            if (!latestIdPage.isEmpty()) {
                Order latestOrder = orderRepository.findOrdersWithItemsByIds(latestIdPage.getContent()).get(0);

                // Use a perfectly scoped local variable for the lambda to capture safely
                OrderResponse mappedResponse = orderMapper.toOrderResponse(latestOrder);
                shipmentRepository.findByOrder_Id(latestOrder.getId())
                        .ifPresent(shipment -> orderMapper.mapShipment(mappedResponse, shipment));

                finalLatestOrderResponse = mappedResponse;
            } else {
                finalLatestOrderResponse = null;
            }
        }

        // 5. Build and return the final DTO
        return new CustomerDashboardDTO(
                totalOrder,
                totalSpent != null ? totalSpent : BigDecimal.ZERO,
                finalLatestOrderResponse,
                recentOrdersResponse
        );
    }

}
