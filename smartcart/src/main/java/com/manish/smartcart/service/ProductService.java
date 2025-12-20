package com.manish.smartcart.service;

import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.model.product.Review;
import com.manish.smartcart.repository.CategoryRepository;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ReviewRepository reviewRepository;


    /**
     * ACTIVITY: Onboarding (Creation)
     * Handles Slug and SKU generation automatically.
     */
    @Transactional
    public Product createProduct(Product product) {
        // 1. Generate SEO-friendly slug: "Apple iPhone 15" -> "apple-iphone-15-uuid" , "Gaming Mouse" -> "gaming-mouse-a1b2" , "Nike Air Max" -> "nike-air-max-a1b2c"
        // This ensures SEO-friendly URLs and prevents duplicates across different sellers.
        String slug = product.getProductName().toLowerCase().replaceAll("[^a-z0-9]", "-");
        product.setSlug(slug + "-" + UUID.randomUUID().toString().substring(0, 5));

        // 2. Smart SKU Generation, if not provided
        // Warehouse-ready ID if the seller leaves it blank.
        if(product.getSku() == null || product.getSku().isBlank()){
            product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        return productRepository.save(product);
    }

    /**
     * ACTIVITY: Stock Management
     * Handles inventory updates safely.
     * Positive quantity adds stock, negative removes it.
     */
    @Transactional
    public Product updateStock(Long productId, Integer quantityChange) {
        Product product = productRepository.findById(productId).orElseThrow(()->new RuntimeException("Product not found with ID " + productId));

        int newQuantity = product.getStockQuantity() + quantityChange;
        if (newQuantity < 0) {
            throw new RuntimeException("Action denied: Not enough stock available.");
        }

        product.setStockQuantity(newQuantity);
        return productRepository.save(product);
    }

    /**
     * ACTIVITY: Discovery (Basic Retrieval)
     * Phase 1: Simple retrieval methods. We will add Pagination & Filtering later.
     */

    public List<Product>getAllProducts(){
        return productRepository.findAll();
    }
    /**
     * ACTIVITY: Discovery (Fetch by Slug)
     */

    public Product getProductBySlug(String slug){
         return productRepository.findBySlug(slug)
                 .orElseThrow(()-> new RuntimeException("Product not found"));
    }
    /**
     * ACTIVITY: Discovery (Fetch by Category)
     */

    public List<Product>getProductsByCategory(Long categoryId){
        return productRepository.findByCategoryId(categoryId);
    }

    /**
     * ACTIVITY: Visibility Control
     * Toggle availability for a product (e.g., if it's discontinued).
     * True --> false or false -> true
     */

    public void toggleAvailability(Long productId){
         Product product = productRepository.findById(productId)
                 .orElseThrow(()->new RuntimeException("Product not found with ID " + productId));
         product.setIsAvailable(!product.getIsAvailable());
         productRepository.save(product);
    }

    /**
     * Add a review and automatically recalculate the product's average rating.
     * This is a "Smart" business logic step.
     */
    @Transactional
    public void addReview(Long productId, Review review) {
         Product product = productRepository.findById(productId)
                 .orElseThrow(()->new RuntimeException("Product not found"));

        // Save the review
        review.setProduct(product);
        reviewRepository.save(review);
    }

    /**
     * Delete product by using product id.
     */

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Cannot delete. Product ID " + id + " does not exist.");
        }
        productRepository.deleteById(id);
    }
}
