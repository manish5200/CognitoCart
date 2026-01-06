package com.manish.smartcart.dto.product;

import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@ToString
@Builder
public class ProductSearchDTO {

    private String category;

    @DecimalMin(value = "0.0", message = "Minimum price cannot be negative")
    private BigDecimal minPrice;

    @DecimalMin(value = "0.0", message = "Maximum price cannot be negative")
    private BigDecimal maxPrice;
    private Double minRating;
    private String keyword;

}
