package com.manish.smartcart.product.service;

import com.manish.smartcart.product.dto.CategoryDTO;
import com.manish.smartcart.product.model.Category;
import com.manish.smartcart.product.repository.CategoryRepository;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;


    // ─────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDTO createCategory(Category category) {
        // Auto-generate slug from name — no slug needed in request JSON
        category.setSlug(generateUniqueSlug(category.getName()));

        // BUG FIX #1: Was resolving parent by internal `id`, but frontend sends `publicId`.
        // Now consistently resolves parent by publicId across create AND update.
        if (category.getParentCategory() != null && category.getParentCategory().getPublicId() != null) {
            Category parent = categoryRepository
                    .findByPublicId(category.getParentCategory().getPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Parent category not found: " + category.getParentCategory().getPublicId()));

            // Prevent self-parenting at creation (edge case: client sends own publicId)
            if(parent.getPublicId().equals(category.getPublicId())){
                throw new IllegalArgumentException("A category cannot be its own parent.");
            }
            category.setParentCategory(parent);
        }
        return CategoryDTO.from(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public List<CategoryDTO> createCategoriesBulk(List<Category> categories) {
        return categories.stream()
                .map(this::createCategory)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────
    /**
     * Returns ONLY root categories (no parent), each with their full
     * nested subtree already populated. Frontend gets a ready-to-render tree.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'tree'")
    public List<CategoryDTO> getCategoryTree() {
        return categoryRepository.findByParentCategoryIsNull()
                .stream()
                .map(CategoryDTO::from)
                .collect(Collectors.toList());
    }


    /** Flat list — for internal use (product filtering, etc.) */
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'all'")
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }


    @Transactional(readOnly = true)
    public List<Long> getAllChildCategoryIds(Long parentId) {
        return getAllChildCategoryIdsRecursive(parentId, new HashSet<>());
    }

    public List<Long> getAllChildCategoryIdsRecursive(Long currentId, Set<Long> visited) {
        List<Long> allIds = new ArrayList<>();
        if (visited.contains(currentId)) return allIds; // cycle guard
        visited.add(currentId);
        allIds.add(currentId);
        categoryRepository.findByParentCategoryId(currentId)
                .forEach(child -> allIds.addAll(getAllChildCategoryIdsRecursive(child.getId(), visited)));
        return allIds;
    }


    @Transactional(readOnly = true)
    public Long getCategoryIdByPublicId(UUID categoryPublicId) {
        return categoryRepository.findByPublicId(categoryPublicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with public ID: " + categoryPublicId))
                .getId();
    }


    // ─────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDTO updateCategory(UUID publicId, Category request) {
        Category category = categoryRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + publicId));

        // BUG FIX #2: Old code set name BEFORE comparing — slug was never updated.
        // Save old name first, then compare, then update.
        String oldName = category.getName();
        category.setName(request.getName());

        if(!oldName.equals(request.getName())){
            category.setSlug(generateUniqueSlug(request.getName()));
        }

        // Parent update with full cycle detection
        if (request.getParentCategory() != null &&
                request.getParentCategory().getPublicId() != null) {

            Category parent = categoryRepository
                    .findByPublicId(request.getParentCategory().getPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

            // BUG FIX #3: Full ancestor chain check — prevents A→B→A circular loops.
            // Old code only checked direct self-reference.
            if(isAncestor(category.getId(), parent.getId())){
                throw new IllegalArgumentException(
                        "Circular reference detected: setting this parent would create an infinite loop.");
            }
            category.setParentCategory(parent);
        } else {
            // Explicitly setting null promotes category to root
            category.setParentCategory(null);
        }

        return CategoryDTO.from(categoryRepository.save(category));
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(UUID publicId) {

        Category category = categoryRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + publicId));

        /*
        BUG FIX #4: Old code only soft-deleted the parent.
        Children became orphaned ghost entries. Now recursively deletes entire subtree.
        */

        deleteCategorySubtree(category);
    }

    /** Recursively soft-deletes a category and ALL its descendants. */
    private void deleteCategorySubtree(Category category) {
        // Delete children first (depth-first)
        List<Category> children = categoryRepository.findByParentCategoryId(category.getId());
        for (Category child : children) {
            deleteCategorySubtree(child);
        }
        // @SQLDelete triggers soft-delete (sets is_deleted = true)
        categoryRepository.delete(category);
    }


    // ─────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────
    /**
     * Checks if `ancestorId` is anywhere in the ancestor chain of `categoryId`.
     * Prevents circular parent assignments like A → B → C → A.
     */
    private boolean isAncestor(Long categoryId, Long ancestorId) {
        Set<Long> visited = new HashSet<>();
        Long current = ancestorId;
        while (current != null) {
            if (visited.contains(current)) return false; // safety: malformed existing cycle
            if (current.equals(categoryId)) return true;  // found the cycle
            visited.add(current);
            Category parent = categoryRepository.findById(current).orElse(null);
            current = (parent != null && parent.getParentCategory() != null)
                    ? parent.getParentCategory().getId()
                    : null;
        }
        return false;
    }

    /**
     * Generates a URL-safe unique slug from a name.
     * "Men's Clothing" → "mens-clothing"
     * Handles duplicates: "electronics", "electronics-2", "electronics-3"
     */
    private String generateUniqueSlug(String name) {
        String baseSlug = name.toLowerCase()
                .replaceAll("'", "") // remove apostrophes
                .replaceAll("[^a-z0-9]+", "-") // non-alphanum → hyphen
                .replaceAll("^-|-$", ""); // trim leading/trailing hyphens

        String slug = baseSlug;
        int counter = 2;
        while (categoryRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }
}
