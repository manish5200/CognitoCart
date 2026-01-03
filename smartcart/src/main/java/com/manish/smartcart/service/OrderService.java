package com.manish.smartcart.service;

import com.manish.smartcart.dto.order.OrderRequest;
import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.mapper.OrderMapper;
import com.manish.smartcart.model.cart.Cart;
import com.manish.smartcart.model.cart.CartItem;
import com.manish.smartcart.model.order.Order;
import com.manish.smartcart.model.order.OrderItem;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private CartService cartService;
    private OrderMapper orderMapper;
    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, CartService cartService, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderResponse placeOrder(Long userId, OrderRequest orderRequest) {
        // 1. Get the user's cart
        Cart cart = cartService.getCartForUser(userId);
        if(cart == null || cart.getItems().isEmpty()){
            throw  new RuntimeException("Cannot place order with an empty cart");
        }

        // 2. Create the Order "Header"
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setShippingAddress(orderRequest.getShippingAddress());
        order.setTotal(cart.getTotalAmount());

        // 3. Convert CartItems to OrderItems (The Snapshot)
        List<OrderItem> orderItems = new ArrayList<>();
        for(CartItem cartItem : cart.getItems()){
            Product product = cartItem.getProduct();

            // CRITICAL: Re-Check stock before confirming
            if(product.getStockQuantity() < cartItem.getQuantity()){
                throw new RuntimeException("Insufficient stock for: " + product.getProductName());
            }
            //Deduct stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            // Create the snapshot record
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order); // Link back to parent
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(cartItem.getPrice()); // Freeze the price!
            orderItems.add(orderItem);
        }
        order.setOrderItems(orderItems);

        // 4. Save Order and Clear the Cart
        Order savedOrder = orderRepository.save(order);
        cartService.clearTheCart(userId);

        return orderMapper.toOrderResponse(savedOrder);
    }


    //Order History feature.
    public List<OrderResponse>getOrderHistoryForUser(Long userId) {
        List<Order>orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);
        // Map each Entity to a DTO to prevent infinite loops and hide internal DB details
        return orders.stream()
                .map(order -> orderMapper.toOrderResponse(order))
                .collect(Collectors.toList());
    }

    //Cancellation of the order if the order is still pending -> stock will be back to product
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
       // 1. Find the order and verify ownership
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Cannot find order with orderId: " + orderId));

        //It does not make sense but still but if by chance, lets B get oderId of A
        //So to prevent B from cancelling A's order
        if(order.getUser().getId() != userId){
            throw new RuntimeException("Access Denied: You do not own this order.");
        }

       // 2. Security Check: Only PENDING orders can be cancelled
       if(order.getOrderStatus() != OrderStatus.PENDING){
           throw new RuntimeException("Order cannot be cancelled. Current status: " + order.getOrderStatus());
       }
        // 3. Inventory Restoration: Return stock to Product table - every product
        // RESTORE STOCK: Loop through items and update products
       for(OrderItem orderItem : order.getOrderItems()) {
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
}