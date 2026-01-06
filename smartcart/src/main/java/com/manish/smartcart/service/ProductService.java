package com.manish.smartcart.service;

import com.manish.smartcart.dto.product.ProductRequest;
import com.manish.smartcart.dto.product.ProductResponse;
import com.manish.smartcart.dto.product.ProductSearchDTO;
import com.manish.smartcart.mapper.ProductMapper;
import com.manish.smartcart.model.product.Category;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.repository.CategoryRepository;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.repository.specifications.ProductSpecifications;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper  productMapper;
    private final CategoryService categoryService;


    /**
     * ACTIVITY: Onboarding (Creation)
     * Handles Slug and SKU generation automatically.
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest, Long currentSellerId) {

        Product product = productMapper.toProduct(productRequest);

        // 2. Assign the seller ID from the authenticated user
        product.setSellerId(currentSellerId);

        // 3. Resolve the Category Link (Crucial Fix)
        if (productRequest.getCategoryId() != null) {
            // Fetch the separate Category entity from its repository
            Category category = categoryRepository.findById(productRequest.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with ID: " + productRequest.getCategoryId()));

            // Map the full entity to the Product's @ManyToOne field
            product.setCategory(category);
        }

        // 4. Generate SEO-friendly slug: "Apple iPhone 15" -> "apple-iphone-15-uuid" , "Gaming Mouse" -> "gaming-mouse-a1b2" , "Nike Air Max" -> "nike-air-max-a1b2c"
        // This ensures SEO-friendly URLs and prevents duplicates across different sellers.
        String slug = productRequest.getProductName().toLowerCase().replaceAll("[^a-z0-9]", "-");
        product.setSlug(slug + "-" + UUID.randomUUID().toString().substring(0, 5));

        // 5. Smart SKU Generation, if not provided
        // Warehouse-ready ID if the seller leaves it blank.
        if(productRequest.getSku() == null || productRequest.getSku().isBlank()){
            product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }else{
            product.setSku(productRequest.getSku());
        }

        Product savedProduct = productRepository.save(product);
        return productMapper.toProductResponse(savedProduct);
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
             ProductResponse productResponse = productMapper.toProductResponse(product);
              body.add(productResponse);
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


    //Filter the product using specification

    public Page<ProductResponse> getFilteredProduct(ProductSearchDTO searchDTO, Pageable pageable) {

        // 1. Initialize an empty Specification (the "base" query)
        Specification<Product> specs = (root, query, cb) -> cb.conjunction();


        // 2. Dynamically "chain" filters only if they are provided in the DTO
        if (searchDTO.getCategory() != null && !searchDTO.getCategory().isEmpty()) {
            //1. Find the parent category by name (as sent in search)
            Category parentCategory = categoryRepository.findByNameIgnoreCase(searchDTO.getCategory())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            //2. Use your CategoryService to get the whole tree of IDs
            List<Long> categoryIds = categoryService.getAllChildCategoryIds(parentCategory.getId());
            //3. Filter products that belong to ANY of these IDs
            specs = specs.and(ProductSpecifications.hasCategoryIdIn(categoryIds));
        }


            if (searchDTO.getMinPrice() != null) {
                specs = specs.and(ProductSpecifications.hasPriceGreaterThan(searchDTO.getMinPrice()));
            }

            if (searchDTO.getMaxPrice() != null) {
                specs = specs.and(ProductSpecifications.hasPriceLessThan(searchDTO.getMaxPrice()));
            }

            if (searchDTO.getMinRating() != null) {
                specs = specs.and(ProductSpecifications.hasMinRating(searchDTO.getMinRating()));
            }

            if (searchDTO.getKeyword() != null && !searchDTO.getKeyword().isEmpty()) {
                specs = specs.and(ProductSpecifications.hasKeyword(searchDTO.getKeyword()));
            }

            // 3. Execute the dynamic query with pagination
            Page<Product> productPage = productRepository.findAll(specs, pageable);

            // 4. Transform entities to DTOs for the frontend
            return productPage.map(productMapper::toProductResponse);
        }
    }
