package com.manish.smartcart.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CustomerIntelligenceResponse {
    // Top spenders — protect these at all costs
    private List<CustomerCLVDTO> topCustomers;
    // Customers who are going quiet — act before they're gone
    private List<CustomerChurnRiskDTO> atRiskCustomers;
}
