package com.manish.smartcart.model.order;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.manish.smartcart.model.product.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id") // creates a column(order_id) in order_items table acting as FK for orders table
    @JsonBackReference //Prevents infinite recursion in JSON
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id") //creates a column(product_id) in order_items table acting as FK for products table
    private Product product;

    private Integer quantity;

   //Crucial: Storing the price at checkout, not the current product price!
   // This is the "Frozen" price. We do NOT use product.getPrice()
   // for historical viewing; we use this stored value.
    private BigDecimal priceAtPurchase;
}
