package com.manish.smartcart.service;

import com.manish.smartcart.dto.order.PolicySnapshot;
import com.manish.smartcart.dto.product.ReturnPolicyRequest;
import com.manish.smartcart.dto.product.ReturnPolicyResponse;
import com.manish.smartcart.enums.PolicyType;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.exception.ResourceNotFoundException;
import com.manish.smartcart.model.product.Category;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.model.product.ProductReturnPolicy;
import com.manish.smartcart.repository.CategoryRepository;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.repository.ProductReturnPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnPolicyService {

    private final ProductReturnPolicyRepository policyRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

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

    // ─── SELLER CRUD ──────────────────────────────────────────────────────────
    /**
     * Creates a new return policy.
     * Business rules:
     *  1. Exactly one of productId/categoryId must be set — not both, not neither
     *  2. Seller can only set policy on products they own (sellerId check)
     *  3. Duplicate guard — one policy per product/category
     *  4. NON_RETURNABLE must have all flags false and returnWindowDays = 0
     *  5. Non-NON_RETURNABLE must have returnWindowDays > 0
     */

    @Transactional
    public ReturnPolicyResponse createPolicy(Long sellerId, ReturnPolicyRequest request){

        boolean hasProduct = request.getProductId() != null;
        boolean hasCategory = request.getCategoryId() != null;

        // Rule 1: exactly one target
        if(hasProduct == hasCategory){
            throw new BusinessLogicException(
                    "Provide either productId OR categoryId — not both, not neither.");
        }
        validatePolicyTypeConsistency(request);

        ProductReturnPolicy policy = new ProductReturnPolicy();
        if(hasProduct){
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with ID: " + request.getProductId()));
            // Rule 2: ownership — sellerId is a plain Long on Product
            if(!product.getSellerId().equals(sellerId)){
                throw new BusinessLogicException(
                        "You can only set return policies on your own products.");
            }

            // Rule 3: duplicate guard
            if(policyRepository.findByProduct_Id(request.getProductId()).isPresent()){
                throw new BusinessLogicException(
                        "A return policy already exists for product '"
                                + product.getProductName()
                                + "'. Use PUT /return-policy/{policyId} to update it.");
            }
            policy.setProduct(product);
        }else{

            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with ID: " + request.getCategoryId()));

            // Rule 3: duplicate guard for category
            if (policyRepository.findByCategory_Id(request.getCategoryId()).isPresent()) {
                throw new BusinessLogicException(
                        "A return policy already exists for category '"
                                + category.getName()
                                + "'. Use PUT /return-policy/{policyId} to update it.");
            }
            policy.setCategory(category);
        }

        applyFields(policy,request);

        ProductReturnPolicy saved = policyRepository.save(policy);
        log.info("Return policy created (ID={}) by sellerID={}", saved.getId(), sellerId);

        return toResponse(saved);
    }

    /**
     * Updates an existing product-level policy.
     * Seller can only update policies for their own products.
     */
    @Transactional
    public ReturnPolicyResponse updatePolicy(Long sellerId, Long policyId, ReturnPolicyRequest request){
        // One DB call — fetches policy AND validates seller ownership simultaneously
        ProductReturnPolicy policy = policyRepository.findByIdAndProductSellerId(policyId,sellerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Policy not found or it does not belong to your products."));

        validatePolicyTypeConsistency(request);
        applyFields(policy, request);

        ProductReturnPolicy saved = policyRepository.save(policy);
        log.info("Return policy updated (ID={}) by sellerID={}", policyId, sellerId);
        return toResponse(saved);
    }


    /**
     * Deletes a policy. Product falls back to category or NON_RETURNABLE default.
     */
    @Transactional
    public void deletePolicy(Long sellerId, Long policyId){
        ProductReturnPolicy policy = policyRepository.findByIdAndProductSellerId(policyId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Policy not found or it does not belong to your products."));
        policyRepository.delete(policy);
        log.info("Return policy deleted (ID={}) by sellerID={}", policyId, sellerId);
    }

    /**
     * Returns all product-level return policies configured by this seller.
     */
    @Transactional(readOnly = true)
    public List<ReturnPolicyResponse>getMyPolicies(Long sellerId){
        return policyRepository.findAllByProductSellerId(sellerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns the live applicable policy for a product (public — for product page display).
     */
    public ReturnPolicyResponse getLivePolicyResponse(Long productId){
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with ID: " + productId));
        return toResponse(getApplicablePolicy(product));
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────

    /**
     * NON_RETURNABLE must have all flags false and 0 days.
     * Any other type must have returnWindowDays > 0.
     */
    private void validatePolicyTypeConsistency(ReturnPolicyRequest request) {
        if(request.getPolicyType() == PolicyType.NON_RETURNABLE){
            if(request.isReturnAllowed() || request.isExchangeAllowed() ||
            request.isReplacementAllowed() || request.getReturnWindowDays() > 0){
                throw new BusinessLogicException(
                        "NON_RETURNABLE policy must have returnAllowed=false, exchangeAllowed=false, "
                                + "replacementAllowed=false, and returnWindowDays=0.");
            }
        }else{
            if(request.getReturnWindowDays() == 0){
                throw new BusinessLogicException(
                        "returnWindowDays must be greater than 0 for policy type: "
                                + request.getPolicyType());
            }
        }
    }

    private void applyFields(ProductReturnPolicy policy, ReturnPolicyRequest request) {
        policy.setPolicyType(request.getPolicyType());
        policy.setReturnWindowDays(request.getReturnWindowDays());
        policy.setReturnAllowed(request.isReturnAllowed());
        policy.setExchangeAllowed(request.isExchangeAllowed());
        policy.setReplacementAllowed(request.isReplacementAllowed());
        policy.setPickupAvailable(request.isPickupAvailable());
    }

    private ReturnPolicyResponse toResponse(ProductReturnPolicy policy) {
        boolean isProductLevel = policy.getProduct() != null;
        return ReturnPolicyResponse.builder()
                .policyId(policy.getId())
                .productId(isProductLevel ? policy.getProduct().getId() : null)
                .productName(isProductLevel ? policy.getProduct().getProductName() : null)
                .categoryId(!isProductLevel && policy.getCategory() != null
                        ? policy.getCategory().getId() : null)
                .categoryName(!isProductLevel && policy.getCategory() != null
                        ? policy.getCategory().getName() : null)
                .policyType(policy.getPolicyType())
                .returnWindowDays(policy.getReturnWindowDays())
                .returnAllowed(policy.isReturnAllowed())
                .exchangeAllowed(policy.isExchangeAllowed())
                .replacementAllowed(policy.isReplacementAllowed())
                .pickupAvailable(policy.isPickupAvailable())
                .scope(isProductLevel ? "PRODUCT" : "CATEGORY")
                .build();
    }

}
