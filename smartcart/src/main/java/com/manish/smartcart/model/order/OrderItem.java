package com.manish.smartcart.model.order;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.manish.smartcart.model.product.Product;
import jakarta.persistence.*;

import java.math.BigDecimal;

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

   public OrderItem() {}



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPriceAtPurchase() {
        return priceAtPurchase;
    }

    public void setPriceAtPurchase(BigDecimal priceAtPurchase) {
        this.priceAtPurchase = priceAtPurchase;
    }
}
