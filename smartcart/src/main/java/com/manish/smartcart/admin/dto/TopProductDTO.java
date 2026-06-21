package com.manish.smartcart.admin.dto;

import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@ToString
public class TopProductDTO {
    Long productId;
    String productName;
    BigDecimal price;
    private Long totalSold;

}
