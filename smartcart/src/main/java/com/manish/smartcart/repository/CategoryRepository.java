package com.manish.smartcart.repository;

import com.manish.smartcart.model.product.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Long> {

    Optional<Category> findBySlug(String slug);

    // Get all top-level categories for the navbar
    List<Category> findByParentCategoryIsNull();

    // Useful if you want to look up categories by name in your service
    Optional<Category> findByNameIgnoreCase(String name);

    List<Category> findByParentCategoryId(Long currentId);
}
