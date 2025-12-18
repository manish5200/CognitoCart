package com.manish.smartcart.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    // PUBLIC API (no auth required)
    @GetMapping("/public")
    public String publicProducts() {
        return "Public products visible to everyone";
    }

    // CUSTOMER, SELLER, ADMIN (any authenticated user)
    @PreAuthorize("hasAnyRole('ADMIN','SELLER','CUSTOMER')")
    @GetMapping
    public String viewProducts() {
        return "Products visible to logged-in users";
    }

    // ONLY SELLER & ADMIN
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @PostMapping
    public String addProduct() {
        return "Product added successfully";
    }

    // ONLY ADMIN
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id) {
        return "Product with id " + id + " deleted";
    }
}
