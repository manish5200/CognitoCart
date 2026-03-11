package com.manish.smartcart.service;

import com.manish.smartcart.dto.order.OrderRequest;
import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.mapper.OrderMapper;
import com.manish.smartcart.model.cart.Cart;
import com.manish.smartcart.model.cart.CartItem;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.model.order.OrderItem;
import com.manish.smartcart.model.order.UserCouponUsage;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.repository.UserCouponUsageRepository;
import com.manish.smartcart.service.notifications.OrderNotificationService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Transactional
    public OrderResponse placeOrder(Long userId, OrderRequest orderRequest) {
        // 1. Get the user's cart
        Cart cart = cartService.getCartForUser(userId);
        if (cart == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot place order with an empty cart");
        }
        // 2. Create the Order "Header"
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PAYMENT_PENDING);

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
                throw new RuntimeException("Please provide a shipping address or save one in your profile.");
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
            Product product = cartItem.getProduct();

            // CRITICAL: Re-Check stock before confirming
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for: " + product.getProductName());
            }
            // Deduct stock
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

        // 4. Handle Coupons and their Usage Limits
        if (cart.getCouponCode() != null) {
            order.setCouponCode(cart.getCouponCode());
            order.setDiscountAmount(cart.getDiscountAmount());

            // Deduct from overall total
            computedTotal = computedTotal.subtract(cart.getDiscountAmount());

            // --- Increment the global usages of the coupon
            couponService.incrementUsage(cart.getCouponCode());

            // --- Track Per-User Usage Limit ---
            // Fetch the Coupon entity - validation was already done when coupon was applied
            // to cart
            com.manish.smartcart.model.order.Coupon coupon = couponService.getCouponByCode(cart.getCouponCode());

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

        // --- PHASE 3: PAYMENT GATEWAY (RAZORPAY) ---
        // 6. Connect to Razorpay to initialize the remote transaction
        String razorpayOrderId = paymentService.createRazorpayOrder(savedOrder);
        savedOrder.setRazorpayOrderId(razorpayOrderId);
        savedOrder = orderRepository.save(savedOrder);

        cartService.clearTheCart(userId);

        OrderResponse orderResponse = orderMapper.toOrderResponse(savedOrder);
        // Explicitly set the Razorpay Order ID for the frontend to use in checkout
        // overlay
        orderResponse.setRazorpayOrderId(razorpayOrderId);

        // Note: The Order Confirmation Email has been MOVED to PaymentController
        // because we only want to email the user AFTER the webhooks/callbacks verify
        // the payment.

        log.info("Order processed as PENDING for local orderId {} and razorpayId {}", orderResponse.getOrderId(),
                razorpayOrderId);
        return orderResponse;
    }

    // Order History feature.
    public List<OrderResponse> getOrderHistoryForUser(Long userId) {
        List<Order> orders = orderRepository.findByUserIdAndOrderItems(userId);
        // Map each Entity to a DTO to prevent infinite loops and hide internal DB
        // details
        return orders.stream()
                .map(orderMapper::toOrderResponse)
                .collect(Collectors.toList());
    }

    // Cancellation of the order if the order is still pending -> stock will be back
    // to product
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        // 1. Find the order and verify ownership
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Cannot find order with orderId: " + orderId));

        // It does not make sense but still but if by chance, lets B get oderId of A
        // So to prevent B from cancelling A's order
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access Denied: You do not own this order.");
        }

        // 2. Security Check: DELIVERED orders cannot be canceled
        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Order cannot be cancelled❌❌. Current status: " + order.getOrderStatus());
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
        // 4. Update Status
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
}