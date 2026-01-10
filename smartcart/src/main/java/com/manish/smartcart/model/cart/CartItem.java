package com.manish.smartcart.model.cart;
import com.manish.smartcart.model.base.BaseEntity;
import com.manish.smartcart.model.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/*
* CartItem Entity : Represents a specific product in the cart.
* It links a Product to a Cart and stores the quantity.
* */

@Entity
@Table(name = "cart_items")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class CartItem extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")  // act as foreign key to link with product
    private Product product;

    private Integer quantity;

    private BigDecimal priceAtAdding; // Renamed for clarity

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "cart_id") //act as foreign key to link with Cart
    private Cart cart;

}
