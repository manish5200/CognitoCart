package com.manish.smartcart.repository;

import com.manish.smartcart.model.feedback.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review,Long> {

    // Fetch all reviews for a specific product
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);


    // Check if a user has already reviewed this product
    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

}
