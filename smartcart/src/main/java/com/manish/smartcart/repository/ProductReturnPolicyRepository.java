package com.manish.smartcart.repository;

import com.manish.smartcart.model.product.ProductReturnPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductReturnPolicyRepository extends JpaRepository<ProductReturnPolicy, Long> {
    /**
     * Finds the policy tied directly to a specific product.
     * Used as STEP 1 in the resolution chain.
     */
    Optional<ProductReturnPolicy> findByProduct_Id(Long productId);
    /**
     * Finds the category-level fallback policy.
     * Used as STEP 2 in the resolution chain.
     */
    Optional<ProductReturnPolicy> findByCategory_Id(Long categoryId);
}
