package com.manish.smartcart.service;

import com.manish.smartcart.model.product.Category;
import com.manish.smartcart.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Category createCategory(Category category) {
        // Auto-generate slug from name — no slug needed in request JSON
        category.setSlug(generateUniqueSlug(category.getName()));

        if (category.getParentCategory() != null && category.getParentCategory().getId() != null) {
            Category parent = categoryRepository.findById(category.getParentCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Parent Category Not Found"));
            category.setParentCategory(parent);
        }
        return categoryRepository.save(category);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public List<Category> createCategoriesBulk(List<Category> categories) {
        List<Category> savedCategories = new ArrayList<>();
        for (Category category : categories) {
            // Auto-generate slug from name
            category.setSlug(generateUniqueSlug(category.getName()));

            // Resolve parent if provided
            if (category.getParentCategory() != null && category.getParentCategory().getId() != null) {
                Category parent = categoryRepository.findById(category.getParentCategory().getId())
                        .orElseThrow(() -> new RuntimeException(
                                "Parent ID " + category.getParentCategory().getId() + " not found"));
                category.setParentCategory(parent);
            }
            savedCategories.add(categoryRepository.save(category));
        }
        return savedCategories;
    }

    /*
     * Converts category name to a URL-safe, unique slug.
     * "Men's Clothing" -> "mens-clothing"
     * "Home & Kitchen" -> "home-kitchen"
     * Duplicate handling: "electronics", "electronics-2", "electronics-3" ...
     */
    private String generateUniqueSlug(String name) {
        String baseSlug = name.toLowerCase()
                .replaceAll("'", "") // remove apostrophes
                .replaceAll("[^a-z0-9]+", "-") // non-alphanum → hyphen
                .replaceAll("^-|-$", ""); // trim leading/trailing hyphens

        String slug = baseSlug;
        int counter = 2;
        while (categoryRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        return slug;
    }

    @Cacheable(value = "categories", key = "'all'")
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<Long> getAllChildCategoryIds(Long parentId) {
        return getAllChildCategoryIdsRecursive(parentId, new HashSet<>());
    }

    public List<Long> getAllChildCategoryIdsRecursive(Long currentId, Set<Long> visited) {
        List<Long> allIds = new ArrayList<>();
        if (visited.contains(currentId))
            return allIds;
        visited.add(currentId);
        allIds.add(currentId);
        List<Category> children = categoryRepository.findByParentCategoryId(currentId);
        for (Category child : children) {
            allIds.addAll(getAllChildCategoryIdsRecursive(child.getId(), visited));
        }
        return allIds;
    }
}
