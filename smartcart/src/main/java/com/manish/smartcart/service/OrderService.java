package com.manish.smartcart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.repository.UserCouponUsageRepository;
import com.manish.smartcart.repository.UsersRepository;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.exception.InsufficientStockException;
import com.manish.smartcart.exception.ResourceNotFoundException;
import com.manish.smartcart.service.notifications.OrderNotificationService;
import com.manish.smartcart.util.FileValidator;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
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
    private final CloudinaryService cloudinaryService;


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
            // RACE CONDITION FIX: Fetch product with SELECT FOR UPDATE (PESSIMISTIC_WRITE)
            // This DB-level row lock ensures that if two customers checkout simultaneously,
            // the second request WAITS at this line until the first transaction commits.
            // Without this, both could read stockQuantity = 1 and both would pass the
            // stock check — resulting in stock going to -1 (oversell).
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: " + cartItem.getProduct().getId()));

            // CRITICAL: Re-check stock on the freshly locked row
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for: " + product.getProductName());
            }
            // Deduct stock — safe because we hold the row lock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);


            // Create the snapshot record
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order); // Link back to parent
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(cartItem.getPriceAtAdding()); // Freeze the price!
            orderItems.add(orderItem);

            BigDecimal subtotal = cartItem.getPriceAtAdding().multiply(new BigDecimal(cartItem.getQuantity()));
            computedTotal = computedTotal.add(subtotal);
        }
        order.setOrderItems(orderItems);

        // ─── SNAPSHOT THE RETURN POLICY AT CHECKOUT TIME ─────────────────────
        // This freezes the return policy at the moment the customer pays.
        //Even if the seller updates their policy tomorrow, this order is protected.
        // Same principle as priceAtPurchase — immutable audit trail.
        if(!orderItems.isEmpty()){
            Map<Long, PolicySnapshot> policyMap = new HashMap<>();
            for(OrderItem item : orderItems){
                Product product = item.getProduct();
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

    // Order History feature.
    public Page<OrderResponse> getOrderHistoryForUser(Long userId, Pageable pageable) {

        // Step 1: Get paginated order IDs from the database (no join, perfect pagination)
        Page<Long> orderIdPage = orderRepository.findOrderIdsByUserId(userId, pageable);

        if (orderIdPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // Step 2: Fetch complete order data (with items) only for this page's IDs
        List<Order> orders = orderRepository.findOrdersWithItemsByIds(orderIdPage.getContent());

        // Map to response DTOs
        List<OrderResponse> orderResponses = orders.stream()
                .map(orderMapper::toOrderResponse)
                .toList();

        // This ensures correct pagination metadata (totalElements, totalPages) in the response
        return new PageImpl<>(
                orderResponses,
                pageable,
                orderIdPage.getTotalElements()
        );
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
            Product product = orderItem.getProduct();

            // Add the quantity back to the warehouse
            int updatedStock = product.getStockQuantity() + orderItem.getQuantity();
            product.setStockQuantity(updatedStock);
            // saveAndFlush pushes the update to the DB immediately
            productRepository.saveAndFlush(product);
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
            Product product = orderItem.getProduct();
            int updatedStock = product.getStockQuantity() + orderItem.getQuantity();
            product.setStockQuantity(updatedStock);
            // saveAndFlush pushes the update to the DB immediately
            productRepository.saveAndFlush(product);
        }
        // 2. Update order status
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);// Or OrderStatus.EXPIRED
    }

    // ─────────────────────────────────────────────────────────────────────────
// CUSTOMER: Request return / replacement / exchange
// ─────────────────────────────────────────────────────────────────────────
    /**
     * Full policy matrix:

     *  PolicyType           | RETURN req | REPLACEMENT req | EXCHANGE req
     *  ---------------------|------------|-----------------|-------------
     *  NON_RETURNABLE       | ❌         | ❌              | ❌
     *  RETURN_ONLY          | ✅         | ❌              | ❌
     *  EXCHANGE_ONLY        | ❌         | ❌              | ✅
     *  REPLACEMENT_ONLY     | ❌         | ✅ (if in stock)| ❌
     *  RETURN_AND_EXCHANGE  | ✅         | ❌              | ✅

     * Extra rule for REPLACEMENT:
     *   Even if policy allows it, we check LIVE stock.
     *   If out of stock → reject with clear message (do NOT auto-fallback to RETURN).
     */
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

        // Guard 3: Duplicate return check
        if(order.getReturnRequestedAt() != null){
            throw new BusinessLogicException(
                    "A " + order.getReturnRequestType() + " request is already submitted for this order.");
        }

        // Guard 4: Validate against the frozen policy snapshots
        Map<Long, PolicySnapshot>policyMap = null;
        PolicySnapshot legacySnapshot = null;
        if(order.getReturnPolicySnapshot() != null){
            try{
                 policyMap = objectMapper.readValue(order.getReturnPolicySnapshot(),
                         new TypeReference<Map<Long, PolicySnapshot>>(){});
            }catch (Exception e){
                try{
                    legacySnapshot = objectMapper.readValue(order.getReturnPolicySnapshot(), PolicySnapshot.class);
                }catch (Exception e2){
                    log.warn("Could not parse return policy snapshot for order {}. Proceeding with live policy fallback.", orderId, e2);
                }
            }
        }

        // Validate each item in the order
        for(OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            PolicySnapshot itemPolicy = null;
            if (policyMap != null) {
                itemPolicy = policyMap.get(product.getId());
            } else if (legacySnapshot != null) {
                itemPolicy = legacySnapshot;
            }

            // Live fallback if snapshot is missing
            if (itemPolicy == null) {
                try {
                    itemPolicy = returnPolicyService.getPolicySnapshotForCheckout(product);
                } catch (Exception e) {
                    log.warn("Could not resolve live fallback return policy for product {}. Using default RETURN only.",
                            product.getId());
                }
            }

            if (itemPolicy != null) {
                // Guard 5: Return window deadline
                if (order.getDeliveredAt() != null && itemPolicy.getReturnWindowDays() > 0) {
                    LocalDateTime deadline = order.getDeliveredAt().plusDays(itemPolicy.getReturnWindowDays());
                    if (LocalDateTime.now().isAfter(deadline)) {
                        throw new BusinessLogicException(
                                "Return window expired for product '" + product.getProductName()
                                        + "'. You had " + itemPolicy.getReturnWindowDays()
                                        + " days from delivery. Deadline was: " + deadline);
                    }
                }

                // Guard 6: Non - Returnable hard stop
                if (itemPolicy.getPolicyType() == PolicyType.NON_RETURNABLE) {
                    throw new BusinessLogicException(
                            "Product '" + product.getProductName() + "' is not eligible for any post-purchase action " +
                                    "(non-returnable as per seller's policy at time of purchase).");
                }

                // Guard 7: Verify request type matches policy allowances
                switch (requestType) {
                    case RETURN -> {
                        if (!itemPolicy.isReturnAllowed()) {
                            throw new BusinessLogicException(
                                    "Return (refund) is not available for product '" + product.getProductName() + "'." +
                                            buildAvailableOptionsHint(itemPolicy));
                        }
                    }

                    case REPLACEMENT -> {
                        if (!itemPolicy.isReplacementAllowed()) {
                            throw new BusinessLogicException(
                                    "Replacement is not available for product '" + product.getProductName() + "'." +
                                            buildAvailableOptionsHint(itemPolicy));
                        }
                        Product freshProduct = productRepository.findById(product.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Product no longer exists: " + product.getProductName()));

                        if (freshProduct.getStockQuantity() < item.getQuantity()) {
                            String returnHint = itemPolicy.isReturnAllowed() ? " You may request a RETURN (refund) instead." : "";
                            throw new BusinessLogicException(
                                    "Replacement temporarily unavailable for product '" + product.getProductName() +
                                            "' — insufficient stock (Requested: " + item.getQuantity() +
                                            ", Available: " + freshProduct.getStockQuantity() + ")." + returnHint);
                        }
                    }

                    case EXCHANGE -> {
                        if (!itemPolicy.isExchangeAllowed()) {
                            throw new BusinessLogicException(
                                    "Exchange is not available for product '" + product.getProductName() + "'." +
                                            buildAvailableOptionsHint(itemPolicy));
                        }
                    }
                }
            } else {
                // No policy snapshot and live fallback failed
                if (requestType != ReturnType.RETURN) {
                    throw new BusinessLogicException(
                            "No return policy found for product '" + product.getProductName() + "'. Only RETURN (refund) is permitted by default.");
                }
            }
        }

        // Commit Request details
        OrderStatus newStatus =  switch (requestType){
            case RETURN -> OrderStatus.RETURN_REQUESTED;
            case REPLACEMENT -> OrderStatus.REPLACEMENT_REQUESTED;
            case EXCHANGE -> OrderStatus.EXCHANGE_REQUESTED;
        };
        // --- NEW LOGIC: Enforce Media Proof ---
        boolean hasImages = (images != null && images.length > 0);

        if(returnReason.requiresImageProof() && !hasImages){
            throw new BusinessLogicException(
                    "Image proof is mandatory for '" + returnReason.name() + "' requests. Attach at least 1 image.");
        }

        // Upload to Cloudinary ---
        List<String>uploadedImageUrls = new ArrayList<>();
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

        order.setReturnProofImages(uploadedImageUrls);
        order.setOrderStatus(newStatus);
        order.setReturnRequestType(requestType);
        order.setReturnReason(returnReason);
        order.setReturnDescription(returnDescription);
        order.setReturnRequestedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        orderNotificationService.sendStatusUpdateEmail(orderMapper.toOrderResponse(savedOrder));
        log.info("Post-purchase [{}] request for Order ID: {} by User ID: {}", requestType, orderId, userId);
        return orderMapper.toOrderResponse(savedOrder);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN: Approve RETURN → refund money
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public OrderResponse approveReturn(Long orderId){

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getOrderStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new BusinessLogicException(
                    "Order is not in RETURN_REQUESTED state. Current: " + order.getOrderStatus());
        }

        // Restore stock
        for (OrderItem item : order.getOrderItems()) {
            Product p = item.getProduct();
            p.setStockQuantity(p.getStockQuantity() + item.getQuantity());
            productRepository.save(p);
        }

        order.setOrderStatus(OrderStatus.RETURNED);

        // Refund if paid
        if(order.getPaymentStatus() == PaymentStatus.PAID && order.getRazorpayPaymentId() != null){
            try{
                String refundId = razorpayRefundService.initiateFullRefund(
                        order.getRazorpayPaymentId(), order.getTotalAmount());
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                order.setOrderStatus(OrderStatus.REFUNDED);
                Order saved = orderRepository.save(order);
                orderNotificationService.sendRefundEmail(orderMapper.toOrderResponse(saved), refundId);
                return orderMapper.toOrderResponse(saved);

            }catch (Exception e){
                orderRepository.save(order); // Save RETURNED even if refund API fails
                throw new BusinessLogicException(
                        "Stock restored, marked RETURNED. Razorpay refund failed: " + e.getMessage() +
                                " — process manually via Razorpay dashboard.");
            }
        }
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN: Approve REPLACEMENT or EXCHANGE → re-check stock → re-ship
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public OrderResponse approveReplacement(Long orderId){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if(order.getOrderStatus() != OrderStatus.REPLACEMENT_REQUESTED &&
        order.getOrderStatus() != OrderStatus.EXCHANGE_REQUESTED) {
            throw new BusinessLogicException(
                    "Order must be in REPLACEMENT_REQUESTED or EXCHANGE_REQUESTED state. " +
                            "Current: " + order.getOrderStatus());
        }

        // Re-check stock at approval time (may have dropped since request)
        for(OrderItem item : order.getOrderItems()) {

            Product freshProduct = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product no longer exists."));

            if(freshProduct.getStockQuantity() < item.getQuantity()) {
                throw new BusinessLogicException(
                        "Cannot approve replacement — insufficient stock for: " +
                                freshProduct.getProductName() + ". Available: " + freshProduct.getStockQuantity() +
                                ". Consider approving a RETURN (refund) instead.");
            }

            freshProduct.setStockQuantity(freshProduct.getStockQuantity() - item.getQuantity());
            productRepository.save(freshProduct);
        }
        order.setOrderStatus(OrderStatus.REPLACEMENT_SHIPPED);
        Order saved = orderRepository.save(order);

        // Admin then attaches the new shipment tracking via
        // existing POST /api/v1/admin/{orderId}/shipment endpoint
        orderNotificationService.sendStatusUpdateEmail(orderMapper.toOrderResponse(saved));
        log.info("Replacement approved and stock decremented for Order ID: {}", orderId);
        return orderMapper.toOrderResponse(saved);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN: Reject RETURN/REPLACEMENT/EXCHANGE
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public OrderResponse rejectReturn(Long orderId, String adminComment){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if(order.getOrderStatus() != OrderStatus.RETURN_REQUESTED &&
        order.getOrderStatus() != OrderStatus.EXCHANGE_REQUESTED &&
        order.getOrderStatus() != OrderStatus.REPLACEMENT_REQUESTED) {
            throw new BusinessLogicException(
                    "Order is not in a return-requested state. Current: " + order.getOrderStatus());
        }

        // Build and dispatch email before resetting fields, so order details map correctly
        OrderResponse responseBeforeReset = orderMapper.toOrderResponse(order);
        orderNotificationService.sendReturnRejectedEmail(responseBeforeReset, adminComment);

        // Reset order state back to delivered state, freeing them to submit a new request if needed
        order.setOrderStatus(OrderStatus.DELIVERED);
        order.setReturnRequestType(null);
        order.setReturnReason(null);
        order.setReturnDescription(null);
        order.setReturnRequestedAt(null);

        Order saved = orderRepository.save(order);
        log.info("Return request for Order ID: {} has been rejected by admin. Reason: {}", orderId, adminComment);
        return orderMapper.toOrderResponse(saved);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN: Get Pending Return/Replacement/Exchange requests
    // ─────────────────────────────────────────────────────────────────────────
    public List<OrderResponse>getPendingReturnRequests(){
        List<OrderStatus>pendingStatuses = List.of(
                OrderStatus.RETURN_REQUESTED,
                OrderStatus.REPLACEMENT_REQUESTED,
                OrderStatus.EXCHANGE_REQUESTED
        );

        return orderRepository.findByOrderStatusInWithItems(pendingStatuses).stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    private String buildAvailableOptionsHint(PolicySnapshot policy) {
        List<String> options = new ArrayList<>();
        if (policy.isReturnAllowed())      options.add("RETURN (refund)");
        if (policy.isReplacementAllowed()) options.add("REPLACEMENT");
        if (policy.isExchangeAllowed())    options.add("EXCHANGE");
        return options.isEmpty()
                ? " No post-purchase options available for this item."
                : " Available option(s): " + String.join(", ", options) + ".";
    }


}