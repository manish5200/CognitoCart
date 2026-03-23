package com.manish.smartcart.repository;

import com.manish.smartcart.model.user.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

       @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId")
       List<Wishlist> findByUserId(@Param("userId") Long userId);

       @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId AND w.product.id = :productId")
       Optional<Wishlist> findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

       @Modifying
       @Query("DELETE FROM Wishlist w WHERE w.user.id = :userId AND w.product.id = :productId")
       void deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

       /*
        * Rule 1: discountPrice must exist.
        * Rule 2: discountPrice must be strictly cheaper than the base price.
        * Rule 3: Anti-Spam (They have never been emailed OR the last email was more than 14 days ago)
        */
       @Query("SELECT w FROM Wishlist w WHERE w.product.discountPrice IS NOT NULL " +
               "AND w.product.discountPrice < w.product.price " +
               "AND (w.lastPriceDropNotifiedAt IS NULL OR w.lastPriceDropNotifiedAt < :cooldownCutoff)")
       List<Wishlist> findEligibleSalesForNotification(@Param("cooldownCutoff") LocalDateTime cooldownCutoff);

}
