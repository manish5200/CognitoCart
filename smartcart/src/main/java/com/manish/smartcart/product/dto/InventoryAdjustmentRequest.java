package com.manish.smartcart.product.dto;

import com.manish.smartcart.shared.enums.product.StockAdjustmentReason;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for the Inventory Delta API.
 * <p>
 * ─── WHY DELTA INSTEAD OF ABSOLUTE? ───────────────────────────────────────
 * The classic absolute stock update (PUT { "stockQuantity": 50 }) has a
 * fatal race condition:
 * <p>
 *   1. Seller reads current stock: 100
 *   2. 3 customers buy concurrently → stock is now 97
 *   3. Seller submits restock: PUT { "stockQuantity": 150 }
 *   4. DB blindly sets stock = 150, ERASING the 3 deductions!
 *   5. You now have 3 sold items with inventory that was never decremented.
 * <p>
 * The delta approach is immune to this because the SQL is:
 *   UPDATE ... SET stock = stock + :delta
 * The DB engine applies the delta to whatever the current value is at the
 * moment of the UPDATE, not the value the seller read 10 seconds ago.
 * <p>
 * ─── REASON FIELD ──────────────────────────────────────────────────────────
 * The reason enum creates an audit-friendly log trail:
 *   RESTOCK          → supplier delivery arrived
 *   DAMAGE_WRITE_OFF → broken/lost items removed from inventory
 *   SALE_DEDUCTION   → manual correction after flash sale discrepancy
 *   CORRECTION       → admin override to fix data entry error
 */
@Setter
@Getter
public class InventoryAdjustmentRequest {

    /**
     * Stock change amount. Positive = add stock. Negative = remove stock.
     * Cannot be zero — a zero delta is a no-op and signals a client bug.
     * Examples: +100 (restock), -5 (damage write-off)
     */
    @NotNull(message = "Adjustment amount is required")
    private Integer adjustment;

    /**
     * Business reason for this inventory change.
     * Stored in application logs for audit trail.
     * Required so inventory movements are always traceable.
     */
    @NotNull(message = "Reason is required")
    private StockAdjustmentReason reason;

    /**
     * Optional free-text note (e.g., "Delivery from Supplier ABC on 2026-08-07").
     * Not persisted in DB — only emitted in the audit log.
     */
    private String note;
}
