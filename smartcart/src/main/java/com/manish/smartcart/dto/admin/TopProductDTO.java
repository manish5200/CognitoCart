package com.manish.smartcart.dto.admin;

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
