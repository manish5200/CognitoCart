package com.manish.smartcart.service.order;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.dto.order.PolicySnapshot;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.enums.PolicyType;
import com.manish.smartcart.enums.ReturnReason;
import com.manish.smartcart.enums.ReturnType;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.exception.ResourceNotFoundException;
import com.manish.smartcart.mapper.OrderMapper;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.model.order.OrderItem;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.service.CloudinaryService;
import com.manish.smartcart.service.ReturnPolicyService;
import com.manish.smartcart.service.notifications.OrderNotificationService;
import com.manish.smartcart.util.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SRP: Responsible ONLY for the customer-facing return/replacement/exchange lifecycle.
 * Reason to change: return window rules, image proof policy, policy matrix logic.

 * Policy matrix enforced here:
 *  PolicyType           | RETURN | REPLACEMENT | EXCHANGE
 *  ---------------------|--------|-------------|--------
 *  NON_RETURNABLE       |  ❌    |     ❌      |   ❌
 *  RETURN_ONLY          |  ✅    |     ❌      |   ❌
 *  EXCHANGE_ONLY        |  ❌    |     ❌      |   ✅
 *  REPLACEMENT_ONLY     |  ❌    |  ✅(stock)  |   ❌
 *  RETURN_AND_EXCHANGE  |  ✅    |     ❌      |   ✅

 * Extra rule for REPLACEMENT:
 *   Even if policy allows it, we check LIVE stock.
 *  If out of stock → reject with clear message (do NOT auto-fallback to RETURN).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReturnService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final OrderNotificationService  orderNotificationService;
    private final ReturnPolicyService returnPolicyService;
    private final CloudinaryService cloudinaryService;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOMER: Request return / replacement / exchange
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public OrderResponse requestReturn(Long userId, Long orderId,
                                       ReturnType requestType, ReturnReason returnReason, String returnDescription,
                                       MultipartFile[] images) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find order with orderId: " + orderId));

        // Guard 1: Ownership check
        if(!order.getUser().getId().equals(userId)) {
            throw new BusinessLogicException("Access Denied: This order does not belong to you.");
        }

        // Guard 2: Status check — only DELIVERED orders can be returned
        if(order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BusinessLogicException(
                    "Post-purchase request not allowed. Order must be DELIVERED. " +
                            "Current status: " + order.getOrderStatus());
        }

        // Guard 3: No duplicate request
        if(order.getReturnRequestedAt() != null){
            throw new BusinessLogicException(
                    "A " + order.getReturnRequestType() + " request is already submitted for this order.");
        }

        // Guard 4–7: Policy validation per item
        Map<Long, PolicySnapshot> policyMap = parsePolicySnapshot(orderId, order.getReturnPolicySnapshot());

        // Validate each item in the order
        for(OrderItem item : order.getOrderItems()) {
            PolicySnapshot itemPolicy = resolvePolicy(item.getProduct(), policyMap, orderId);
            validatePolicyForItem(item, itemPolicy, requestType, orderId);
        }

        // Guard 8: Image proof enforcement
        boolean hasImages = images != null && images.length > 0;

        if(returnReason.requiresImageProof() && !hasImages){
            throw new BusinessLogicException(
                    "Image proof is mandatory for '" + returnReason.name() + "' requests. Attach at least 1 image.");
        }

        // Upload images to Cloudinary
        List<String> uploadedImageUrls = new ArrayList<>();
        if(hasImages){
            // Cap at 3 images to prevent abuse
            if(images.length > 3){
                throw new BusinessLogicException("Maximum 3 images allowed for return proof.");

            }
            for(MultipartFile file : images){
                FileValidator.validateImage(file);
                // Uploading to a specific folder: returns/{orderId}
                String url  = cloudinaryService.upload(file, "returns/" + order.getId());
                uploadedImageUrls.add(url);
            }
        }

        // Commit the return request
        OrderStatus newStatus = switch (requestType) {
            case RETURN      -> OrderStatus.RETURN_REQUESTED;
            case REPLACEMENT -> OrderStatus.REPLACEMENT_REQUESTED;
            case EXCHANGE    -> OrderStatus.EXCHANGE_REQUESTED;
        };

        order.setReturnProofImages(uploadedImageUrls);
        order.setOrderStatus(newStatus);
        order.setReturnRequestType(requestType);
        order.setReturnReason(returnReason);
        order.setReturnDescription(returnDescription);
        order.setReturnRequestedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        orderNotificationService.sendStatusUpdateEmail(orderMapper.toOrderResponse(savedOrder));

        log.info("Post-purchase [{}] request submitted — orderId={} userId={}", requestType, orderId, userId);

        return orderMapper.toOrderResponse(savedOrder);
    }


    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────
    /**
     * Parses the frozen JSONB snapshot from the order.
     * Falls back to live policy via ReturnPolicyService if snapshot is missing/corrupt.
     */
    private Map<Long, PolicySnapshot> parsePolicySnapshot(Long orderId, String snapshotJson) {
        if(snapshotJson == null){
            return null;
        }
        try{
            return objectMapper.readValue(snapshotJson, new TypeReference<Map<Long, PolicySnapshot>>(){});

        } catch (Exception e) {
            log.warn("Could not parse return policy snapshot for order {}. " +
                    "Will use live policy fallback per item.", orderId);
            return null;
        }
    }

    private PolicySnapshot resolvePolicy(Product product,
                                         Map<Long, PolicySnapshot> policyMap,
                                         Long orderId) {
        if(policyMap != null && policyMap.containsKey(product.getId())){
            return policyMap.get(product.getId());
        }
        // Live fallback — snapshot was missing or corrupt
        try {
            return returnPolicyService.getPolicySnapshotForCheckout(product);
        } catch (Exception e) {
            log.warn("Live policy fallback failed for product {} on order {}.",
                    product.getId(), orderId);
            return null;
        }
    }

    private void validatePolicyForItem(OrderItem item, PolicySnapshot policy,
                                       ReturnType requestType, Long orderId) {
        Product product = item.getProduct();

        if(policy == null){
            // No snapshot, no live fallback — only RETURN (refund) is permitted by safe default
            if (requestType != ReturnType.RETURN) {
                throw new BusinessLogicException(
                        "No return policy found for product '" + product.getProductName() +
                                "'. Only RETURN (refund) is permitted by default.");
            }
            return;
        }

        // Guard 5: Return window deadline
        if(item.getOrder().getDeliveredAt() != null && policy.getReturnWindowDays() > 0){
            LocalDateTime deadline = item.getOrder().getDeliveredAt().plusDays(policy.getReturnWindowDays());
            if(LocalDateTime.now().isAfter(deadline)){
                throw new BusinessLogicException(
                        "Return window expired for product '" + product.getProductName() +
                                "'. You had " + policy.getReturnWindowDays() +
                                " days from delivery. Deadline was: " + deadline);
            }
        }

        // Guard 6: Hard stop for NON_RETURNABLE
        if(policy.getPolicyType()  == PolicyType.NON_RETURNABLE){
            throw new BusinessLogicException(
                    "Product '" + product.getProductName() +
                            "' is not eligible for any post-purchase action " +
                            "(non-returnable as per seller's policy at time of purchase).");
        }

        // Guard 7: Request type vs policy allowances
        switch (requestType){
            case RETURN -> {
                if (!policy.isReturnAllowed()) {
                    throw new BusinessLogicException(
                            "Return (refund) is not available for product '" +
                                    product.getProductName() + "'." + buildHint(policy));
                }
            }

            case EXCHANGE -> {
                if (!policy.isExchangeAllowed()) {
                    throw new BusinessLogicException(
                            "Exchange is not available for product '" +
                                    product.getProductName() + "'." + buildHint(policy));
                }
            }

            case REPLACEMENT -> {
                if(!policy.isReplacementAllowed()){
                    throw new BusinessLogicException(
                            "Replacement is not available for product '" +
                                    product.getProductName() + "'." + buildHint(policy));
                }

                // Real-time stock check at request time
                Product fresh = productRepository.findById(product.getId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Product no longer exists: " + product.getProductName()));

                if(fresh.getStockQuantity() < item.getQuantity()){
                    String hint =  policy.isReturnAllowed()
                            ? " You may request a RETURN (refund) instead." : "";

                    throw new BusinessLogicException(
                            "Replacement unavailable for '" + product.getProductName() +
                                    "' — insufficient stock (requested: " + item.getQuantity() +
                                    ", available: " + fresh.getStockQuantity() + ")." + hint);
                }
            }
        }
    }


    private String buildHint(PolicySnapshot policy) {
        List<String> options = new ArrayList<>();
        if (policy.isReturnAllowed())      options.add("RETURN (refund)");
        if (policy.isReplacementAllowed()) options.add("REPLACEMENT");
        if (policy.isExchangeAllowed())    options.add("EXCHANGE");
        return options.isEmpty()
                ? " No post-purchase options available for this item."
                : " Available: " + String.join(", ", options) + ".";
    }

}
