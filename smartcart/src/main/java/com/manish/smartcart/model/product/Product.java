package com.manish.smartcart.model.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manish.smartcart.model.base.BaseEntity;
import com.manish.smartcart.model.feedback.Review;
import com.manish.smartcart.util.AppConstants;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SoftDelete;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The catalog representation of a product.
 * Domain Rule: Products are the marketing shell (metadata, branding, reviews).
 * All physical inventory and checkout logic delegates to the associated ProductVariants.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@SuperBuilder
@Table(name = "products")
@SoftDelete(columnName = "is_deleted")
public class Product extends BaseEntity {

    // ─── CATALOG IDENTITY ─────────────────────────────────────────────────────

    @NotBlank
    private String productName;

    // SEO-friendly URL identifier (e.g., "nike-air-max-90").
    @NotBlank
    @Column(unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Denormalized brand name for efficient catalog filtering.
    @Column(length = 100)
    private String brand;

    // ─── PRICING ──────────────────────────────────────────────────────────────

    // Master base price. Variant-specific modifiers are applied against this value.
    @NotNull
    private BigDecimal price;

    // Active sale override. If not null, UI reflects a discount (e.g., Strike-through pricing).
    private BigDecimal discountPrice;

    // ─── DISCOVERY & SOCIAL PROOF ─────────────────────────────────────────────

    // Faceted search tags (e.g., "wireless", "waterproof").
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    // Denormalized aggregates to avoid expensive runtime calculations on catalog load.
    @Builder.Default
    private Double averageRating = AppConstants.INITIAL_RATING;

    @Builder.Default
    private Integer totalReviews = AppConstants.INITIAL_REVIEW_COUNT;

    @Builder.Default
    private Integer totalSold = 0;

    // Controls homepage or category-level promotional placement.
    @Column(nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    // Master toggle. Deactivating hides the product and all associated variants.
    @Builder.Default
    private Boolean isAvailable = true;

    // ─── RELATIONSHIPS ────────────────────────────────────────────────────────

    // Microservice-ready loose coupling to the Seller domain.
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_product_category"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "parentCategory", "subCategories"})
    private Category category;

    // Transient DTO field for simplified JSON payload mapping during creation/updates.
    @JsonIgnore
    @Transient
    private Long categoryId;

    // Enforces strict display ordering for variants (e.g., S, M, L).
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    @JsonIgnore
    private List<ProductVariant> variants = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @JsonIgnore
    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL)
    private ProductInsights insights;

    // Master image gallery. Shared across all variants unless overridden by a variant swatch.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    /**
     * Synchronizes denormalized review counts when a new review is attached.
     */
    public void addReview(Review review) {
        this.reviews.add(review);
        this.totalReviews = reviews.size();
    }
}