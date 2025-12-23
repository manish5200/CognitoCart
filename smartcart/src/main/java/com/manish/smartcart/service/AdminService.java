package com.manish.smartcart.service;

import com.manish.smartcart.dto.admin.DashboardResponse;
import com.manish.smartcart.dto.admin.LowStockResponse;
import com.manish.smartcart.dto.admin.TopProductDTO;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    public AdminService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

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

        // 4. Return the combined Dashboard
        return new DashboardResponse(
                revenue != null ? revenue : BigDecimal.ZERO,
                successful,
                canceled,
                lowStockResponse,
                topSellingProducts
        );
    }
}
