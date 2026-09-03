package com.manish.smartcart.product.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.manish.smartcart.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Represents an individual media asset (Image/Video) attached to a Product.
 * Designed to enforce a single primary image per product and preserve exact sort ordering.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "product_media", uniqueConstraints = {
        // Edge Case Protection: A product cannot have two images with the exact same sort order
        @UniqueConstraint(name = "uk_product_media_sort", columnNames = {"product_id", "sort_order"})
})
@SequenceGenerator(name = "entity_seq", sequenceName = "product_media_seq", allocationSize = 50)
public class ProductMedia extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonBackReference
    private Product product;

    @Column(name = "media_url", nullable = false, length = 1000)
    private String mediaUrl;

    @Column(name = "media_type", nullable = false, length = 20)
    @Builder.Default
    private String mediaType = "IMAGE"; // IMAGE or VIDEO

    // The database index (idx_primary_media) guarantees only ONE true value per product_id.
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean isPrimary = false;

    // Zero-based index (0, 1, 2) for strict frontend rendering sequence
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    // Critical for Accessibility (a11y) and SEO parsing
    @Column(name = "alt_text", length = 500)
    private String altText;
}
