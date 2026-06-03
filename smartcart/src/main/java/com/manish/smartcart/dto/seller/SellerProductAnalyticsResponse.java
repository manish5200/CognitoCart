package com.manish.smartcart.dto.seller;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SellerProductAnalyticsResponse {

    // Sorted by worst-performing products first (CRITICAL at top)
    // so sellers immediately see what needs fixing — no scrolling required
    private List<SellerProductQualityDTO> products;

    // Summary counts — frontend renders these as KPI cards at the top
    private long criticalCount;   // Products needing immediate attention
    private long warningCount;    // Products to watch
    private long excellentCount;  // Healthy products
}
