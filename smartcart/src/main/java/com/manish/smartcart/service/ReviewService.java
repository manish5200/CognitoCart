package com.manish.smartcart.service;

import com.manish.smartcart.dto.feedback.RatingDistributionDTO;
import com.manish.smartcart.dto.feedback.ReviewRequestDTO;
import com.manish.smartcart.dto.feedback.ReviewResponseDTO;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.exception.ResourceNotFoundException;
import com.manish.smartcart.mapper.ReviewMapper;
import com.manish.smartcart.model.feedback.Review;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.repository.ReviewRepository;
import com.manish.smartcart.repository.UsersRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UsersRepository usersRepository;
    private final ReviewMapper reviewMapper;


    // ─────────────────────────────────────────────────────────────────────────
    // SUBMIT / UPDATE REVIEW (Upsert with Verified Purchase enforcement)
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Recovery handler for the @Retryable below.
     * If all 3 optimistic-lock retries fail (database is under extreme load),
     * this method is called instead of letting a raw exception bubble up.

     * CONCEPT: @Recover must have the same return type as the @Retryable method.
     * Spring Retry calls it automatically as a last-resort fallback.
     */
    @Recover
    public Map<String,Object>recover(
            ObjectOptimisticLockingFailureException e,
            Long userId,
            Long productId,
            ReviewRequestDTO reviewRequestDTO){
        // Log the failure so it shows in our monitoring dashboards
        log.error("All 3 retry attempts exhausted for review submission. userId={}, productId={}, cause={}",
                userId, productId, e.getMessage());
        // Throw a proper typed exception — GlobalExceptionHandler will return HTTP 503
        throw new BusinessLogicException(
                "The system is under high load. Your review could not be saved — please try again in a moment."
        );
    }


    /**
     * Creates a new review or updates an existing one (upsert pattern).

     * KEY CONCEPT — @Retryable:
     * When two users try to save a review for the same product simultaneously,
     * Hibernate's optimistic locking may throw ObjectOptimisticLockingFailureException.
     * Instead of crashing, we automatically retry up to 3 times with a 500ms pause.
     * This is a production-grade pattern used in high-traffic e-commerce systems.
     */
    @Transactional
    @Retryable(
            retryFor = {ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay =  500)// Wait 500ms before trying again
    )
    public Map<String,Object> addOrUpdateReview(Long userId, Long productId, ReviewRequestDTO reviewRequestDTO) {

        // 1. Confirm the product is real before doing anything else
        Product product = productRepository
                .findById(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Product not found with ID: " + productId));

        // 2. ━━━ 🛡️ VERIFIED PURCHASE CHECK GATE ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // This is the core business rule: you can only review something you received.
        // We check for both DELIVERED and RETURNED — a returned item was still received.
        boolean hasPurchased = orderRepository.existsByUserIdAndOrderItems_Product_IdAndOrderStatus(
                        userId, productId, OrderStatus.DELIVERED)
                || orderRepository.existsByUserIdAndOrderItems_Product_IdAndOrderStatus(
                        userId, productId, OrderStatus.RETURNED);

        if(!hasPurchased){
            throw new BusinessLogicException(
                    "You can only review products you have received. Complete your order first."
            );
        }

        // 3. THE SMART CHECK: Find existing review or create new
        //Upsert Logic
        Optional<Review>existingReview = reviewRepository.findByProductIdAndUserId(productId, userId);
        String statusMessage;
        Review finalReview;
        if(existingReview.isPresent()){
            // ── UPDATE PATH ─────────────────────────────────────────────
            Review review = existingReview.get();
            // Mathematically adjust the product's rolling average rating
            // SUBTRACT the old rating from the average before adding the new one
            updateProductRatingOnEdit(product, review.getRating(), reviewRequestDTO.getRating());
            review.setRating(reviewRequestDTO.getRating());
            review.setComment(reviewRequestDTO.getComment());
            finalReview = reviewRepository.save(review);
            statusMessage = "Your review has been updated successfully";
            log.info("Review updated for productId={} by userId={}", productId, userId);
        }else{
            // ── CREATE PATH ─────────────────────────────────────────────
            Review review = new Review();
            review.setProduct(product);

            // Link the user! (Using getReferenceById avoids an extra DB Select)
            Users user = usersRepository.getReferenceById(userId);
            review.setUser(user);

            review.setRating(reviewRequestDTO.getRating());
            review.setComment(reviewRequestDTO.getComment());
            finalReview = reviewRepository.save(review);
            updateProductRatingOnNew(product, reviewRequestDTO.getRating());
            statusMessage = "Your review has been submitted. Thank you!";
            log.info("New review created for productId={} by userId={}", productId, userId);
        }
        return Map.of(
                "message",statusMessage,
                "review",reviewMapper.toReviewResponseDTO(finalReview));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE REVIEW — Customer's own review only
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Deletes a review and mathematically corrects the product's average rating.

     * SECURITY: We query by BOTH reviewId AND userId in one database call.
     * If the review doesn't belong to this user, it's invisible to them — clean 404.
     * This prevents horizontal privilege escalation (user A deleting user B's review).

     * CONCEPT — "Authorization at the query layer":
     * Don't fetch first and check ownership in Java. Make the DB do it atomically.
     */
    @Transactional
    public void deleteMyReview(Long reviewId, Long userId) {

        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review not found or you do not have permission to delete it."
                ));
        Product product = review.getProduct();
        int oldTotalReview = product.getTotalReviews();

        if (oldTotalReview > 1) {
            double oldAverageRating = product.getAverageRating();
            double newAverage = ((oldAverageRating * oldTotalReview) - review.getRating()) / (oldTotalReview - 1);

            product.setAverageRating(Math.round(newAverage * 10.0) / 10.0);
            product.setTotalReviews(oldTotalReview - 1);

        } else {
            // This was the only review — reset the product to its pristine state
            product.setTotalReviews(0);
            product.setAverageRating(0.0);
        }
        productRepository.save(product);
        reviewRepository.delete(review);
        log.info("Review ID {} deleted by userId={}", reviewId, userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE REVIEW — Admin force-delete (for abusive/spam content)
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Admin-only hard delete. No ownership check — admin can remove any review.
     * Use case: handling reported abusive, spam, or fake reviews.
     * The product's average is still corrected to maintain data integrity.
     */
    public void adminDeleteReview(Long reviewId){
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId));

        Product product = review.getProduct();
        int oldTotalReview = product.getTotalReviews();

        if(oldTotalReview > 1){
            double newAverage = ((product.getAverageRating() * oldTotalReview) - review.getRating()) / (oldTotalReview - 1);
            product.setAverageRating(Math.round(newAverage * 10.0) / 10.0);
            product.setTotalReviews(oldTotalReview - 1);
        }else{
            product.setTotalReviews(0);
            product.setAverageRating(0.0);
        }

        productRepository.save(product);
        reviewRepository.delete(review);
        log.warn("Admin force-deleted reviewId={} for productId={}", reviewId, product.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RATING DISTRIBUTION — The star breakdown widget
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Returns the star-rating breakdown for a product (the "histogram" widget).

     * CONCEPT: One DB aggregate query instead of loading all reviews into memory.
     * PostgreSQL's GROUP BY is hundreds of times faster than Java stream counting.
     */
    @Transactional(readOnly = true)
    public RatingDistributionDTO getRatingDistribution(Long productId){
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        // Ask the database to count reviews per star level in ONE query
        List<Object[]>rawCounts = reviewRepository.countReviewsByRatingForProduct(productId);

        // Convert the raw DB result into a lookup map: { 5 → 90, 4 → 35, ... }
        Map<Integer,Long>countByRating = new HashMap<>();
        for(Object[] row : rawCounts){
            Integer star = (Integer)row[0];
            Long count = (Long)row[1];
            countByRating.put(star, count);
        }

        return RatingDistributionDTO.builder()
                .productId(productId)
                .productName(product.getProductName())
                .averageRating(product.getAverageRating())
                .totalReviews(product.getTotalReviews())
                .fiveStars(countByRating.getOrDefault(5, 0L))
                .fourStars(countByRating.getOrDefault(4, 0L))
                .threeStars(countByRating.getOrDefault(3, 0L))
                .twoStars(countByRating.getOrDefault(2, 0L))
                .oneStar(countByRating.getOrDefault(1, 0L))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET REVIEWS — Public listing for a product
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Returns all reviews for a product, newest first.
     * readOnly = true → tells the DB "no write locks needed" → faster query.
     */
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO>getReviewsForProduct(Long productId){
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(reviewMapper::toReviewResponseDTO)
                .toList();
    }


    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE MATH HELPERS — Rolling average update logic
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Recalculates the product's average when an existing review's star rating changes.
     * Formula: newAverage = (currentTotal - oldRating + newRating) / totalCount
     */
    private void updateProductRatingOnEdit(Product product, Integer oldRating, Integer newRating) {
        if (product.getTotalReviews() == 0) return; // Defensive check

        double currentTotalScore = product.getAverageRating()*product.getTotalReviews();
        double newAverageRating = (currentTotalScore + (double)newRating - (double)oldRating) / product.getTotalReviews();
        // Consistent rounding for the UI
        product.setAverageRating(Math.round(newAverageRating * 10.0) / 10.0);
        productRepository.save(product);
    }

    /**
     * Recalculates the product's average when a brand-new review is added.
     * Formula: newAverage = (currentTotal + newRating) / (totalReviews + 1)
     */
    private void updateProductRatingOnNew(Product product, Integer newRating) {
          int newTotalReview = product.getTotalReviews() + 1;
          double currentTotalScore = product.getAverageRating()*product.getTotalReviews();
          double newAverageRating = (currentTotalScore + newRating )/newTotalReview;

          product.setTotalReviews(newTotalReview);
        // Apply rounding here as well
         product.setAverageRating(Math.round(newAverageRating * 10.0) / 10.0);
         productRepository.save(product);
    }

}
