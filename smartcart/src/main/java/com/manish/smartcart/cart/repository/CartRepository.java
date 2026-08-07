package com.manish.smartcart.cart.repository;

import com.manish.smartcart.cart.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // JOIN FETCH items, variants, and products so Cart.items is fully initialized
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items ci LEFT JOIN FETCH ci.variant v LEFT JOIN FETCH v.product WHERE c.user.id = :userId")
    Optional<Cart> findByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM Cart c JOIN FETCH c.user WHERE c.updatedAt < :thresholdDate AND SIZE(c.items) > 0")
    List<Cart> findAbandonedCarts(@Param("thresholdDate") java.time.LocalDateTime thresholdDate);

    // PUBLIC ID LOOKUP
    Optional<Cart> findByPublicId(UUID publicId);
}
