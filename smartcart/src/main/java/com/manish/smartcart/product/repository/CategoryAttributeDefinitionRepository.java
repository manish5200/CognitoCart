package com.manish.smartcart.product.repository;

import com.manish.smartcart.product.model.CategoryAttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Manages the dynamic structural schema for categories.
 * Crucial for powering the progressive 10-step Seller UI.
 */
@Repository
public interface CategoryAttributeDefinitionRepository extends JpaRepository<CategoryAttributeDefinition, Long> {
    /**
     * Fetches all required and optional attributes for a specific category.
     * Edge Case Guard: MUST be ordered by sortOrder ASC so the frontend form
     * doesn't shuffle the input fields randomly on every page load.
     */
    List<CategoryAttributeDefinition> findByCategoryIdOrderBySortOrderAsc(Long categoryId);
}
