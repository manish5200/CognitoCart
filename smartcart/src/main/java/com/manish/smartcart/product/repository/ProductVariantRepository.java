package com.manish.smartcart.product.repository;

import com.manish.smartcart.product.model.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core Domain Repository for SKU/Variant Lifecycle and Inventory Control.
 * <p>
 * HARD DATABASE INDEX REQUIREMENTS:
 * - UNIQUE INDEX (sku)
 * - UNIQUE INDEX (barcode)
 * - UNIQUE INDEX (public_id)
 * - COMPOSITE INDEX (product_id, is_active, sort_order) -> Crucial for default variant lookups
 * - INDEX (seller_id) -> Prevents full table scans on seller dashboards
 */
@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    // Bounded natively by the number of variants a single product has (safe for List)
    List<ProductVariant> findByProductIdAndIsActiveTrue(Long productId);

    // Bounded natively by product. Used for seller management.
    List<ProductVariant> findByProductId(Long productId);

    // Strict uniqueness guards for catalog integrity
    Optional<ProductVariant> findBySku(String sku);
    Optional<ProductVariant> findByBarcode(String barcode);
    boolean existsBySku(String sku);
    boolean existsByBarcode(String barcode);

    /**
     * CONCURRENCY GUARD: PESSIMISTIC WRITE
     * <p>
     * Enforces a database-level row lock (SELECT ... FOR UPDATE).
     * Strictly reserved for the Cart/Checkout Saga to guarantee inventory atomicity.
     * Prevents the "last unit sold twice" race condition during high-traffic flash sales.
     * Must be executed within a tightly scoped @Transactional boundary.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);

    /**
     * MEMORY-SAFE BATCH PROCESSOR: Low Stock Scanner
     * <p>
     * Uses 'Slice' to process notifications in chunks, preventing JVM Heap exhaustion.
     * <p>
     * DB PERFORMANCE WARNING (Sargability):
     * The expression `(pv.stockQuantity - pv.reservedQuantity)` prevents standard B-Tree
     * index usage, forcing a table scan.
     * Long-term fix for catalog > 1M items: Implement a materialized column
     * `available_quantity` managed via DB triggers, and index that directly.
     */
    @Query("SELECT pv FROM ProductVariant pv " +
            "WHERE pv.isActive = true " +
            "AND (pv.stockQuantity - pv.reservedQuantity) <= pv.lowStockThreshold")
    Slice<ProductVariant> findLowStockVariants(Pageable pageable);

    /**
     * Seller Analytics & Dashboard Feed.
     * Forces Pageable to prevent mega-sellers (100k+ SKUs) from crashing the server heap
     * on dashboard loads. Page allows the UI to render total count metadata.
     */
    @Query("SELECT pv FROM ProductVariant pv " +
            "WHERE pv.product.sellerId = :sellerId AND pv.isActive = true")
    Page<ProductVariant> findBySellerIdAndActive(@Param("sellerId") Long sellerId, Pageable pageable);

    /**
     * External API Security Lookup (IDOR mitigation).
     */
    Optional<ProductVariant> findByPublicId(UUID publicId);

    /**
     * DB-level Limit Extraction.
     * Avoids pulling all variants into memory to find the default SKU.
     * Executes natively as: ORDER BY sort_order ASC LIMIT 1.
     */
    Optional<ProductVariant> findFirstByProductIdAndIsActiveTrueOrderBySortOrderAsc(Long productId);
}