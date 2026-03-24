package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.product.ProductRequest;
import com.manish.smartcart.dto.product.ProductResponse;
import com.manish.smartcart.dto.product.ProductSearchDTO;
import com.manish.smartcart.mapper.ProductMapper;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.service.*;
import com.manish.smartcart.util.AppConstants;
import com.manish.smartcart.util.FileValidator;
import com.manish.smartcart.util.VectorAttributeConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
@Tag(name = "4. Product Management", description = "Browse, search, and manage products")
public class ProductController {

        private final ProductService productService;
        private final CategoryService categoryService;
        private final ProductRepository productRepository;
        private final FileService fileService;
        private final CloudinaryService cloudinaryService;
        private final EmbeddingService embeddingService;
        private final ProductMapper productMapper;

        // Get All products
        @GetMapping
        public ResponseEntity<?> getAllProducts() {
                return ResponseEntity
                                .status(HttpStatus.OK)
                                .body(Map.of("List of Products", productService.getAllProducts()));
        }

        /**
         * POST: Create a new product (Seller only as for now)
         * Returns 201 Created with the finalized Product (with Slug/SKU)
         */
        @Operation(summary = "Add Product (Seller Only)", description = "Creates a new product in the catalog.")
        @SecurityRequirement(name = "bearerAuth") // Marks this specific method as protected
        @PostMapping
        @PreAuthorize("hasRole('SELLER')")
        public ResponseEntity<?> createProduct(@RequestBody ProductRequest productRequest,
                        Authentication authentication) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                assert userDetails != null;
                ProductResponse createdProduct = productService
                                .createProduct(productRequest, userDetails.getUser().getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
        }

        /**
         * GET: Product Detail by Slug (Public)
         */
        @GetMapping("/{slug}")
        public ResponseEntity<?> getProductBySlug(@PathVariable String slug) {
                return ResponseEntity.status(HttpStatus.OK)
                                .body(productService.getProductBySlug(slug));

        }

        /**
         * GET: Products by Category Tree (Public)
         * Finds products in the category and all its sub-categories recursively.
         */
        @GetMapping("/category/{categoryId}")
        public ResponseEntity<?> getProductByCategoryId(@PathVariable Long categoryId) {
                List<Long> allCategoryIds = categoryService.getAllChildCategoryIds(categoryId);
                List<ProductResponse> products = productService.getProductsByCategoryIds(allCategoryIds);

                // Return 200 OK with empty list if no products,
                // OR handle the case where the parent category ID itself doesn't exist.
                return ResponseEntity.status(HttpStatus.OK).body(products);
        }

        /**
         * PATCH: Toggle Visibility (Seller/Admin Only)
         */
        @PatchMapping("/{id}/toggle")
        @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
        public ResponseEntity<?> toggleVisibility(@PathVariable Long id,
                        Authentication authentication) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                assert userDetails != null;
                boolean isAdmin = userDetails.getAuthorities()
                                .stream()
                                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));
                productService.toggleAvailability(id, userDetails.getUser().getId(), isAdmin);
                return ResponseEntity.ok(Map.of("message", "Visibility updated successfully."));
        }

        /**
         * DELETE: Remove Product (Seller/Admin Only)
         */
        @DeleteMapping("/{id}")
        @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
        public ResponseEntity<?> deleteProduct(@PathVariable Long id,
                        Authentication authentication) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                assert userDetails != null;
                boolean isAdmin = userDetails.getAuthorities()
                                .stream()
                                .anyMatch(a -> Objects.equals(a
                                                .getAuthority(), "ROLE_ADMIN"));

                productService.deleteProduct(id, userDetails.getUser().getId(), isAdmin);
                return ResponseEntity.ok(Map.of("message", "Product deleted successfully."));
        }

        /**
         * Advanced Search and Filtering Endpoint
         * GET /api/products/search?category=Electronics&maxPrice=500&page=0&size=10
         */

        @Operation(summary = "Search Products", description = "Search products by name, " +
                        "category, or price range with pagination.")
        @GetMapping("/search")
        public ResponseEntity<?> searchProduct(
                        @Valid ProductSearchDTO searchDTO,
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

        // Image upload
        @PostMapping("/{productId}/upload-image")
        @PreAuthorize("hasRole('SELLER')")
        public ResponseEntity<?> uploadProductImage(
                        @PathVariable Long productId,
                        @RequestParam("file") MultipartFile file) throws IOException {

                // STEP 1. Validate the file (Security First!)
                FileValidator.validateImage(file);

                // STEP 2: Upload to Cloudinary CDN.
                // CloudinaryService.upload() pushes the bytes to Cloudinary's servers
                // and returns back a permanent, public https:// URL like:
                // "https://res.cloudinary.com/your-cloud/image/upload/v123/products/file.jpg"
                // We pass "products" as the folder so images are organized in Cloudinary
                // dashboard.
                String imageUrl = cloudinaryService.upload(file, "products");

                // STEP 3: Fetch the product from DB.
                // We need the current product entity to append the new URL to its image list.
                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException("Product not found"));

                // STEP 4: Append the CDN URL to the product's image list.
                // IMPORTANT: We APPEND — we do NOT overwrite! A product can have many images.
                // The @ElementCollection on Product.imageUrls stores each URL as a row
                // in the product_images table. Adding to the list = inserting a new row.
                // Append new image URL to existing list (do NOT overwrite)
                List<String> existingUrls = product.getImageUrls() != null
                                ? new java.util.ArrayList<>(product.getImageUrls())
                                : new java.util.ArrayList<>();

                existingUrls.add(imageUrl);
                product.setImageUrls(existingUrls);

                // STEP 5: Persist the updated list.
                productRepository.save(product);

                // STEP 6: Return the CDN URL AND the publicId to the caller.
                // WHY RETURN publicId: The caller (frontend/Postman) needs the publicId
                // to later call DELETE /{productId}/images?publicId=... for cleanup.
                // We extract it here from the URL since the Cloudinary SDK result map
                // also provides it — no extra API call needed.
                String publicId = cloudinaryService.extractPublicId(imageUrl);

                return ResponseEntity.ok(Map.of(
                                "message", "Image uploaded successfully to Cloudinary CDN",
                                "imageUrl", imageUrl, // Full CDN URL — use in <img src="...">
                                "publicId", publicId // Store this! Pass it to DELETE endpoint to remove the image
                ));
        }

        // ─── DELETE A SPECIFIC PRODUCT IMAGE ───────────────────────────────────────
        // REST: DELETE
        // /api/v1/products/{productId}/images?publicId=products/usb-hub-abc123
        // The caller passes the Cloudinary publicId (received from the upload response
        // or
        // the Cloudinary Dashboard). We handle CDN deletion + DB cleanup here.
        @DeleteMapping("/{productId}/images")
        @PreAuthorize("hasRole('SELLER')")
        public ResponseEntity<?> deleteProductImage(
                        @PathVariable Long productId,
                        @RequestParam String publicId) { // e.g. "products/usb-hub-abc123"

                // STEP 1: Fetch the product entity.
                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException("Product not found"));

                // STEP 2: Find the matching CDN URL in the stored imageUrls list.
                // WHY: We store full URLs like
                // "https://res.cloudinary.com/.../products/usb-hub.jpg"
                // The publicId "products/usb-hub-abc123" is a SUBSTRING of that URL.
                // So we search for the URL that CONTAINS the publicId to identify which one to
                // remove.
                String matchingUrl = product.getImageUrls().stream()
                                .filter(url -> url.contains(publicId))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException(
                                                "No image found with publicId '" + publicId + "' for product "
                                                                + productId));

                // STEP 3: Delete from Cloudinary CDN first (safer order — see why below).
                // WHY FIRST: If DB delete succeeds but Cloudinary delete fails, we've lost
                // the URL forever and can never clean it up. Doing CDN first is the safer
                // order.
                cloudinaryService.delete(publicId);

                // STEP 4: Remove the matching URL from the product's imageUrls list.
                // @ElementCollection means this list maps to rows in product_images table.
                // Removing from the list + saving = DELETE FROM product_images WHERE image_url
                // = ?
                List<String> updatedUrls = new java.util.ArrayList<>(product.getImageUrls());
                updatedUrls.remove(matchingUrl);
                product.setImageUrls(updatedUrls);
                productRepository.save(product);

                return ResponseEntity.ok(Map.of(
                                "message", "Image deleted successfully",
                                "deletedPublicId", publicId));
        }

        /**
         * PHASE 4 — AI Semantic Vector Search
         * GET /api/v1/products/search/semantic?q=your query&limit=10
         *
         * Unlike keyword search (LIKE '%word%'), this finds products by MEANING.
         * "earphones for studying" → finds "Noise Cancelling Headphones" even with no matching words.
         *
         * Flow: query text → HuggingFace float[384] vector → pgvector cosine similarity → top N results
         */
        @Operation(
                summary = "🤖 Semantic AI Search",
                description = "Find products by meaning using HuggingFace AI + pgvector. " +
                        "Example: 'earphones for noisy cafe' finds noise-cancelling headphones."
        )
        @GetMapping("/search/semantic")
        public ResponseEntity<List<ProductResponse>> semanticSearch(
                        @RequestParam String q,
                        @RequestParam(defaultValue = "10") int limit) {

                // Step 1: Convert the user's plain-English query into a 384-dim vector
                // using the same HuggingFace model used when products were indexed
                float[] queryVector = embeddingService.generateEmbedding(q);

                // Step 2: Format float[] → "[0.021,-0.455,...]" for the native SQL CAST
                String vectorString = new VectorAttributeConverter()
                                .convertToDatabaseColumn(queryVector);

                // Step 3: PostgreSQL cosine distance (<=> operator) finds closest product vectors
                List<Product> results = productRepository.findBySimilarity(vectorString, limit);

                // Step 4: Map entities → response DTOs
                List<ProductResponse> response = results.stream()
                                .map(productMapper::toProductResponse)
                                .collect(java.util.stream.Collectors.toList());

                return ResponseEntity.ok(response);
        }

        // ─── PRODUCT RECOMMENDATIONS ───────────────────────────────────────
        @Operation(summary = "Get Recommendations", description =
                "Return frequently bought together product")
        @GetMapping("/{productId}/recommendations")
        public ResponseEntity<List<ProductResponse>>getRecommendations(@PathVariable Long productId,
                                                   @RequestParam(defaultValue = "5") int limit){
                List<Product>recommendations = productRepository
                        .findFrequentlyBoughtTogether(productId, limit);

                List<ProductResponse> responses = recommendations.stream()
                        .map(productMapper::toProductResponse)
                        .toList();

                return ResponseEntity.ok(responses);
        }

}

