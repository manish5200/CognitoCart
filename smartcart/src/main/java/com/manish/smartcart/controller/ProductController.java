package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.repository.UsersRepository;
import com.manish.smartcart.service.CategoryService;
import com.manish.smartcart.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UsersRepository usersRepository;

    /**
     * POST: Create a new product (Seller only as for now)
     * Returns 201 Created with the finalized Product (with Slug/SKU)
     */
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?>createProduct(@RequestBody Product product, Authentication authentication) {
        try{
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Product createdProduct = productService.createProduct(product,userDetails.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }


    /**
     * GET: Product Detail by Slug (Public)
     */
    @GetMapping("/{slug}")
    public ResponseEntity<?>getProductBySlug(@PathVariable String slug) {
         try{
             return ResponseEntity.status(HttpStatus.OK).body(productService.getProductBySlug(slug));
         }catch(Exception e){
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
         }
    }

    /**
     * GET: Products by Category Tree (Public)
     * Finds products in the category and all its sub-categories recursively.
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?>getProductByCategoryId(@PathVariable Long categoryId) {
        try{
            List<Long>allCategoryIds = categoryService.getAllChildCategoryIds(categoryId);
            List<Product>products = productService.getProductsByCategoryIds(allCategoryIds);

            // Return 200 OK with empty list if no products,
            // OR handle the case where the parent category ID itself doesn't exist.
            return ResponseEntity.status(HttpStatus.OK).body(products);
        }catch (Exception e){
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error",e.getMessage()));
        }
    }

    /**
     * PATCH: Toggle Visibility (Seller/Admin Only)
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<?>toggleVisibility(@PathVariable Long id, Authentication authentication) {
        try{
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            boolean isAdmin = userDetails.getAuthorities()
                    .stream()
                    .anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));
            productService.toggleAvailability(id,userDetails.getUserId(),isAdmin);
            return ResponseEntity.ok(Map.of("message", "Visibility updated successfully."));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE: Remove Product (Seller/Admin Only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<?>deleteProduct(@PathVariable Long id, Authentication authentication) {
         try{
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
             boolean isAdmin = userDetails.getAuthorities()
                     .stream()
                     .anyMatch(a -> a
                             .getAuthority().equals("ROLE_ADMIN"));

             productService.deleteProduct(id,userDetails.getUserId(),isAdmin);
             return ResponseEntity.ok(Map.of("message", "Product deleted successfully."));
         }catch(Exception e){
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
         }
    }


}
