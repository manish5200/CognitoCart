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
