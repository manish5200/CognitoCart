package com.manish.smartcart.product.controller;

import com.manish.smartcart.product.dto.InventoryAdjustmentRequest;
import com.manish.smartcart.product.dto.ProductVariantResponse;
import com.manish.smartcart.security.CustomUserDetails;
import com.manish.smartcart.product.dto.ProductVariantRequest;
import com.manish.smartcart.product.service.ProductVariantService;
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
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products/{productPublicId}/variants")
@Tag(name = "Product Variants", description = "Seller management for multi-SKU product sizes, colors, and stock")
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    @Operation(summary = "Get all variants for a product", description = "Public endpoint used by the catalog/product details page.")
    @GetMapping
    public ResponseEntity<?> getPublicVariants(@PathVariable UUID productPublicId) {
        return ResponseEntity.ok(productVariantService.getPublicVariants(productPublicId));
    }

    @Operation(summary = "Add a new variant", description = "Seller creates a new SKU (e.g., Size M) for an existing product.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> addVariant(
            @PathVariable UUID productPublicId,
            @Valid @RequestBody ProductVariantRequest request,
            Authentication authentication){
        Long sellerId = extractUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productVariantService.addProductVariant(productPublicId, request, sellerId));
    }

    @Operation(summary = "Update a variant", description = "Seller updates stock, price modifier, or attributes.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{variantPublicId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> updateVariant(
            @PathVariable UUID productPublicId,
            @PathVariable UUID variantPublicId,
            @Valid @RequestBody ProductVariantRequest request,
            Authentication authentication) {

        Long sellerId = extractUserId(authentication);
        return ResponseEntity.ok(productVariantService
                .updateProductVariant(productPublicId, variantPublicId, request, sellerId));
    }

    @Operation(summary = "Upload Variant Image", description = "Upload a specific swatch image to Cloudinary for this SKU.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "{variantPublicId}/upload-image", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> uploadVariantImage(
            @PathVariable UUID productPublicId,
            @PathVariable UUID variantPublicId,
            @RequestParam("file")MultipartFile file,
            Authentication authentication) {

        Long sellerId = extractUserId(authentication);
        String imageUrl = productVariantService.uploadVariantImage(productPublicId, variantPublicId, file, sellerId);

        return ResponseEntity.ok(Map.of(
                "message", "Variant image uploaded successfully",
                "imageUrl", imageUrl
        ));
    }

    @Operation(summary = "Toggle Variant Status", description = "Soft delete / deactivate a SKU.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{variantPublicId}/status")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> toggleVariantStatus(
            @PathVariable UUID productPublicId,
            @PathVariable UUID variantPublicId,
            Authentication authentication) {

        Long sellerId = extractUserId(authentication);
        productVariantService.toggleVariantStatus(productPublicId, variantPublicId, sellerId);

        return ResponseEntity.ok(Map.of("message", "Variant status toggled successfully."));
    }

    /**
     * Atomic inventory adjustment using a signed delta value.
     * <p>
     * ─── WHY PATCH? ──────────────────────────────────────────────────────────────
     * PATCH = partial update. We are not replacing the entire variant resource
     * (that's PUT), just adjusting one field (stock) by a delta amount.
     * Semantically correct by HTTP spec (RFC 5789).
     * <p>
     * ─── EXAMPLE REQUESTS ────────────────────────────────────────────────────────
     * Restock 100 units after supplier delivery:
     *   PATCH /api/v1/products/{pId}/variants/{vId}/stock
     *   { "adjustment": 100, "reason": "RESTOCK", "note": "Supplier XYZ delivery" }
     * <p>
     * Write off 3 damaged units:
     *   PATCH /api/v1/products/{pId}/variants/{vId}/stock
     *   { "adjustment": -3, "reason": "DAMAGE_WRITE_OFF", "note": "Water damage" }
     */
    @Operation(
            summary = "Adjust variant stock (delta)",
            description = "Atomically adjust stock by a signed integer. " +
                    "Positive = add stock. Negative = remove stock. " +
                    "Safe under concurrent checkout. Cannot go below zero."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{variantPublicId}/stock")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<?> adjustStock(
            @PathVariable UUID productPublicId,
            @PathVariable UUID variantPublicId,
            @Valid @RequestBody InventoryAdjustmentRequest request,
            Authentication authentication){

        Long sellerId = extractUserId(authentication);
        ProductVariantResponse updated = productVariantService.adjustStock(variantPublicId, request, sellerId);

        return ResponseEntity.ok(Map.of(
                "message",  "Stock adjusted successfully",
                "delta",     request.getAdjustment(),
                "reason",    request.getReason(),
                "variant",   updated
        ));
    }

    //Helper
    private Long extractUserId(Authentication authentication) {
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        return customUserDetails.getUser().getId();
    }
}
