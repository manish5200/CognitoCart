package com.manish.smartcart.repository;

import com.manish.smartcart.model.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long> {

    List<Product> findByCategoryId(Long categoryId);

    Optional<Product> findBySlug(String slug);

    // FIX: Use 'category.id' instead of 'categoryId'
    @Query("SELECT p FROM Product p WHERE p.category.id IN :categoryIds")
    List<Product> findByCategoryIdIn(@Param("categoryIds") List<Long> categoryIds);
}
