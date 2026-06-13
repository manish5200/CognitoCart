package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.product.ProductVariantRequest;
import com.manish.smartcart.service.ProductVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products/{productId}/variants")
@Tag(name = "Product Variants", description = "Seller management for multi-SKU product sizes, colors, and stock")
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    @Operation(summary = "Get all variants for a product", description = "Public endpoint used by the catalog/product details page.")
    @GetMapping
    public ResponseEntity<?> getPublicVariants(@PathVariable Long productId) {
        return ResponseEntity.ok(productVariantService.getPublicVariants(productId));
    }

    @Operation(summary = "Add a new variant", description = "Seller creates a new SKU (e.g., Size M) for an existing product.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> addVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request,
            Authentication authentication){
        Long sellerId = extractUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productVariantService.addProductVariant(productId, request, sellerId));
    }

    @Operation(summary = "Update a variant", description = "Seller updates stock, price modifier, or attributes.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{variantId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request,
            Authentication authentication) {

        Long sellerId = extractUserId(authentication);
        return ResponseEntity.ok(productVariantService.updateProductVariant(productId, variantId, request, sellerId));
    }

    @Operation(summary = "Upload Variant Image", description = "Upload a specific swatch image to Cloudinary for this SKU.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "{variantId}/upload-image", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> uploadVariantImage(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @RequestParam("file")MultipartFile file,
            Authentication authentication) {

        Long sellerId = extractUserId(authentication);
        String imageUrl = productVariantService.uploadVariantImage(productId, variantId, file, sellerId);

        return ResponseEntity.ok(Map.of(
                "message", "Variant image uploaded successfully",
                "imageUrl", imageUrl
        ));
    }

    @Operation(summary = "Toggle Variant Status", description = "Soft delete / deactivate a SKU.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{variantId}/status")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> toggleVariantStatus(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            Authentication authentication) {

        Long sellerId = extractUserId(authentication);
        productVariantService.toggleVariantStatus(productId, variantId, sellerId);

        return ResponseEntity.ok(Map.of("message", "Variant status toggled successfully."));
    }

    //Helper
    private Long extractUserId(Authentication authentication) {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        return customUserDetails.getUser().getId();
    }
}
