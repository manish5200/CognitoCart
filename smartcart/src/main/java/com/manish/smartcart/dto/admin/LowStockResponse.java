package com.manish.smartcart.dto.admin;

public class LowStockResponse {
    private Long productId;
    private String productName;
    private Integer currentStock;
    private Long sellerId;
    private String sku; // Stock Keeping Unit - very important for sellers

    public LowStockResponse() {
    }

    public LowStockResponse(Long productId, String productName, Integer currentStock, Long sellerId, String sku) {
        this.productId = productId;
        this.productName = productName;
        this.currentStock = currentStock;
        this.sellerId = sellerId;
        this.sku = sku;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
}
