package com.manish.smartcart.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name="products")
public class Product {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    private String productName;
    private String description;
    private BigDecimal price;
    private Long quantity;


    @ManyToOne
    @JoinColumn(name = "seller_id")
    private Users seller;


}
