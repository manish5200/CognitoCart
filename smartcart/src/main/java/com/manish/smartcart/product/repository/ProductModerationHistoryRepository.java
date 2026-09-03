package com.manish.smartcart.product.repository;

import com.manish.smartcart.product.model.ProductModerationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The immutable ledger for tracking product approvals and rejections.
 */
@Repository
public interface ProductModerationHistoryRepository extends JpaRepository<ProductModerationHistory, Long> {

    /**
     * Fetches the complete audit trail for a product.
     * Edge Case Guard: Ordered by createdAt DESC so the most recent action
     * (e.g., the current Rejection Reason) is always index 0.
     */
    List<ProductModerationHistory> findByProductIdOrderByCreatedAtDesc(Long productId);
}
