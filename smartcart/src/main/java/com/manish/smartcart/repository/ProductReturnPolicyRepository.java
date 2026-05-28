package com.manish.smartcart.repository;

import com.manish.smartcart.model.product.ProductReturnPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Finds all product-level policies owned by a seller.
     * NOTE: Product.sellerId is a plain Long — no join to Users needed.
     * Used by GET /api/v1/sellers/return-policy
     */
    @Query("Select p from ProductReturnPolicy p where p.product.sellerId= :sellerId")
    List<ProductReturnPolicy> findAllByProductSellerId(@Param("sellerId") Long sellerId);

    /**
     * Ownership validation for update/delete.
     * Checks the policy exists AND its product belongs to this seller.
     * Returns empty Optional if either condition fails — clean 404, no leaking info.
     */
    @Query("select p from ProductReturnPolicy p where p.id = :policyId AND p.product.sellerId= :sellerId")
    Optional<ProductReturnPolicy> findByIdAndProductSellerId(@Param("policyId") Long policyId,
                                                             @Param("sellerId") Long sellerId);
}
