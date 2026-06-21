package com.manish.smartcart.admin.dto;

import com.manish.smartcart.shared.enums.ReturnReason;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ReturnReasonStats {

    private ReturnReason returnReason;
    private Long count;

    //What is the financial impact of this specific reason?
    private BigDecimal financialImpact;
}
