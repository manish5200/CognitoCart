package com.manish.smartcart.product.model;

import com.manish.smartcart.shared.enums.product.CategoryAttributeScope;
import com.manish.smartcart.shared.enums.product.CategoryAttributeType;
import com.manish.smartcart.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * Defines the strict schema for a category.
 * E.g., Laptops MUST have "RAM" (attribute_key) of type SELECT, scoped to VARIANT.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "category_attribute_definitions", uniqueConstraints = {
        // Edge Case Protection: A category cannot define the exact same attribute key twice
        @UniqueConstraint(name = "uk_category_attribute", columnNames = {"category_id", "attribute_key"})
})
@SequenceGenerator(name = "entity_seq", sequenceName = "category_attribute_def_seq", allocationSize = 50)
public class CategoryAttributeDefinition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // The strict programmatic key used in the JSONB payload (e.g. "screen_size")
    @Column(name = "attribute_key", nullable = false, length = 100)
    private String attributeKey;

    // The human-readable label for the Seller UI (e.g. "Screen Size")
    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 30)
    private CategoryAttributeType dataType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryAttributeScope scope;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private boolean isRequired = false;

    @Column(name = "is_filterable", nullable = false)
    @Builder.Default
    private boolean isFilterable = false;

    @Column(name = "is_searchable", nullable = false)
    @Builder.Default
    private boolean isSearchable = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    // Pre-defined dropdown options (e.g., ["4GB", "8GB", "16GB"]) for SELECT types.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> options;
}
