package com.manish.smartcart.repository;

import com.manish.smartcart.model.user.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
