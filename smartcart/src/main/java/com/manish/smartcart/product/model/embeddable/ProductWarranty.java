package com.manish.smartcart.product.model.embeddable;

import com.manish.smartcart.shared.enums.product.WarrantyDurationUnit;
import com.manish.smartcart.shared.enums.product.WarrantyType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductWarranty {

    @Enumerated(EnumType.STRING)
    @Column(name = "warranty_type", nullable = false)
    @Builder.Default
    private WarrantyType warrantyType = WarrantyType.NONE;

    @Column(name = "warranty_duration")
    private Integer warrantyDuration;

    @Enumerated(EnumType.STRING)
    @Column(name = "warranty_duration_unit")
    private WarrantyDurationUnit warrantyDurationUnit;

}
