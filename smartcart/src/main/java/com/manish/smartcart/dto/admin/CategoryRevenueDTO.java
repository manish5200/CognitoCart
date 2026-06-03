package com.manish.smartcart.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CategoryRevenueDTO {
    private Long categoryId;
    private String categoryName;
    private Long totalOrders;
    private Long unitsSold;
    private BigDecimal revenue;
}
