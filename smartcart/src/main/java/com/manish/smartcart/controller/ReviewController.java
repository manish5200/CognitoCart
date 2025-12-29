package com.manish.smartcart.controller;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.dto.feedback.ReviewRequestDTO;
import com.manish.smartcart.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/{productId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> postReview(
            @PathVariable Long productId,
            @RequestBody ReviewRequestDTO  reviewRequestDTO,
            Authentication authentication) {
            CustomUserDetails  customUserDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = customUserDetails.getUserId();
            return ResponseEntity
                    .ok(reviewService.addOrUpdateReview(
                            userId,
                            productId,
                            reviewRequestDTO));
    }
}
