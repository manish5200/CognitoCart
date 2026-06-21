package com.manish.smartcart.sale.repository;

import com.manish.smartcart.shared.enums.ApprovalStatus;
import com.manish.smartcart.sale.model.FlashSaleItem;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, Long> {

    /**
     * Fetches all approved items for a specific event. Highly cacheable in Redis.
     */
    List<FlashSaleItem> findByPlatformSaleEventIdAndApprovalStatus(Long eventId, ApprovalStatus status);

    /**
     * MAGIC QUERY FOR CART SERVICE:
     * Instantly checks if a given variant is part of an ACTIVE event and is APPROVED.
     * Prevents sellers from bypassing rules if the parent event ends.
     */
    @Query("SELECT f FROM FlashSaleItem f JOIN f.platformSaleEvent e" +
            " WHERE f.productVariant.id = :variantId " +
            "AND f.approvalStatus = 'APPROVED' " +
            "AND e.status = 'ACTIVE'")
    Optional<FlashSaleItem>findActiveDiscountForVariant(@Param("variantId") Long variantId);

    /**
     * ELITE ANTI-RACE CONDITION:
     * Modifies the usedUnits directly in the database atomically without loading the entity into Hibernate memory.
     * Guaranteed to prevent overselling even if 10,000 users check out in the exact same millisecond.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FlashSaleItem f SET f.usedUnits = f.usedUnits + :quantity " +
            "WHERE f.id = :itemId AND (f.usedUnits + :quantity) <= f.maxUnits")
    int atomicallyIncrementUsedUnits(@Param("itemId") Long itemId, @Param("quantity") int quantity);

    // Allows sellers to view their submitted items
    List<FlashSaleItem>findBySellerId(Long sellerId);
}
