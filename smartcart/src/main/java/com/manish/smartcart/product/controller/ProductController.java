package com.manish.smartcart.product.controller;

import com.manish.smartcart.infrastructure.returnpolicy.ReturnPolicyService;
import com.manish.smartcart.product.dto.*;
import com.manish.smartcart.product.service.CategoryService;
import com.manish.smartcart.product.service.ProductService;
import com.manish.smartcart.security.CustomUserDetails;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.shared.util.AppConstants;
import com.manish.smartcart.shared.util.FileValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
@Tag(name = "Product Management", description = "Browse, search, and manage products")
public class ProductController {

        private final ProductService productService;
        private final CategoryService categoryService;
        private final ReturnPolicyService returnPolicyService;

        // Get All products natively paginated
        @Operation(summary = "Get all products", description = "Retrieves a paginated list of all products in the catalog.")
        @ApiResponse(responseCode = "200", description = "Successfully retrieved products")
        @GetMapping
        public ResponseEntity<?> getAllProducts(@PageableDefault(size = 20) Pageable pageable) {
                return ResponseEntity.status(HttpStatus.OK).body(productService.getAllProducts(pageable));
        }

        /**
         * POST: Create a new product (Seller only as for now)
         * Returns 201 Created with the finalized Product (with Slug/SKU)
         */
        @Operation(summary = "Add Product (Seller Only)", description = "Creates a new product in the catalog.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Product created successfully"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Seller access required")
        })
        @SecurityRequirement(name = "bearerAuth") // Marks this specific method as protected
        @PostMapping
        @PreAuthorize("hasRole('SELLER')")
        public ResponseEntity<?> createProduct(
                        @RequestBody ProductRequest productRequest,
                        Authentication authentication) {
                Long sellerId = extractUserId(authentication);
                ProductResponse createdProduct = productService.createProduct(productRequest, sellerId);
                return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
        }

        /**
         * PUT: Update product (Seller only)
         */
        @Operation(summary = "Update Product (Seller Only)", description = "Updates an existing product in the catalog.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Product updated successfully"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Seller access required")
        })
        @SecurityRequirement(name = "bearerAuth")
        @PutMapping("/{productPublicId}")
        @PreAuthorize("hasRole('SELLER')")
        public ResponseEntity<?> updateProduct(
                        @PathVariable UUID productPublicId,
                        @RequestBody ProductRequest productRequest,
                        Authentication authentication) {
                Long sellerId = extractUserId(authentication);
                ProductResponse updatedProduct = productService.updateProduct(productPublicId, productRequest,
                                sellerId);
                return ResponseEntity.status(HttpStatus.OK).body(updatedProduct);
        }

        /**
         * GET: Product Detail by Slug or PublicId (Public)
         */
        @Operation(summary = "Get product by slug or ID", description = "Finds a specific product using its SEO-friendly slug or UUID.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Product found"),
                        @ApiResponse(responseCode = "404", description = "Product not found")
        })
        @GetMapping("/{identifier}")
        public ResponseEntity<?> getProductByIdentifier(@PathVariable String identifier) {
                return ResponseEntity.status(HttpStatus.OK).body(productService.getProductByIdentifier(identifier));
        }

        /**
         * GET: Products by Category Tree (Public)
         * Finds products in the category and all its sub-categories recursively.
         */
        @Operation(summary = "Get products by category", description = "Finds products inside a category and its recursive sub-categories using hierarchy traversal.")
        @ApiResponse(responseCode = "200", description = "Successfully retrieved category products")
        @GetMapping("/category/{categoryPublicId}")
        public ResponseEntity<?> getProductByCategoryId(
                        @PathVariable UUID categoryPublicId,
                        @PageableDefault(size = 20) Pageable pageable) {

                Long categoryId = categoryService.getCategoryIdByPublicId(categoryPublicId);
                List<Long> allCategoryIds = categoryService.getAllChildCategoryIds(categoryId);
                Page<ProductResponse> products = productService.getProductsByCategoryIds(allCategoryIds, pageable);

                return ResponseEntity.status(HttpStatus.OK).body(products);
        }

        /**
         * PATCH: Toggle Visibility (Seller/Admin Only)
         */
        @Operation(summary = "Toggle product availability", description = "Allows a seller or admin to hide/show a product from the public catalog.")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Visibility toggled successfully"),
                @ApiResponse(responseCode = "403", description = "Forbidden - User does not own this product")
        })
        @PatchMapping("/{productPublicId}/toggle")
        @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
        public ResponseEntity<?> toggleVisibility(@PathVariable UUID productPublicId,
                        Authentication authentication) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                assert userDetails != null;
                boolean isAdmin = userDetails.getAuthorities()
                                .stream()
                                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));
                productService.toggleAvailability(productPublicId, userDetails.getUser().getId(), isAdmin);
                return ResponseEntity.ok(Map.of("message", "Visibility updated successfully."));
        }

        /**
         * DELETE: Remove Product (Seller/Admin Only)
         */
        @Operation(summary = "Delete product", description = "Soft DELETES a product from the database.")
        @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Product deleted successfully"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - User does not own this product") })
        @DeleteMapping("/{productPublicId}")
        @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
        public ResponseEntity<?> deleteProduct(@PathVariable UUID productPublicId,
                        Authentication authentication) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                assert userDetails != null;
                boolean isAdmin = userDetails.getAuthorities()
                                .stream()
                                .anyMatch(a -> Objects.equals(a
                                                .getAuthority(), "ROLE_ADMIN"));

                productService.deleteProduct(productPublicId, userDetails.getUser().getId(), isAdmin);
                return ResponseEntity.ok(Map.of("message", "Product deleted successfully."));
        }

        /**
         * Advanced Search and Filtering Endpoint
         * GET /api/products/search?category=Electronics&maxPrice=500&page=0&size=10
         */

        @Operation(summary = "Search Products", description = "Search products by name, "
                        + "category, or price range with pagination.")
        @ApiResponse(responseCode = "200", description = "Search operation successful")
        @GetMapping("/search")
        public ResponseEntity<?> searchProduct(@Valid ProductSearchDTO searchDTO,
                        @RequestParam(name = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
                        @RequestParam(name = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
                        @RequestParam(name = "sortBy", defaultValue = AppConstants.DEFAULT_SORT_BY) String sortBy,
                        @RequestParam(name = "direction", defaultValue = AppConstants.DEFAULT_SORT_DIRECTION) String direction) {
                Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending()
                                : Sort.by(sortBy).ascending();

                Pageable pageable = PageRequest.of(page, size, sort);

                Page<ProductResponse> result = productService.getFilteredProduct(searchDTO, pageable);

                return ResponseEntity.status(HttpStatus.OK).body(Map.of("Search result", result));
        }


        /**
         * PHASE 4 — AI Semantic Vector Search
         * GET /api/v1/products/search/semantic?q=your query&limit=10
         * Unlike keyword search (LIKE '%word%'), this finds products by MEANING.
         * "earphones for studying" → finds "Noise Cancelling Headphones" even with no
         * matching words.
         * Flow: query text → HuggingFace float[384] vector → pgvector cosine similarity
         * → top N results
         */
        // ─── UPDATED: /search/semantic now supports optional filter params ──────────
        @Operation(summary = "🤖 Semantic AI Search", description = "Find products by meaning. Optionally combine with price/rating filters.")
        @ApiResponse(responseCode = "200", description = "Top N closest semantic matches determined by cosine distance")
        @GetMapping("/search/semantic")
        public ResponseEntity<SemanticSearchResponse> semanticSearch(
                        @RequestParam String q,
                        @RequestParam(defaultValue = "10") int limit,
                        @RequestParam(required = false) BigDecimal minPrice,
                        @RequestParam(required = false) BigDecimal maxPrice,
                        @RequestParam(required = false) Double minRating) {
                SemanticSearchResponse response = productService.semanticSearch(q, limit, minPrice, maxPrice,
                                minRating);
                return ResponseEntity.ok(response);
        }

        // ─── NEW: Admin endpoint to fix old products with missing embeddings ─────────
        // Only admins can trigger this (it makes ~N HuggingFace API calls)
        @Operation(summary = "🔄 Reindex product embeddings", description = "Admin-only: generates AI embeddings for all products that are missing one.")
        @PostMapping("/admin/reindex")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<?> reindexEmbeddings() {
                int indexed = productService.reindexMissingEmbeddings();
                return ResponseEntity.ok(Map.of(
                                "message", "Reindex complete",
                                "productsIndexed", indexed));
        }

        @Operation(summary = "Get return policy for product", description = "Returns the live applicable return/exchange policy for a product. "
                        + "Follows the chain: product-level → category-level → NON_RETURNABLE default.")
        @ApiResponse(responseCode = "200", description = "Policy retrieved")
        @GetMapping("/{productPublicId}/return-policy")
        public ResponseEntity<ReturnPolicyResponse> getProductReturnPolicy(@PathVariable UUID productPublicId) {
                return ResponseEntity.ok(returnPolicyService.getLivePolicyResponse(productPublicId));
        }

        @Operation(summary = "Set return policy", description = "Creates a return policy for a product or category.")
        @ApiResponse(responseCode = "201", description = "Policy created")
        @PostMapping("/return-policy")
        @PreAuthorize("hasRole('SELLER')")
        public ResponseEntity<ReturnPolicyResponse> createReturnPolicy(
                        @RequestBody ReturnPolicyRequest request,
                        Authentication authentication) {
                Long sellerId = extractUserId(authentication);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(returnPolicyService.createPolicy(sellerId, request));
        }

        @Operation(summary = "Update return policy", description = "Updates an existing return policy.")
        @ApiResponse(responseCode = "200", description = "Policy updated")
        @PutMapping("/return-policy/{policyPublicId}")
        @PreAuthorize("hasRole('SELLER')")
        public ResponseEntity<ReturnPolicyResponse> updateReturnPolicy(
                        @PathVariable UUID policyPublicId,
                        @RequestBody ReturnPolicyRequest request,
                        Authentication authentication) {
                Long sellerId = extractUserId(authentication);
                return ResponseEntity.ok(returnPolicyService.updatePolicy(sellerId, policyPublicId, request));
        }

        // ─── V4.2 MODERATION PIPELINE ENDPOINTS ──────────────────────────────────────
        
        @Operation(summary = "Admin: Get Pending Products", description = "Retrieves all products currently in PENDING_REVIEW status.")
        @GetMapping("/admin/pending")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<?> getProductsPendingReview(@PageableDefault(size = 20) Pageable pageable) {
                return ResponseEntity.ok(productService.getProductsPendingReview(pageable));
        }

        @Operation(summary = "Admin: Get Moderation History", description = "Retrieves the immutable audit ledger for a product.")
        @GetMapping("/admin/{productPublicId}/moderation-history")
        @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
        public ResponseEntity<List<com.manish.smartcart.product.dto.ProductModerationHistoryResponse>> getProductModerationHistory(
                        @PathVariable UUID productPublicId) {
                return ResponseEntity.ok(productService.getProductModerationHistory(productPublicId));
        }

        @Operation(summary = "Admin: Moderate Product", description = "Allows an Admin to Approve, Reject, or Request Changes for a product currently in PENDING_REVIEW status.")
        @PostMapping("/admin/{productPublicId}/moderate")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<?> moderateProduct(
                        @PathVariable UUID productPublicId,
                        @Valid @RequestBody ProductModerationRequest request,
                        Authentication authentication) {
                Long adminId = extractUserId(authentication);

                productService.moderateProduct(
                                productPublicId,
                                request.getAction(),
                                request.getReason(),
                                adminId);

                return ResponseEntity.ok(Map.of(
                                "status", "success",
                                "message",
                                "Product moderation action '" + request.getAction() + "' recorded successfully."));
        }

        @Operation(summary = "Seller: Submit for Review", description = "Allows a Seller to submit a DRAFT or REQUIRES_CHANGES product into the Admin Moderation Queue.")
        @PostMapping("/{productPublicId}/submit-for-review")
        @PreAuthorize("hasRole('SELLER')")
        public ResponseEntity<?> submitForReview(
                        @PathVariable UUID productPublicId,
                        Authentication authentication) {

                Long sellerId = extractUserId(authentication);
                productService.submitForReview(
                                productPublicId,
                                sellerId);
                return ResponseEntity.ok(Map.of(
                                "status", "success",
                                "message",
                                "Product successfully submitted for Admin review. It is now in PENDING_REVIEW status."));
        }

        // ─── MEDIA ENDPOINTS ────────────────────────────────────────────────────────

        // Image upload
        @Operation(summary = "Upload product image",
                description = "Validates and uploads an image to the Cloudinary CDN, linking it to the product's media gallery.")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Image uploaded and CDN URL returned"),
                @ApiResponse(responseCode = "400", description = "Invalid file type or size limits exceeded")
        })
        @PostMapping("/{productPublicId}/upload-image")
        @PreAuthorize("hasRole('SELLER')")
        public ResponseEntity<?> uploadProductImage(
                @PathVariable UUID productPublicId,
                @RequestParam("file") MultipartFile file,
                Authentication authentication) {

                FileValidator.validateImage(file);

                Long sellerId = extractUserId(authentication);

                Map<String, String> uploadResult = productService.uploadProductImage(productPublicId, file, sellerId);

                return ResponseEntity.ok(Map.of(
                        "message", "Image uploaded successfully to Cloudinary CDN",
                        "imageUrl", uploadResult.get("imageUrl"),
                        "publicId", uploadResult.get("publicId")
                ));
        }

        @Operation(summary = "Delete product image", description = "Erases the image from the Cloudinary CDN and removes it from the product gallery.")
        @ApiResponse(responseCode = "200", description = "Image deleted successfully")
        @DeleteMapping("/{productPublicId}/images")
        @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
        public ResponseEntity<?> deleteProductImage(
                @PathVariable UUID productPublicId,
                @RequestParam String publicId, // e.g. "products/usb-hub-abc123"
                Authentication authentication) {

                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                Long actorId = extractUserId(authentication);

                boolean isAdmin = userDetails.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                productService.deleteProductImage(productPublicId, publicId, actorId, isAdmin);

                return ResponseEntity.ok(Map.of(
                        "message", "Image deleted successfully",
                        "deletedPublicId", publicId,
                        "deletedByAdmin", isAdmin
                ));
        }


        // HELPER FUNCTION TO EXTRACT UserId
        private long extractUserId(Authentication authentication) {
                CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
                if (customUserDetails == null) {
                        throw new BusinessLogicException("Authentication context is missing. Please log in again.");
                }
                return customUserDetails.getUser().getId();
        }
}
