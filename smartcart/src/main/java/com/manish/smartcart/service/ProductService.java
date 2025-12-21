package com.manish.smartcart.service;

import com.manish.smartcart.dto.ProductResponse;
import com.manish.smartcart.model.product.Category;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.model.product.Review;
import com.manish.smartcart.repository.CategoryRepository;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
    public Product createProduct(Product product, Long currentSellerId) {
        // 1. Assign the seller ID from the authenticated user
        product.setSellerId(currentSellerId);

        // 2. Resolve the Category Link (Crucial Fix)
        if (product.getCategoryId() != null) {
            // Fetch the separate Category entity from its repository
            Category category = categoryRepository.findById(product.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with ID: " + product.getCategoryId()));

            // Map the full entity to the Product's @ManyToOne field
            product.setCategory(category);
        }

        // 3. Generate SEO-friendly slug: "Apple iPhone 15" -> "apple-iphone-15-uuid" , "Gaming Mouse" -> "gaming-mouse-a1b2" , "Nike Air Max" -> "nike-air-max-a1b2c"
        // This ensures SEO-friendly URLs and prevents duplicates across different sellers.
        String slug = product.getProductName().toLowerCase().replaceAll("[^a-z0-9]", "-");
        product.setSlug(slug + "-" + UUID.randomUUID().toString().substring(0, 5));

        // 4. Smart SKU Generation, if not provided
        // Warehouse-ready ID if the seller leaves it blank.
        if(product.getSku() == null || product.getSku().isBlank()){
            product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        return productRepository.save(product);
    }

    /**
     * ACTIVITY: Visibility Control
     * Toggle availability for a product (e.g., if it's discontinued).
     * True --> false or false -> true
     * Logic ensures only the owner or an admin can hide/show the product.
     */

    @Transactional
    public void toggleAvailability(Long productId, Long currentSellerId, boolean isAdmin) {
         Product product = productRepository.findById(productId)
                 .orElseThrow(()->new RuntimeException("Product not found with ID " + productId));

         // Security Check: Unauthorized if not Admin and not the Owner
         if(!isAdmin && !product.getSellerId().equals(currentSellerId)){
             throw new RuntimeException("Access Denied: You do not have permission to modify this product.");
         }
         product.setIsAvailable(!product.getIsAvailable());
         productRepository.save(product);
    }

    /**
     * ACTIVITY: Stock Management
     * ACTIVITY: Inventory Management
     * Handles inventory updates safely.
     * Positive quantity adds stock, negative removes it.
     */
    @Transactional
    public Product updateStock(Long productId, Integer quantityChange, Long currentSellerId, boolean isAdmin) {
        Product product = productRepository.findById(productId)
                .orElseThrow(()->new RuntimeException("Product not found with ID " + productId));

        if(!isAdmin && !product.getSellerId().equals(currentSellerId)){
            throw new RuntimeException("Access Denied: Inventory updates restricted to owners.");
        }
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

    public List<ProductResponse>getAllProducts(){
        List<ProductResponse>body = new ArrayList<>();
        for(Product product: productRepository.findAll()){
              ProductResponse newProd = new ProductResponse(
                product.getId(),
                product.getProductName(),
                      product.getDescription(),
                      product.getPrice(),
                      product.getStockQuantity()
              );
              body.add(newProd);
        }
        return body;
    }

    /**
     * ACTIVITY: Discovery (Fetch by Category)
     */
    public List<Product>getProductsByCategoryIds(List<Long>categoryId){
        return productRepository.findByCategoryIdIn(categoryId);
    }

    /**
     * ACTIVITY: Discovery (Fetch by Slug)
     */
    public Product getProductBySlug(String slug){
        return productRepository.findBySlug(slug)
                .orElseThrow(()-> new RuntimeException("Product not found"));
    }

    /**
     * Add a review and automatically recalculate the product's average rating.
     * This is a "Smart" business logic step.
     * Done by customer only
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
     * Done by seller or Admin only
     */

    @Transactional
    public void deleteProduct(Long id, Long authenticatedUserId, boolean isAdmin) {
        // Combined exists and find into one call for efficiency
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cannot delete. Product ID " + id + " does not exist."));

        if (!isAdmin && !product.getSellerId().equals(authenticatedUserId)) {
            throw new RuntimeException("Access Denied: Only owners or admins can delete products.");
        }
        productRepository.deleteById(id);
    }
}
