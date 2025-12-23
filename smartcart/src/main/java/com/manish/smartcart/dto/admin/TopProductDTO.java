package com.manish.smartcart.dto.admin;

import com.manish.smartcart.model.product.Product;

import java.math.BigDecimal;
import java.util.List;

public class TopProductDTO {
    Long productId;
    String productName;
    BigDecimal price;
    private Long totalSold;

    public TopProductDTO() {}

    public TopProductDTO(Long productId, String productName, BigDecimal price, Long totalSold) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.totalSold = totalSold;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getTotalSold() {
        return totalSold;
    }

    public void setTotalSold(Long totalSold) {
        this.totalSold = totalSold;
    }
}
