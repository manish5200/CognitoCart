package com.manish.smartcart.service;

import com.manish.smartcart.dto.admin.*;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.exception.ResourceNotFoundException;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    // CONCEPT: "Terminal States" — once an order reaches these, it is IMMUTABLE.
    // Just like you cannot un-deliver a package or un-cancel a ticket.
    // We use a Set for O(1) lookup instead of chaining multiple if-else conditions.
    private static final java.util.Set<OrderStatus> IMMUTABLE_STATES = java.util.Set.of(
            OrderStatus.DELIVERED,       // Job done — cannot go back
            OrderStatus.CANCELLED,       // Canceled orders stay canceled
            OrderStatus.RETURNED,        // Item is back at warehouse
            OrderStatus.REFUNDED         // Money returned — audit trail must not change
    );

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getAdminStats(int stockThreshold,int pageNumber,int pageSize) {
        // 1. Calculate Metrics
        BigDecimal revenue = orderRepository.calculateRevenue();// Using your JPQL query
        Long successful = orderRepository.countByOrderStatus(OrderStatus.DELIVERED);
        Long canceled = orderRepository.countByOrderStatus(OrderStatus.CANCELLED);

        // 2. Fetch Low Stock Products (threshold < 5) and  Map to LowStockResponse DTO
        List<Product> products = productRepository.findByStockQuantityLessThan(stockThreshold);
        List<LowStockResponse>lowStockResponse = products.stream()
                .map(product -> new LowStockResponse(
                        product.getId(),
                        product.getProductName(),
                        product.getStockQuantity(),
                        product.getSellerId(),
                        product.getSku()
                )).toList();

        //3. Identify Top Sellers (Requesting top 5) using Pageable
        // Spring Data Pageable handles the LIMIT and OFFSET in the background
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        //Object[] array of Product Class and Total Quant -> from repo
        List<Object[]>topSellersRaw = productRepository.findToSellingProducts(pageable);
        List<TopProductDTO>topSellingProducts = topSellersRaw.stream()
                .map(result ->{
                       Product p = (Product) result[0];
                       Long totalQuantity =  (Long) result[1];
                       return new TopProductDTO(
                               p.getId(),
                               p.getProductName(),
                               p.getPrice(),
                               totalQuantity
                        );
                }).toList();

        // ADD THIS: Calculate the trend for the last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<DailyRevenueDTO> dailyTrend = orderRepository.getDailyRevenueTrend(sevenDaysAgo);
        // 4. Return the combined Dashboard
        return new DashboardResponse(
                revenue != null ? revenue : BigDecimal.ZERO,
                successful,
                canceled,
                lowStockResponse,
                topSellingProducts,
                dailyTrend
        );
    }


    // Play with the order
    public Order changeTheStatusOfOrders(StatusChangeRequest statusChangeRequest) {
        Order order = orderRepository.findById(statusChangeRequest.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with ID: " + statusChangeRequest.getOrderId()));

        // GUARD: Reject any modification attempt on terminal-state orders.
        // This prevents accidental data corruption from admin panel mistakes.

        if (IMMUTABLE_STATES.contains(order.getOrderStatus())) {
            throw new BusinessLogicException(                                 // ✅ HTTP 400
                    "Order #" + order.getId() + " is in a terminal state ("
                            + order.getOrderStatus() + ") and cannot be modified.");
        }

        try {
            OrderStatus newStatus = OrderStatus.valueOf(
                    statusChangeRequest.getOrderStatus().toUpperCase());
            order.setOrderStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new BusinessLogicException(                                 // ✅ HTTP 400
                    "Invalid order status: '" + statusChangeRequest.getOrderStatus()
                            + "'. Valid values: " + Arrays.toString(OrderStatus.values()));
        }
        return orderRepository.save(order);
    }
}
