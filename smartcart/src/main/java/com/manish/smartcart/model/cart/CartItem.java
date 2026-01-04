package com.manish.smartcart.model.cart;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manish.smartcart.model.product.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/*
* CartItem Entity : Represents a specific product in the cart.
* It links a Product to a Cart and stores the quantity.
* */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")  // act as foreign key to link with product
    private Product product;

    private Integer quantity;

    private BigDecimal price; // Price at the time of adding to cart

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "cart_id") //act as foreign key to link with Cart
    @JsonIgnore // Keep this here as well
    private Cart cart;

}
