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
import java.util.Map;

@RequiredArgsConstructor
@RestController
@Tag(name = "Feedback & Reviews", description = "Verified purchase review system with rating distribution analytics")
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;


    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC ENDPOINT: No login required to read reviews
    // ─────────────────────────────────────────────────────────────────────────
    @Operation(
            summary = "Get reviews for a product",
            description = "Returns reviews sorted by newest first. Each review includes the reviewer's " +
                    "abbreviated name, star rating, comment, date, and verified purchase badge. " +
                    "No authentication required."
    )
    @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully")
    @GetMapping("/{productId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsForProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsForProduct(productId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Star rating endpoint
    // ─────────────────────────────────────────────────────────────────────────
    @Operation(
            summary = "Get star rating breakdown for a product",
            description = "Returns the count of 1★ through 5★ reviews using a single database aggregate query. " +
                    "Suitable for rendering a rating histogram widget. No authentication required."
    )
    @ApiResponse(responseCode = "200", description = "Rating distribution retrieved successfully")
    @GetMapping("/{productId}/distribution")
    public ResponseEntity<?> getRatingDistribution(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getRatingDistribution(productId));
    }


    // ─── CUSTOMER ENDPOINTS — Requires CUSTOMER role ─────────────────────────

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

    //Deletion Endpoint
    @Operation(
            summary = "Delete your own review",
            description = "CUSTOMER only. Permanently removes your review. " +
                    "The product's average rating is mathematically corrected immediately. " +
                    "You can only delete your own review — attempting to delete another user's review returns 404."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Review not found or does not belong to you")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> deleteMyReview(
            @PathVariable Long reviewId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        reviewService.deleteMyReview(reviewId, userId);
        return ResponseEntity.ok(Map.of("message", "Your review has been removed successfully."));
    }


    // ─── ADMIN ENDPOINTS — Requires ADMIN role ────────────────────────────────
    @Operation(
            summary = "Admin: Force-delete any review",
            description = "ADMIN only. Removes abusive, spam, or fake reviews from the platform. " +
                    "Product rating is automatically corrected after deletion."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review removed by admin"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/admin/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminDeleteReview(@PathVariable Long reviewId) {
        reviewService.adminDeleteReview(reviewId);
        return ResponseEntity.ok(Map.of("message", "Review has been removed by admin."));
    }


    private Long extractUserId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (userDetails == null) {
            throw new BusinessLogicException("Authentication context is missing. Please log in again.");
        }
        return userDetails.getUser().getId();
    }
}
