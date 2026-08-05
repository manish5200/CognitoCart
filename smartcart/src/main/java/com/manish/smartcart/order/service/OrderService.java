package com.manish.smartcart.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manish.smartcart.cart.service.CartService;
import com.manish.smartcart.order.coupon.service.CouponService;
import com.manish.smartcart.order.dto.OrderRequest;
import com.manish.smartcart.order.dto.OrderResponse;
import com.manish.smartcart.order.dto.PolicySnapshot;
import com.manish.smartcart.infrastructure.returnpolicy.ReturnPolicyService;
import com.manish.smartcart.order.repository.OrderRepository;
import com.manish.smartcart.order.repository.UserCouponUsageRepository;
import com.manish.smartcart.payment.service.PaymentService;
import com.manish.smartcart.payment.service.RazorpayRefundService;
import com.manish.smartcart.product.repository.ProductVariantRepository;
import com.manish.smartcart.sale.repository.FlashSaleItemRepository;
import com.manish.smartcart.shared.mapper.OrderMapper;
import com.manish.smartcart.cart.model.Cart;
import com.manish.smartcart.cart.model.CartItem;
import com.manish.smartcart.order.coupon.model.Coupon;
import com.manish.smartcart.order.model.Order;
import com.manish.smartcart.order.model.OrderItem;
import com.manish.smartcart.order.model.UserCouponUsage;
import com.manish.smartcart.sale.model.FlashSaleItem;
import com.manish.smartcart.product.model.Product;
import com.manish.smartcart.product.model.ProductVariant;
import com.manish.smartcart.user.model.Users;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.shared.exception.InsufficientStockException;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import com.manish.smartcart.notification.service.OrderNotificationService;
import com.manish.smartcart.shared.enums.OrderStatus;
import com.manish.smartcart.shared.enums.PaymentStatus;
import com.manish.smartcart.user.repository.UsersRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Core Order Orchestration Engine.
 * <p>
 * ARCHITECTURE OVERVIEW:
 * This service implements the Orchestration-based Saga Pattern to manage distributed
 * transactions between our internal database and the external Razorpay payment gateway.
 * <p>
 * We intentionally strictly forbid method-level @Transactional annotations here.
 * Instead, we use Spring's programmatic TransactionTemplate. This protects our
 * HikariCP connection pool by ensuring long-running network I/O (HTTP calls to Razorpay)
 * are executed completely outside of database transaction boundaries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartService cartService;
    private final OrderMapper orderMapper;
    private final OrderNotificationService orderNotificationService;
    private final CouponService couponService;
    private final PaymentService paymentService;
    private final UserCouponUsageRepository userCouponUsageRepository;
    private final UsersRepository usersRepository;
    private final RazorpayRefundService razorpayRefundService;
    private final MeterRegistry meterRegistry;
    private final ReturnPolicyService returnPolicyService;
    private final ObjectMapper objectMapper;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final TransactionTemplate transactionTemplate;

    // ─────────────────────────────────────────────────────────────────────────────
    // CORE CHECKOUT FLOW (Saga Pattern)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Executes the main checkout pipeline.
     * <p>
     * SAGA PHASES:
     * 1. Local TX 1: Validate, apply pessimistic locks, deduct stock, and persist the order.
     * 2. Network I/O: Request a Payment ID from Razorpay (ZERO database connections held).
     * 3. Local TX 2: Attach the Razorpay ID and flush the user's cart.
     * <p>
     * If Phase 2 fails, a Compensating Transaction (cancelAndReleaseStock) is executed to
     * rollback Phase 1, maintaining eventual consistency.
     */
    public OrderResponse placeOrder(Long userId, OrderRequest orderRequest) {

        // PHASE 1: Fast Database Transaction (~50ms)
        // Connection is acquired, locks are held, data is flushed, and the connection is RELEASED.
        Order savedOrder = transactionTemplate.execute(status -> buildAndPersistOrder(userId, orderRequest));
        Objects.requireNonNull(savedOrder, "TX1 failed silently — order not persisted. Check DB logs.");
        String razorpayOrderId;

        // PHASE 2: Network Execution (3000ms - 5000ms)
        // High-latency HTTP call. Because TX 1 is closed, our Hikari pool remains fully available
        // for other concurrent users. This is the key to massive horizontal scale.
        try {
            razorpayOrderId = paymentService.createRazorpayOrder(savedOrder);
        } catch (Exception e) {
            log.error("Razorpay order creation FAILED for Order ID: {}. Executing compensating rollback. Error: {}",
                    savedOrder.getId(), e.getMessage(), e);

            // COMPENSATING TRANSACTION: Rollback stock and coupons.
            // We pass the ID instead of the object to prevent Detached Entity / LazyInitialization traps.
            cancelAndReleaseStock(savedOrder.getId());

            throw new BusinessLogicException(
                    "Payment gateway is currently unavailable. Your cart has been restored. Please try again.");
        }

        // RECOVERY CHECKPOINT: If the node crashes between Phase 2 and Phase 3, this log
        // acts as an audit trail for manual Database/Gateway reconciliation.
        log.info("CHECKPOINT: orderId={} razorpayOrderId={} — persisting to DB now.", savedOrder.getId(), razorpayOrderId);

        // PHASE 3: Fast Database Transaction (~20ms)
        // Re-fetch the order to ensure we operate on a fresh L1 cache entity.
        Order finalOrder = transactionTemplate.execute(status -> {
            Order freshOrder = orderRepository.findById(savedOrder.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order desync after save: " + savedOrder.getId()));
            freshOrder.setRazorpayOrderId(razorpayOrderId);
            Order result = orderRepository.save(freshOrder);

            cartService.clearTheCart(userId);
            return result;
        });

        // OBSERVABILITY: Publish business metrics for Grafana/Prometheus alerting.
        meterRegistry.counter("cognitocart.orders.placed").increment();

        Objects.requireNonNull(finalOrder, "TX2 failed silently: Razorpay ID not attached. RazorpayOrderId=" + razorpayOrderId);
        OrderResponse orderResponse = orderMapper.toOrderResponse(finalOrder);
        orderResponse.setRazorpayOrderId(razorpayOrderId);

        log.info("Order processed as PENDING for local orderId {} and razorpayId {}", orderResponse.getOrderPublicId(), razorpayOrderId);
        return orderResponse;
    }

    /**
     * Heavy-lifting helper for Phase 1 of checkout.
     * Guaranteed to execute entirely within the bounds of a single TransactionTemplate context.
     */
    private Order buildAndPersistOrder(Long userId, OrderRequest orderRequest) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // SECURITY GUARD: Enforce email verification to mitigate bot/fraud checkout attempts.
        if (!user.isEmailVerified()) {
            throw new BusinessLogicException("Please verify your email before placing an order.");
        }

        Cart cart = cartService.getCartForUser(userId);
        if (cart == null || cart.getItems().isEmpty()) {
            throw new BusinessLogicException("Cannot place order with an empty cart");
        }

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PAYMENT_PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setDeliveryFee(cart.getDeliveryFee());

        applyShippingAddress(order, orderRequest, cart.getUser());

        BigDecimal computedTotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // SORTING: Sort by Variant ID before locking to prevent DB Deadlocks when two users
        // try to check out with the same items in different orders.
        List<CartItem> sortedCartItems = new ArrayList<>(cart.getItems());
        sortedCartItems.sort(Comparator.comparing(item -> item.getVariant().getId()));

        for (CartItem cartItem : sortedCartItems) {

            // CONCURRENCY GUARD: Pessimistic Row-Level Lock.
            // Prevents overselling during high-traffic flash sales by forcing concurrent
            // checkouts for the same SKU to queue at the database level.
            ProductVariant variant = productVariantRepository.findByIdForUpdate(cartItem.getVariant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant discontinued: SKU " + cartItem.getVariant().getSku()));

            int availableStock = variant.getAvailableStock();
            if (availableStock < cartItem.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for: " + variant.getDisplayLabel() + ". Available: " + availableStock);
            }

            // PRICING ENGINE: Calculate real-time expected price.
            Optional<FlashSaleItem> activeFlashSale = flashSaleItemRepository.findActiveDiscountForVariant(variant.getId());
            BigDecimal currentExpectedPrice = variant.getProduct().getPrice().add(variant.getPriceModifier() != null ? variant.getPriceModifier() : BigDecimal.ZERO);
            boolean isFlashSaleItem = false;

            if (activeFlashSale.isPresent()) {
                FlashSaleItem sale = activeFlashSale.get();
                BigDecimal discountMultiplier = BigDecimal.ONE.subtract(sale.getDiscountPercentage().divide(BigDecimal.valueOf(100), 10 , RoundingMode.HALF_UP));
                currentExpectedPrice = currentExpectedPrice.multiply(discountMultiplier);
                isFlashSaleItem = true;
            }

            // ABANDONED CART EXPLOIT GUARD: Verify the price hasn't shifted since the item was added.
            if (cartItem.getPriceAtAdding().subtract(currentExpectedPrice).abs().compareTo(BigDecimal.valueOf(0.05)) > 0) {
                throw new BusinessLogicException("Price changed during checkout for " + variant.getDisplayLabel() + ". Please refresh your cart.");
            }

            if (isFlashSaleItem) {
                // BYPASS L1 CACHE: Use native @Modifying query for atomic increment.
                int rowsUpdated = flashSaleItemRepository.atomicallyIncrementUsedUnits(activeFlashSale.get().getId(), cartItem.getQuantity());
                if (rowsUpdated == 0) {
                    throw new BusinessLogicException("Sorry, the Flash Sale deal for " + variant.getDisplayLabel() + " just sold out! Please refresh your cart.");
                }
            }

            // Deduct primary inventory. Safe due to our Pessimistic Write Lock.
            variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
            productVariantRepository.save(variant);

            Product product = variant.getProduct();
            BigDecimal lineTotal = cartItem.getPriceAtAdding().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            // IMMUTABLE SNAPSHOTTING: Freeze product metadata (Names, SKUs, Images).
            // If the seller alters the catalog tomorrow, this order receipt remains historically accurate.
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setVariant(variant);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(cartItem.getPriceAtAdding());
            orderItem.setLineTotal(lineTotal);
            orderItem.setProductNameSnapshot(product.getProductName());
            orderItem.setVariantLabelSnapshot(variant.getDisplayLabel());
            orderItem.setSkuSnapshot(variant.getSku());
            orderItem.setImageUrlSnapshot(
                    variant.getVariantImageUrl() != null ? variant.getVariantImageUrl()
                            : (product.getImageUrls() != null && !product.getImageUrls().isEmpty() ? product.getImageUrls().get(0) : null));

            orderItems.add(orderItem);
            computedTotal = computedTotal.add(lineTotal);
        }
        order.setOrderItems(orderItems);

        // AUDIT TRAIL: Freeze the Return Policy JSON at the exact moment of checkout.
        snapshotReturnPolicies(order, orderItems);

        // PROMOTIONAL ENGINE
        if (cart.getCouponCode() != null) {
            order.setCouponCode(cart.getCouponCode());
            order.setDiscountAmount(cart.getDiscountAmount());
            computedTotal = computedTotal.subtract(cart.getDiscountAmount());

            couponService.incrementUsage(cart.getCouponCode());
            Coupon coupon = couponService.getCouponByCode(cart.getCouponCode());

            UserCouponUsage usage = userCouponUsageRepository.findByUserIdAndCouponId(userId, coupon.getId())
                    .orElse(UserCouponUsage.builder().user(cart.getUser()).coupon(coupon).usage(0).build());
            usage.setUsage(usage.getUsage() + 1);
            userCouponUsageRepository.save(usage);
        }

        if (order.getDeliveryFee() != null) {
            computedTotal = computedTotal.add(order.getDeliveryFee());
        }

        // FLOOR LIMIT: Prevent negative totals (edge case with massive flat-rate coupons).
        order.setTotalAmount(computedTotal.max(BigDecimal.ZERO));
        return orderRepository.save(order);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // CANCELLATION FLOW (Network-First Execution Paradigm)
    // ─────────────────────────────────────────────────────────────────────────────

    public OrderResponse cancelOrder(Long userId, Long orderId) {
        final String[] capturedPaymentId = {null};
        final BigDecimal[] capturedTotal = {null};

        // PHASE 1: Validation & Status Check
        Order cancelledOrder = transactionTemplate.execute(status -> {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

            // SECURITY: Prevent IDOR (Insecure Direct Object Reference).
            // We return 404 instead of 403 to prevent attackers from enumerating valid Order IDs.
            if (!order.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Order not found: " + orderId);
            }

            Set<OrderStatus> nonCancellableStatuses = Set.of(
                    OrderStatus.DELIVERED, OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY,
                    OrderStatus.RETURNED, OrderStatus.REFUNDED, OrderStatus.CANCELLED);

            if (nonCancellableStatuses.contains(order.getOrderStatus())) {
                throw new BusinessLogicException("Order cannot be cancelled. Current status: " + order.getOrderStatus());
            }

            restoreStockForOrder(order);

            // Capture immutable data needed for the external gateway before closing the TX.
            if(order.getPaymentStatus() == PaymentStatus.PAID && order.getRazorpayPaymentId() != null){
                capturedPaymentId[0] = order.getRazorpayPaymentId();
                capturedTotal[0] = order.getTotalAmount();
            }

            reverseCouponUsage(order);

            order.setOrderStatus(OrderStatus.CANCELLED);
            return orderRepository.save(order);
        });

        // PHASE 2: Gateway Integration (Zero DB Locks)
        if (capturedPaymentId[0] != null) {
            try {
                // If this network call succeeds, the money is moving back to the user.
                String refundId = razorpayRefundService.initiateFullRefund(capturedPaymentId[0], capturedTotal[0]);
                log.info("Razorpay refund initiated for Order ID {}, refundId: {}", orderId, refundId);

                // PHASE 3: Acknowledge Success
                transactionTemplate.execute(status -> {
                    Order fresh = orderRepository.findById(orderId).orElseThrow();
                    fresh.setPaymentStatus(PaymentStatus.REFUNDED);
                    Order saved = orderRepository.save(fresh);
                    orderNotificationService.sendRefundEmail(orderMapper.toOrderResponse(saved), refundId);
                    return saved;
                });

            } catch (Exception e) {
                // PARTIAL SAGA FAILURE STATE:
                // The order is canceled and stock is restored, but the Gateway refused the refund
                // (or timed out). We gracefully degrade to a manual ops state instead of crashing.
                log.error("Razorpay refund FAILED for Order ID: {}. Flagging MANUAL_REFUND_REQUIRED. Error: {}", orderId, e.getMessage(), e);

                transactionTemplate.execute(status -> {
                    Order fresh = orderRepository.findById(orderId).orElseThrow();
                    fresh.setOrderStatus(OrderStatus.MANUAL_REFUND_REQUIRED);
                    return orderRepository.save(fresh);
                });
            }
        }

        Objects.requireNonNull(cancelledOrder, "TX1 (cancel) failed silently: order not cancelled. orderId=" + orderId);
        return orderMapper.toOrderResponse(cancelledOrder);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SYSTEM RECOVERY (Idempotent Sagas)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Centralized Compensating Transaction.
     * Used by:
     * 1. placeOrder() when the Payment Gateway throws an exception.
     * 2. OrderCleanupScheduler for purging stale, abandoned carts.
     * <p>
     * DESIGN REQUIREMENT: Must take an ID, not an Entity.
     * Passing a detached Entity from an expired Transaction context causes
     * LazyInitializationExceptions when attempting to traverse order.getOrderItems().
     */
    public void cancelAndReleaseStock(Long orderId) {
        transactionTemplate.executeWithoutResult(status -> {
            // Fresh load ensures the entity is safely bound to the current Hibernate Session.
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cannot release stock: order not found: " + orderId));

            // IDEMPOTENCY GUARD: Ensures that retry loops or duplicate Kafka/Cron events
            // do not inflate "ghost inventory" by restoring the same order's stock twice.
            if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.MANUAL_REFUND_REQUIRED) {
                log.warn("cancelAndReleaseStock called on already-terminal order {}. Skipping.", orderId);
                return;
            }

            log.info("Releasing stock for order ID: {}", order.getId());
            restoreStockForOrder(order);
            reverseCouponUsage(order);

            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // INTERNAL UTILITIES
    // ─────────────────────────────────────────────────────────────────────────────

    private void applyShippingAddress(Order order, OrderRequest orderRequest, Users user) {
        if (orderRequest != null && orderRequest.getShippingAddress() != null) {
            var addr = orderRequest.getShippingAddress();
            order.setShippingFullName(addr.getFullName());
            order.setShippingPhone(addr.getPhoneNumber());
            order.setShippingStreetAddress(addr.getStreetAddress());
            order.setShippingCity(addr.getCity());
            order.setShippingState(addr.getState());
            order.setShippingZipCode(addr.getZipCode());
            order.setShippingCountry(addr.getCountry());
        } else {
            var addr = user.getPrimaryAddress();
            if (addr == null) throw new BusinessLogicException("Please provide a shipping address or save one in your profile.");
            order.setShippingFullName(addr.getFullName());
            order.setShippingPhone(addr.getPhoneNumber());
            order.setShippingStreetAddress(addr.getStreetAddress());
            order.setShippingCity(addr.getCity());
            order.setShippingState(addr.getState());
            order.setShippingZipCode(addr.getZipCode());
            order.setShippingCountry(addr.getCountry());
        }
    }

    private void snapshotReturnPolicies(Order order, List<OrderItem> orderItems) {
        if (orderItems.isEmpty()) return;
        Map<Long, PolicySnapshot> policyMap = new HashMap<>();
        for (OrderItem item : orderItems) {
            if (item.getVariant() == null) continue;
            Product product = item.getVariant().getProduct();
            if (!policyMap.containsKey(product.getId())) {
                try {
                    policyMap.put(product.getId(), returnPolicyService.getPolicySnapshotForCheckout(product));
                } catch (Exception e) {
                    log.warn("Could not fetch return policy snapshot for product {}: {}", product.getId(), e.getMessage());
                }
            }
        }
        try {
            order.setReturnPolicySnapshot(objectMapper.writeValueAsString(policyMap));
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize return policy snapshot map: {}", e.getMessage());
        }
    }

    private void restoreStockForOrder(Order order) {
        for (OrderItem orderItem : order.getOrderItems()) {
            if (orderItem.getVariant() == null) {
                log.warn("Skipping stock restore for order {}: variant hard-deleted post-order.", order.getId());
                continue;
            }
            ProductVariant variant = orderItem.getVariant();
            variant.setStockQuantity(variant.getStockQuantity() + orderItem.getQuantity());
            productVariantRepository.saveAndFlush(variant);
        }
    }

    private void reverseCouponUsage(Order order) {
        if (order.getCouponCode() != null) {
            try {
                couponService.decrementUsage(order.getCouponCode());
                Coupon coupon = couponService.getCouponByCode(order.getCouponCode());
                userCouponUsageRepository
                        .findByUserIdAndCouponId(order.getUser().getId(), coupon.getId())
                        .ifPresent(usage -> {
                            usage.setUsage(Math.max(0, usage.getUsage() - 1));
                            userCouponUsageRepository.save(usage);
                        });
            } catch (Exception e) {
                // Non-fatal exception. A failed coupon reversal must never block stock restoration.
                log.error("Could not reverse coupon '{}' for order {}: {}. Manual patch required.",
                        order.getCouponCode(), order.getId(), e.getMessage());
            }
        }
    }
}
