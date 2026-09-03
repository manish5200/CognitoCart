package com.manish.smartcart.cart.dto;

import com.manish.smartcart.shared.enums.product.PolicyType;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class CartResponse {

    private Long cartId;
    private BigDecimal totalAmount; // This will now represent the FINAL amount (including delivery)
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;  // <-- NEW FIELD HERE
    private List<ItemDTO> items;
    // A simple Item DTO with NO link back to cart
    @Setter
    @Getter
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class ItemDTO {
        private String productName;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;
        private java.util.UUID variantPublicId;
        private java.util.UUID productPublicId;
        private String imageUrl;
        private String variantInfo;
        private PolicyType policyType;
        private Integer returnWindowDays;
    }
}
