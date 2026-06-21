package com.manish.smartcart.product.repository;

import com.manish.smartcart.product.model.ProductInsights;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductInsightsRepository extends JpaRepository<ProductInsights, Long> {

    Optional<ProductInsights>findByProductId(Long productId);
}
