package com.manish.smartcart.model.cart;
import com.manish.smartcart.model.base.BaseEntity;
import com.manish.smartcart.model.product.Product;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/*
* CartItem Entity : Represents a specific product in the cart.
* It links a Product to a Cart and stores the quantity.
* */

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@SuperBuilder
@Entity
@Table(name = "cart_items")
public class CartItem extends BaseEntity{

    @ManyToOne
    @JoinColumn(name = "product_id")  // act as foreign key to link with product
    private Product product;

    private Integer quantity;

    private BigDecimal priceAtAdding; // Renamed for clarity

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "cart_id") //act as foreign key to link with Cart
    private Cart cart;

}
