package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.product.ProductRequest;
import com.manish.smartcart.dto.product.ProductResponse;
import com.manish.smartcart.dto.product.ProductSearchDTO;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.service.CategoryService;
import com.manish.smartcart.service.FileService;
import com.manish.smartcart.service.ProductService;
import com.manish.smartcart.util.AppConstants;
import com.manish.smartcart.util.FileValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/products")
@Tag(name = "4. Product Management", description = "Browse, search, and manage products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductRepository productRepository;
    private final FileService fileService;
    public  ProductController(
            ProductService productService,
            CategoryService categoryService,
            ProductRepository productRepository,
            FileService fileService ) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.productRepository = productRepository;
        this.fileService = fileService;
    }

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
    @Operation(summary = "Add Product (Seller Only)", description = "Creates a new product in the catalog.")
    @SecurityRequirement(name = "bearerAuth") // Marks this specific method as protected
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?>createProduct(@RequestBody ProductRequest productRequest, Authentication authentication) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        assert userDetails != null;
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
        assert userDetails != null;
        boolean isAdmin = userDetails.getAuthorities()
                    .stream()
                    .anyMatch(a-> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));
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
        assert userDetails != null;
        boolean isAdmin = userDetails.getAuthorities()
                     .stream()
                     .anyMatch(a -> Objects.equals(a
                             .getAuthority(), "ROLE_ADMIN"));

             productService.deleteProduct(id,userDetails.getUserId(),isAdmin);
             return ResponseEntity.ok(Map.of("message", "Product deleted successfully."));
    }


    /**
     * Advanced Search and Filtering Endpoint
     * GET /api/products/search?category=Electronics&maxPrice=500&page=0&size=10
     */

    @Operation(summary = "Search Products",
            description = "Search products by name, " +
                    "category, or price range with pagination.")
    @GetMapping("/search")
    public ResponseEntity<?>searchProduct(
            @Valid ProductSearchDTO searchDTO,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String direction){
            Sort sort = direction.equalsIgnoreCase("desc") ?
                    Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);

            Page<ProductResponse> result = productService.getFilteredProduct(searchDTO, pageable);

            return ResponseEntity.status(HttpStatus.OK).body(Map.of("Search result",result));
    }


    //Image upload
    @PostMapping("/{productId}/upload-image")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> uploadProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) throws IOException {

        // 1. Validate the file (Security First!)
        FileValidator.validateImage(file);

        // 2. Proceed with upload if validation passes
        String fileName = fileService.uploadImage(file);

        // 3. Update Product in Database
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setImageUrls(Collections.singletonList(fileName));
        productRepository.save(product);
        return ResponseEntity.ok(Map.of(
                "message", "Image verified and uploaded successfully",
                "fileName", fileName
        ));
    }

}
