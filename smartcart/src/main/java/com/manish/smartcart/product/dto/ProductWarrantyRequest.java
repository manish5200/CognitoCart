package com.manish.smartcart.product.dto;

import com.manish.smartcart.shared.enums.product.WarrantyDurationUnit;
import com.manish.smartcart.shared.enums.product.WarrantyType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductWarrantyRequest {
    @NotNull
    private WarrantyType warrantyType;
    private Integer warrantyDuration;
    private WarrantyDurationUnit warrantyDurationUnit;
}
