package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.feedback.ReviewRequestDTO;
import com.manish.smartcart.dto.feedback.ReviewResponseDTO;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@Tag(name = "Feedback & Reviews", description = "Product rating and review system with verified purchase enforcement")
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;


    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC ENDPOINT: No login required to read reviews
    // ─────────────────────────────────────────────────────────────────────────
    @Operation(
            summary = "Get reviews for a product",
            description = "Returns all reviews for a product, sorted by newest first. No authentication required."
    )
    @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully")
    @GetMapping("/{productId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsForProduct(@PathVariable Long productId) {
        // CONCEPT: Product reviews are public data. Anyone browsing the store
        // should see ratings without needing to log in.
        return ResponseEntity.ok(reviewService.getReviewsForProduct(productId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROTECTED ENDPOINT: Only verified customers who bought the product
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Submit or update a review",
            description = "CUSTOMER only. Enforces a verified-purchase check — " +
                    "you can only review a product you have received (DELIVERED status). " +
                    "Supports both create and update (upsert logic)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review submitted or updated successfully"),
            @ApiResponse(responseCode = "400", description = "User has not purchased this product"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{productId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> postReview(
            @PathVariable Long productId,
            @RequestBody ReviewRequestDTO reviewRequestDTO,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        return ResponseEntity
                .ok(reviewService.addOrUpdateReview(
                        userId,
                        productId,
                        reviewRequestDTO));
    }


    private Long extractUserId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (userDetails == null) {
            throw new BusinessLogicException("Authentication context is missing. Please log in again.");
        }
        return userDetails.getUser().getId();
    }
}
