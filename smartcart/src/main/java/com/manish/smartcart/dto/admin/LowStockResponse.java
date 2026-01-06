package com.manish.smartcart.dto.admin;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class LowStockResponse {
    private Long productId;
    private String productName;
    private Integer currentStock;
    private Long sellerId;
    private String sku; // Stock Keeping Unit - very important for sellers
}
