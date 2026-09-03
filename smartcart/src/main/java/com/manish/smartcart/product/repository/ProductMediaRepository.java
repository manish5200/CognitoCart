package com.manish.smartcart.product.repository;

import com.manish.smartcart.product.model.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Manages individual media assets.
 * Note: We don't strictly need a findByProductId here because we fetch media
 * via the Product entity's @OneToMany relationship (which is already ordered by sortOrder).
 * This repository is provided for administrative bulk operations or direct media deletion.
 */
@Repository
public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {
}
