package com.manish.smartcart.shared.enums;

/**
 * Reason codes for inventory delta adjustments.
 * Every stock change must carry a reason for audit traceability.
 */
public enum StockAdjustmentReason {
    RESTOCK,           // Supplier delivery — adds stock
    DAMAGE_WRITE_OFF,  // Broken/lost goods — removes stock
    SALE_DEDUCTION,    // Manual correction after sale event discrepancy
    CORRECTION         // Admin override for data entry error
}
