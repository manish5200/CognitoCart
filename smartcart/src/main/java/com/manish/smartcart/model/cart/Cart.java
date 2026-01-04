package com.manish.smartcart.model.cart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.manish.smartcart.model.user.Users;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
* Cart Entity: Represents the container. It has a One-to-One relationship with the User.
**/
@Data
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private Users user;

    @JsonIgnoreProperties("cart") // Prevents CartItem from reaching back to this Cart
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<CartItem>();

    private BigDecimal totalAmount = BigDecimal.ZERO;

    // RECTIFICATION: Helper to sync bidirectional relationship
    public void addCartItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }
    // RECTIFICATION: Helper to safely remove and break link
    public void removeCartItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }
}
