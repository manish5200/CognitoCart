package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.product.ProductRequest;
import com.manish.smartcart.dto.product.ProductResponse;
import com.manish.smartcart.dto.product.ProductSearchDTO;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.repository.UsersRepository;
import com.manish.smartcart.service.CategoryService;
import com.manish.smartcart.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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


    //Get All products
    @GetMapping
    public ResponseEntity<?>getAllProducts(){
         return ResponseEntity
                 .status(HttpStatus.OK)
                 .body(Map.of("List of Products",productService.getAllProducts()));
    }

    /**
     * POST: Create a new product (Seller only as for now)
     * Returns 201 Created with the finalized Product (with Slug/SKU)
     */
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?>createProduct(@RequestBody ProductRequest productRequest, Authentication authentication) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            ProductResponse createdProduct = productService
                    .createProduct(productRequest,userDetails.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }


    /**
     * GET: Product Detail by Slug (Public)
     */
    @GetMapping("/{slug}")
    public ResponseEntity<?>getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(productService.getProductBySlug(slug));

    }

    /**
     * GET: Products by Category Tree (Public)
     * Finds products in the category and all its sub-categories recursively.
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?>getProductByCategoryId(@PathVariable Long categoryId) {
            List<Long>allCategoryIds = categoryService.getAllChildCategoryIds(categoryId);
            List<Product>products = productService.getProductsByCategoryIds(allCategoryIds);

            // Return 200 OK with empty list if no products,
            // OR handle the case where the parent category ID itself doesn't exist.
            return ResponseEntity.status(HttpStatus.OK).body(products);
    }

    /**
     * PATCH: Toggle Visibility (Seller/Admin Only)
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public ResponseEntity<?>toggleVisibility(@PathVariable Long id, Authentication authentication) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            boolean isAdmin = userDetails.getAuthorities()
                    .stream()
                    .anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));
            productService.toggleAvailability(id,userDetails.getUserId(),isAdmin);
            return ResponseEntity.ok(Map.of("message", "Visibility updated successfully."));
    }

    /**
     * DELETE: Remove Product (Seller/Admin Only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<?>deleteProduct(@PathVariable Long id, Authentication authentication) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
             boolean isAdmin = userDetails.getAuthorities()
                     .stream()
                     .anyMatch(a -> a
                             .getAuthority().equals("ROLE_ADMIN"));

             productService.deleteProduct(id,userDetails.getUserId(),isAdmin);
             return ResponseEntity.ok(Map.of("message", "Product deleted successfully."));
    }


    /**
     * Advanced Search and Filtering Endpoint
     * GET /api/products/search?category=Electronics&maxPrice=500&page=0&size=10
     */

    @GetMapping("/search")
    public ResponseEntity<?>searchProduct(
            @Valid ProductSearchDTO searchDTO,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction){
            Sort sort = direction.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);

            Page<ProductResponse> result = productService.getFilteredProduct(searchDTO, pageable);

            return ResponseEntity.status(HttpStatus.OK).body(Map.of("Search result",result));
    }

}
