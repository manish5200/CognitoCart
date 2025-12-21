package com.manish.smartcart.dto;

import java.math.BigDecimal;

public class ProductResponse {
    private Long id;
    private String productName;
    private BigDecimal price;
    private String description;
    private Integer stockAvailable;

    public ProductResponse(Long id, String productName, String description, BigDecimal price, Integer stockAvailable) {
        this.id = id;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.stockAvailable = stockAvailable;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStockAvailable() {
        return stockAvailable;
    }

    public void setStockAvailable(Integer stockAvailable) {
        this.stockAvailable = stockAvailable;
    }
}
