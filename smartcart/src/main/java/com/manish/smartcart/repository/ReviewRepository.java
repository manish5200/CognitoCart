package com.manish.smartcart.repository;

import com.manish.smartcart.model.feedback.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.product.id = :productId ORDER BY r.createdAt DESC")
    List<Review> findByProductIdOrderByCreatedAtDesc(@Param("productId") Long productId);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.user.id = :userId")
    Optional<Review> findByProductIdAndUserId(@Param("productId") Long productId, @Param("userId") Long userId);


    /**
     * Computes the star-rating breakdown for a product using a single DB-level aggregate query.
     * Returns Object[] rows where:
     *   result[0] = the star value (Integer: 1, 2, 3, 4, or 5)
     *   result[1] = count of reviews at that star level (Long)
     */

    @Query("select r.rating, count(r) from Review r where r.product.id = :productId group by r.rating")
    List<Object[]>countReviewsByRatingForProduct(@Param("productId") Long productId);


    /**
     * Ownership check query: returns the review ONLY if it belongs to the given user.
     * Used before any delete or edit operation to prevent users from tampering with other people's reviews.
     * SECURITY CONCEPT: "Authorization at the data layer."
     * If the review doesn't exist OR doesn't belong to the user, Optional.empty() is returned.
     */
    @Query("select r from Review r where r.id = :reviewId and r.user.id = :userId")
    Optional<Review>findByIdAndUserId(
            @Param("reviewId") Long reviewId,
            @Param("userId") Long userId);


}
