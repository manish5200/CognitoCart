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

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId ORDER BY r.createdAt DESC")
    List<Review> findByProductIdOrderByCreatedAtDesc(@Param("productId") Long productId);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.user.id = :userId")
    Optional<Review> findByProductIdAndUserId(@Param("productId") Long productId, @Param("userId") Long userId);
}
