package com.manish.smartcart.repository;

import com.manish.smartcart.model.product.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    // All active variants for a product — builds the product page variant selector
    List<ProductVariant> findByProductIdAndIsActiveTrue(Long productId);

    // All variants including inactive — for seller management dashboard
    List<ProductVariant> findByProductId(Long productId);

    // SKU lookup — warehouse barcode scan / deduplication
    Optional<ProductVariant> findBySku(String sku);

    // Barcode lookup — for 3PL/POS integrations
    Optional<ProductVariant> findByBarcode(String barcode);

    // Deduplication guards — prevent duplicate SKU/barcode before insert
    boolean existsBySku(String sku);
    boolean existsByBarcode(String barcode);

    /**
     * PESSIMISTIC WRITE lock — used ONLY at checkout stock deduction.
     * Locks the variant row so no concurrent transaction can read or modify
     * this variant's stock until this checkout transaction commits.
     * Prevents the "last unit sold twice" oversell bug.
     *
     * ⚠ Only call from within a @Transactional method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);

    /**
     * Low-stock scanner — used by the scheduled notification job.
     * Checks available stock (gross - reserved) against seller-defined threshold.
     */
    @Query("SELECT pv FROM ProductVariant pv " +
            "WHERE pv.isActive = true " +
            "AND (pv.stockQuantity - pv.reservedQuantity) <= pv.lowStockThreshold")
    List<ProductVariant> findLowStockVariants();

    /**
     * All active variants for a specific seller — seller analytics and management.
     */
    @Query("SELECT pv FROM ProductVariant pv " +
            "WHERE pv.product.sellerId = :sellerId AND pv.isActive = true")
    List<ProductVariant> findBySellerIdAndActive(@Param("sellerId") Long sellerId);
}
