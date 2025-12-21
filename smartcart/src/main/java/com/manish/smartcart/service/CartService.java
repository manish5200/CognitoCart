package com.manish.smartcart.service;

import com.manish.smartcart.model.cart.Cart;
import com.manish.smartcart.model.cart.CartItem;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.CartItemRepository;
import com.manish.smartcart.repository.CartRepository;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.repository.UsersRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UsersRepository  usersRepository;

    @Transactional
    public Cart addItemToCart(Long userId, Long productId, Integer quantity) {
        // 1. Get or Create Cart for User
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(()-> creatNewCart(userId));

        // 2. Find Product & Check stock
        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new RuntimeException("Product not found"));

        if(quantity < 0){
            throw new RuntimeException("Invalid quantity. Please try again");
        }
        if(product.getStockQuantity()<quantity){
            throw new RuntimeException("Not enough stock available. We will back soon.");
        }

        // 3. Update existing item or add new one
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(),productId)
                .orElse(new CartItem());

        //4. check if are getting else add to the new one
        if(cartItem.getId() == null){
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setPrice(product.getPrice());
            cartItem.setQuantity(quantity);
            //Final add to the list of the cart
            cart.getItems().add(cartItem);
        }else{
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        }

        // 4. Recalculate Total
        updateCartTotal(cart);

        return cartRepository.save(cart);
    }

    //Helper method to create a cart
    private Cart creatNewCart(Long userId) {

        Users user = usersRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("User not found"));
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setTotalAmount(BigDecimal.ZERO);
        return cartRepository.save(cart);
    }

    private void updateCartTotal(Cart cart) {
        BigDecimal total = cart.getItems()
                //thinks like cartItems on conveyor belt
                .stream()
                //Transform into single quantity by multiplying
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                //This is the "Sum" function for BigDecimal.
                // It starts at zero and adds every item's subtotal one by one.
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(total);

        //Simpler version
        /*BigDecimal total = BigDecimal.ZERO;
          for (CartItem item : cart.getItems()) {
            BigDecimal subtotal = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
            total = total.add(subtotal); // Note: total = total.add(), BigDecimal is immutable!
        }*/
    }


    //View cart
    public Cart getCartForUser(Long userId){
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(()-> new RuntimeException("Cart not found"));
        return cart;
    }

    //clear cart
    @Transactional
    public Cart clearTheCart(Long userId){
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        // 1. Clear the list (JPA orphanRemoval deletes the rows in DB)
        cart.getItems().clear();
        // 2. Reset the total to zero
        cart.setTotalAmount(BigDecimal.ZERO);
        return cartRepository.save(cart);
    }


    //Include coupon percentage
    @Transactional
    public Cart applyCoupon(Long userId, Double discountPercentage){
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() ->  new RuntimeException("Cart not found"));

        BigDecimal originalTotal = cart.getTotalAmount();
        BigDecimal discount =  new BigDecimal(discountPercentage)
                .divide(new BigDecimal(100));

        // Formula: Total = Total - (Total * Discount)
        BigDecimal discountAmount = originalTotal.multiply(discount);
        BigDecimal newTotal = originalTotal.subtract(discountAmount);

        // Rounding to 2 decimal places (Industry Standard)
        cart.setTotalAmount(newTotal.setScale(2, RoundingMode.HALF_UP));

        return cartRepository.save(cart);
    }

}
