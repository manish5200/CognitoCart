package com.manish.smartcart.model.cart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manish.smartcart.model.base.BaseEntity;
import com.manish.smartcart.model.user.Users;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Cart Entity: Represents the container. It has a One-to-One relationship with
 * the User.
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Table(name = "carts")
public class Cart extends BaseEntity { // Added BaseEntity for Versioning

    @OneToOne
    @JoinColumn(name = "user_id")
    private Users user;
    @JsonIgnoreProperties("cart") // Prevents CartItem from reaching back to this Cart
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<CartItem>();

    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // --- COUPONS ---
    private String couponCode;

    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // --- DELIVERY ---
    // CONCEPT: The Delivery Fee is calculated *dynamically* inside the CartService
    // anytime someone adds/removes an item. If their net total drops below 599, this becomes 50.
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;


    //Helper to sync bidirectional relationship
    public void addCartItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    //Helper to safely remove and break link
    public void removeCartItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

}
