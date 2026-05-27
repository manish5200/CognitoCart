package com.manish.smartcart.service;

import com.manish.smartcart.dto.order.PolicySnapshot;
import com.manish.smartcart.enums.PolicyType;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.model.product.ProductReturnPolicy;
import com.manish.smartcart.repository.ProductReturnPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnPolicyService {

    private final ProductReturnPolicyRepository policyRepository;

    // ─── CHAIN OF RESPONSIBILITY ─────────────────────────────────────────────
    /**
     1. Product-level policy? → use it (most specific)
     2. Category-level policy? → use it (fallback)
     3. Neither? → safe hardcoded default: NON_RETURNABLE (never throws NPE)
     4. IMPORTANT: product.categoryId is @Transient — use product.getCategory().getId()
     */

    private ProductReturnPolicy getApplicablePolicy(Product product) {

        // Step 1: Product-specific policy (most specific wins)
        Optional<ProductReturnPolicy> productPolicy = policyRepository
                .findByProduct_Id(product.getId());

        if(productPolicy.isPresent()) {
            log.debug("Product-level return policy found for product ID: {}", product.getId());
            return productPolicy.get();
        }

        // Step 2: Category-level fallback
        if(product.getCategory() != null) {
            Optional<ProductReturnPolicy>categoryPolicy = policyRepository
                    .findByCategory_Id(product.getCategory().getId());

            if(categoryPolicy.isPresent()) {
                log.debug("Category-level return policy found for product ID: {}", product.getId());
                return categoryPolicy.get();
            }
        }

        // Step 3: Safe hardcoded default — NON_RETURNABLE, 0 days
        log.debug("No return policy found for product ID: {}. Using safe default (NON_RETURNABLE).", product.getId());
        ProductReturnPolicy defaultPolicy = new ProductReturnPolicy();
        defaultPolicy.setPolicyType(PolicyType.NON_RETURNABLE);
        defaultPolicy.setReturnWindowDays(0);
        defaultPolicy.setReturnAllowed(false);
        defaultPolicy.setExchangeAllowed(false);
        defaultPolicy.setReplacementAllowed(false);
        defaultPolicy.setPickupAvailable(false);
        return defaultPolicy;
    }

    /**
     * Called at checkout — returns an immutable PolicySnapshot to freeze into the order.
     */
    public PolicySnapshot getPolicySnapshotForCheckout(Product product) {
        return toSnapshot(getApplicablePolicy(product));
    }

    /**
     * Called by GET /api/v1/products/{productId}/return-policy
     * Returns LIVE current policy for product page display.
     */
    public PolicySnapshot getLivePolicyForProduct(Product product) {
        return toSnapshot(getApplicablePolicy(product));
    }

    private PolicySnapshot toSnapshot(ProductReturnPolicy policy) {
        return PolicySnapshot.builder()
                .policyType(policy.getPolicyType())
                .returnWindowDays(policy.getReturnWindowDays())
                .returnAllowed(policy.isReturnAllowed())
                .exchangeAllowed(policy.isExchangeAllowed())
                .replacementAllowed(policy.isReplacementAllowed())
                .pickupAvailable(policy.isPickupAvailable())
                .build();
    }
}
