package com.manish.smartcart.service.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manish.smartcart.dto.order.OrderRequest;
import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.dto.order.PolicySnapshot;
import com.manish.smartcart.enums.*;
import com.manish.smartcart.mapper.OrderMapper;
import com.manish.smartcart.model.cart.Cart;
import com.manish.smartcart.model.cart.CartItem;
import com.manish.smartcart.model.order.Coupon;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.model.order.OrderItem;
import com.manish.smartcart.model.order.UserCouponUsage;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.model.product.ProductVariant;
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.*;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.exception.InsufficientStockException;
import com.manish.smartcart.exception.ResourceNotFoundException;
import com.manish.smartcart.service.*;
import com.manish.smartcart.service.notifications.OrderNotificationService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
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


    @Transactional
    public OrderResponse placeOrder(Long userId, OrderRequest orderRequest) {

        // CHECKOUT GUARD: Unverified accounts cannot place orders.
        // This forces email ownership confirmation before any money moves.
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // GUARD: Email must be verified before any order can be placed.
        // CONCEPT: Email verification guard — same pattern used by Amazon, Flipkart.
        // If the user hasn't verified their email, we block checkout entirely.
        // Why here and not in the controller? Because business rules belong in the service layer. 
        if(!user.isEmailVerified()){
            throw new BusinessLogicException(
                    "Please verify your email before placing an order. " +
                            "Check your inbox for the OTP, or use /auth/resend-otp to get a new one."
            );
        }

        // 1. Get the user's cart
        Cart cart = cartService.getCartForUser(userId);
        if (cart == null || cart.getItems().isEmpty()) {
            throw new BusinessLogicException("Cannot place order with an empty cart");
        }
        // 2. Create the Order "Header"
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PAYMENT_PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING); // ← Payment lifecycle starts here

        // --- NEW: Transfer Delivery Fee ---
        order.setDeliveryFee(cart.getDeliveryFee());

        // --- ADDRESS SNAPSHOTTING ---
        // Priority 1: Use the address provided in the checkout request body
        // Priority 2: Fall back to the user's saved primary address
        if (orderRequest != null && orderRequest.getShippingAddress() != null) {
            // Use the DTO address sent in the request (most common checkout flow)
            var shippingAddr = orderRequest.getShippingAddress();
            order.setShippingFullName(shippingAddr.getFullName());
            order.setShippingPhone(shippingAddr.getPhoneNumber());
            order.setShippingStreetAddress(shippingAddr.getStreetAddress());
            order.setShippingCity(shippingAddr.getCity());
            order.setShippingState(shippingAddr.getState());
            order.setShippingZipCode(shippingAddr.getZipCode());
            order.setShippingCountry(shippingAddr.getCountry());
        } else {
            // Fall back to saved primary address
            var shippingAddr = cart.getUser().getPrimaryAddress();
            if (shippingAddr == null) {
                throw new BusinessLogicException("Please provide a shipping address or save one in your profile.");
            }
            order.setShippingFullName(shippingAddr.getFullName());
            order.setShippingPhone(shippingAddr.getPhoneNumber());
            order.setShippingStreetAddress(shippingAddr.getStreetAddress());
            order.setShippingCity(shippingAddr.getCity());
            order.setShippingState(shippingAddr.getState());
            order.setShippingZipCode(shippingAddr.getZipCode());
            order.setShippingCountry(shippingAddr.getCountry());
        }

        // 3. Convert CartItems to OrderItems (The Snapshot)
        BigDecimal computedTotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            // RACE CONDITION FIX: Lock the VARIANT row (stock now lives on variant, not product).
            // Two customers checking out the last unit simultaneously:
            //   → Second transaction BLOCKS here until the first commits.
            //   → Second then reads the already-decremented stock and throws InsufficientStockException.
            ProductVariant variant = productVariantRepository.findByIdForUpdate(cartItem.getVariant().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Variant no longer exists: SKU " + cartItem.getVariant().getSku()));

            // CRITICAL: Re-check available stock on the freshly-locked row.
            // availableStock = stockQuantity - reservedQuantity (units held in other live carts)
            int availableStock = variant.getAvailableStock();
            if (availableStock < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for: " + variant.getDisplayLabel() +
                                " (SKU: " + variant.getSku() + "). Available: " + availableStock);
            }
            // Deduct from gross stock — safe because we hold the PESSIMISTIC_WRITE row lock
            variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
            productVariantRepository.save(variant);

            // Navigate variant → product for name + images + policy
            Product product = variant.getProduct();

            // Build the immutable OrderItem snapshot
            // SNAPSHOT RULE: All string/price fields are copied at checkout.
            // Even if the seller renames the product tomorrow, this order shows
            // exactly what the customer saw and paid for.
            BigDecimal lineTotal = cartItem.getPriceAtAdding()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(order); // Link back to parent
            orderItem.setVariant(variant);   // Live FK (nullable — survives hard deletes)
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(cartItem.getPriceAtAdding());            // Frozen unit price
            orderItem.setLineTotal(lineTotal);                                    // Frozen line total
            orderItem.setProductNameSnapshot(product.getProductName());           // Frozen product name
            orderItem.setVariantLabelSnapshot(variant.getDisplayLabel());         // e.g., "Navy Blue / UK 9"
            orderItem.setSkuSnapshot(variant.getSku());                           // Frozen SKU for disputes

            orderItem.setImageUrlSnapshot(
                    variant.getVariantImageUrl() != null
                    ? variant.getVariantImageUrl()
                            : (product.getImageUrls() != null && !product.getImageUrls().isEmpty()
                            ? product.getImageUrls().get(0)
                            : null));

            orderItems.add(orderItem);
            computedTotal = computedTotal.add(lineTotal);
        }
        order.setOrderItems(orderItems);
        // ─── SNAPSHOT THE RETURN POLICY AT CHECKOUT TIME ─────────────────────
        // This freezes the return policy at the moment the customer pays.
        //Even if the seller updates their policy tomorrow, this order is protected.
        // Same principle as priceAtPurchase — immutable audit trail.
        if(!orderItems.isEmpty()){
            Map<Long, PolicySnapshot> policyMap = new HashMap<>();
            for(OrderItem item : orderItems){
                // Navigate variant → product for the policy chain (product → category → default)
                // Guard: variant may be null if SKU was hard-deleted mid-session (extremely rare)
                if(item.getVariant() == null) continue;
                Product product = item.getVariant().getProduct();
                if(!policyMap.containsKey(product.getId())){
                    try{
                        PolicySnapshot policySnapshot = returnPolicyService.getPolicySnapshotForCheckout(product);
                        policyMap.put(product.getId(), policySnapshot);
                    }catch (Exception e){
                        log.warn("Could not fetch return policy snapshot for product {}: {}", product.getId(), e.getMessage());
                    }
                }
            }
            try{
                order.setReturnPolicySnapshot(objectMapper.writeValueAsString(policyMap));
            } catch (JsonProcessingException e) {
                // Non-fatal: order still proceeds. Return eligibility falls back to live policy.
                log.warn("Could not serialize return policy snapshot map: {}", e.getMessage());
            }
        }

        // 4. Handle Coupons and their Usage Limits
        if (cart.getCouponCode() != null) {
            order.setCouponCode(cart.getCouponCode());
            order.setDiscountAmount(cart.getDiscountAmount());

            // Deduct from overall total
            computedTotal = computedTotal.subtract(cart.getDiscountAmount());

            // --- Increment the global usages of the coupon
            couponService.incrementUsage(cart.getCouponCode());

            // --- Track Per-User Usage Limit ---
            // Fetch the Coupon entity - validation was already done when coupon was applied to cart
            Coupon coupon = couponService.getCouponByCode(cart.getCouponCode());

            // Check if they already have a usage record, otherwise create one
            UserCouponUsage usage = userCouponUsageRepository.findByUserIdAndCouponId(userId, coupon.getId())
                    .orElse(UserCouponUsage.builder().user(cart.getUser()).coupon(coupon).usage(0).build());

            // Increment their personal usage count
            usage.setUsage(usage.getUsage() + 1);
            userCouponUsageRepository.save(usage);
        }

        // --- NEW: Add the Delivery Fee to the Final Total! ---
        if (order.getDeliveryFee() != null) {
            computedTotal = computedTotal.add(order.getDeliveryFee());
        }

        // Fail-safe
        if (computedTotal.compareTo(BigDecimal.ZERO) < 0) {
            computedTotal = BigDecimal.ZERO;
        }

        order.setTotalAmount(computedTotal);
        Order savedOrder = orderRepository.save(order);

        //PAYMENT GATEWAY (RAZORPAY) ---
        //6. Connect to Razorpay to initialize the remote transaction
        String razorpayOrderId = paymentService.createRazorpayOrder(savedOrder);
        savedOrder.setRazorpayOrderId(razorpayOrderId);
        savedOrder = orderRepository.save(savedOrder);

        //Prometheus
        meterRegistry.counter("cognitocart.orders.placed").increment();
        // ↑ Every time an order is placed, this counter ticks up by 1.
        // In Grafana, you can graph: "How many orders per hour?"

        cartService.clearTheCart(userId);

        OrderResponse orderResponse = orderMapper.toOrderResponse(savedOrder);
        // Explicitly set the Razorpay Order ID for the frontend to use in checkout
        // overlay
        orderResponse.setRazorpayOrderId(razorpayOrderId);

        // Note: The Order Confirmation Email has been MOVED to PaymentController
        // because we only want to email the user AFTER the webhooks/callbacks verify the payment.
        log.info("Order processed as PENDING for local orderId {} and razorpayId {}", orderResponse.getOrderId(),
                razorpayOrderId);
        return orderResponse;
    }

    /*
    Cancellation of the order if the order is still pending -> stock will be back
    to product
    */
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        // 1. Find the order and verify ownership
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find order with orderId: " + orderId));

        // It does not make sense but still but if by chance, lets B get oderId of A
        // So to prevent B from cancelling A's order
        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessLogicException("Access Denied: You do not own this order.");
        }

        // Block cancellation for terminal/active fulfillment states
        Set<OrderStatus> nonCancellableStatuses = Set.of(
                OrderStatus.DELIVERED,
                OrderStatus.SHIPPED,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.RETURNED,
                OrderStatus.REFUNDED,
                OrderStatus.CANCELLED
        );
        // 2. Security Check: DELIVERED orders cannot be canceled
        if (nonCancellableStatuses.contains(order.getOrderStatus())) {
            throw new BusinessLogicException(
                    "Order cannot be cancelled. Current status: " + order.getOrderStatus() +
                            ". Only orders that are CREATED, PAYMENT_PENDING, PAID, CONFIRMED, or PACKED can be cancelled.");
        }
        // 3. Inventory Restoration: Return stock to Product table - every product
        // RESTORE STOCK: Loop through items and update products
        for (OrderItem orderItem : order.getOrderItems()) {
            // Guard: variant may be null if it was hard-deleted after the order was placed.
            // In that case, we skip stock restore — the physical unit is already gone.
            if (orderItem.getVariant() == null) {
                log.warn("Skipping stock restore for order item on order {}: variant was deleted.", orderId);
                continue;
            }
            ProductVariant variant = orderItem.getVariant();

            // Add the quantity back to the warehouse
            variant.setStockQuantity(variant.getStockQuantity() + orderItem.getQuantity());
            // saveAndFlush pushes the update to the DB immediately
            productVariantRepository.saveAndFlush(variant);
        }

        // 4. REFUND : Handle Refunds if the order was already PAID
        if(order.getPaymentStatus() == PaymentStatus.PAID &&
        order.getRazorpayPaymentId() != null) {
            try{
                // Call Razorpay API to return the money
                String refundId = razorpayRefundService.initiateFullRefund(order.getRazorpayPaymentId(), order.getTotalAmount());

                // Update payment lifecycle tracking
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                log.info("Refund processed for Order ID {}", orderId);

                // Send Premium Refund Email
                OrderResponse orderResponse = orderMapper.toOrderResponse(order);
                orderNotificationService.sendRefundEmail(orderResponse, refundId);

            } catch (Exception e) {
                // If refund fails, we probably shouldn't cancel the order yet,
                // or we need manual admin intervention. For now, throw the error.
                throw new RuntimeException("Cannot cancel order: " + e.getMessage());
            }
        }
        // 5. Update Status
        order.setOrderStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toOrderResponse(savedOrder);
    }


    // OrderProcessing: Abandoning the stale orders
    @Transactional
    public void cancelAndReleaseStock(Order order) {
        log.info("Releasing stock for abandoned order ID: {}", order.getId());

        // 1. Restore stock to the products
        for (OrderItem orderItem : order.getOrderItems()) {
            if (orderItem.getVariant() == null) {
                log.warn("Skipping stock restore for abandoned order {}: variant was deleted.", order.getId());
                continue;
            }
            ProductVariant variant = orderItem.getVariant();
            variant.setStockQuantity(variant.getStockQuantity() + orderItem.getQuantity());
            // saveAndFlush pushes the update to the DB immediately
            productVariantRepository.saveAndFlush(variant);
        }
        // 2. Update order status
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);// Or OrderStatus.EXPIRED
    }


}