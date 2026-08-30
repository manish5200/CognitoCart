package com.manish.smartcart.product.dto;

import com.manish.smartcart.product.model.Category;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Safe tree-serializable DTO for Category API responses.
 * Avoids circular JSON serialization caused by bidirectional JPA relationships.
 * Recursively maps subCategories for a full nested tree.
 */
@Setter
@Getter
public class CategoryDTO {

    private UUID publicId;
    private String name;
    private String slug;
    private UUID parentPublicId;             // null for root categories
    private List<CategoryDTO> subCategories; // nested children

    /**
     * Maps a Category entity to a DTO, recursively building the full tree.
     * Only includes non-deleted children (SQLRestriction handles this at DB level).
     */
    public static CategoryDTO from(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setPublicId(category.getPublicId());
        dto.setName(category.getName());
        dto.setSlug(category.getSlug());

        // Set parent reference (publicId only — no infinite nesting)
        if (category.getParentCategory() != null) {
            dto.setParentPublicId(category.getParentCategory().getPublicId());
        }

        // Recursively map children
        if(category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            dto.setSubCategories(
                    category.getSubCategories().stream()
                            .map(CategoryDTO::from)
                            .collect(Collectors.toList())
            );
        }else{
            dto.setSubCategories(List.of());
        }
        return dto;
    }
}
