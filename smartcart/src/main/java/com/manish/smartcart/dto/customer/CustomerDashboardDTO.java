package com.manish.smartcart.dto.customer;

import com.manish.smartcart.dto.order.OrderResponse;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class CustomerDashboardDTO {

    private Long totalOrders;
    private BigDecimal totalSpent;
    private OrderResponse latestOrder; // For the "Buy it again" or "Track" card
    private List<OrderResponse> recentOrders;

}
