package com.manish.smartcart.cart.repository;

import com.manish.smartcart.cart.model.GuestCart;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Notice we extend CrudRepository instead of JpaRepository,
 * because Redis is a NoSQL Key-Value store, not SQL!
 */
@Repository
public interface GuestCartRepository extends CrudRepository<GuestCart, String> {
}
