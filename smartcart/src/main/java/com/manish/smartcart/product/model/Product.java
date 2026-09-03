package com.manish.smartcart.product.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manish.smartcart.product.model.embeddable.ProductSEO;
import com.manish.smartcart.product.model.embeddable.ProductWarranty;
import com.manish.smartcart.shared.enums.product.Condition;
import com.manish.smartcart.shared.enums.product.LifecycleStatus;
import com.manish.smartcart.shared.enums.product.ProductApprovalStatus;
import com.manish.smartcart.shared.enums.product.ProductType;
import com.manish.smartcart.shared.model.BaseEntity;
import com.manish.smartcart.review.model.Review;
import com.manish.smartcart.shared.util.AppConstants;
import com.manish.smartcart.shared.util.HumanIdGenerator;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.*;

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
@SequenceGenerator(name = "entity_seq", sequenceName = "product_seq",   allocationSize = 50)
// ✅Appending ID to the slug upon deletion to free up the unique constraint
@SQLDelete(sql = "UPDATE products SET is_deleted = true, slug = slug || '-deleted-' || id WHERE id=?")
@SQLRestriction("is_deleted = false")
public class Product extends BaseEntity {

    // ─── CATALOG IDENTITY ─────────────────────────────────────────────────────
    @Column(name = "product_code", unique = true, nullable = false)
    private String productCode;

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

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    // ─── RELATIONSHIPS ────────────────────────────────────────────────────────

    // Microservice-ready loose coupling to the Seller domain.
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @ManyToOne(fetch = FetchType.LAZY)
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
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC") // Ensures Hibernate always loads them in UI-ready order
    @Builder.Default
    private List<ProductMedia> mediaGallery = new ArrayList<>();

    // ─── CORE DOMAIN FIELDS ───────────────────────────────────────────────────
    @Column(name = "country_of_origin", length = 2)
    private String countryOfOrigin; // ISO 3166-1 alpha-2 (e.g., "IN", "US")

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Condition condition;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", length = 30)
    private ProductType productType;

    // ─── STATE MACHINE ────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 30)
    @Builder.Default
    private LifecycleStatus lifecycleStatus = LifecycleStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    @Builder.Default
    private ProductApprovalStatus approvalStatus = ProductApprovalStatus.NOT_REQUIRED;

    // ─── JSONB & EMBEDDABLE ──────────────────────────────────────────────────
    // Stores dynamic product-scoped attributes (e.g., Brand, Material) without altering DB schema
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> attributes = new LinkedHashMap<>();

    @Embedded
    private ProductWarranty warranty;

    @Embedded
    private ProductSEO seo;

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    /**
     * Synchronizes denormalized review counts when a new review is attached.
     */
    public void addReview(Review review) {
        this.reviews.add(review);
        this.totalReviews = reviews.size();
    }

    @PrePersist
    private void humanIDGenerator(){
        if(this.productCode == null){
            this.productCode = HumanIdGenerator.generate("PRD");
        }
    }

    /**
     * Helper to get the primary image URL since we migrated to the ProductMedia gallery.
     */
    @Transient
    @JsonIgnore
    public String getPrimaryImageUrl() {
        if (mediaGallery == null || mediaGallery.isEmpty()) {
            return null;
        }
        return mediaGallery.stream()
                .filter(ProductMedia::isPrimary)
                .findFirst()
                .map(ProductMedia::getMediaUrl)
                .orElse(mediaGallery.get(0).getMediaUrl());
    }
}