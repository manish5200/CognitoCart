package com.manish.smartcart.model.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manish.smartcart.model.base.BaseEntity;
import com.manish.smartcart.model.feedback.Review;
import com.manish.smartcart.util.AppConstants;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="products")
@SoftDelete(columnName = "is_deleted") // <--- That's it! No @SQLDelete or @SQLRestriction needed.
public class Product extends BaseEntity{

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String productName;

    @NotBlank
    @Column(unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    private BigDecimal price;

    @NotBlank
    @Column(unique = true)
    private String sku; // Stock Keeping Unit

    @Min(0)
    private Integer stockQuantity;

    private Boolean isAvailable = true; // Default to true so new products show up immediately

    // --- Discovery & Social Proof ---
    @ElementCollection
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>(); // Smart: For "Similar Products" search

    private Double averageRating = AppConstants.INITIAL_RATING; // Denormalized for performance
    private Integer totalReviews = AppConstants.INITIAL_REVIEW_COUNT;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId; // Loose coupling for microservices

    // This prevents "HttpMessageNotWritableException" when returning a Product in JSON.
    // It stops Jackson from trying to load Hibernate's internal proxy fields
    // after the database session is closed.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id",
    foreignKey = @ForeignKey(name = "fk_product_category"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Category category;

    // Inside your Product class
    @Transient
    private Long categoryId; // Used only for mapping the incoming JSON ID

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    //Hibernate will automatically create a secondary table called product_images.
    //It will have two columns: product_id and image_url.
    @ElementCollection
    @CollectionTable(
            name = "product_images",
            joinColumns = @JoinColumn(name = "product_id")
    )
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();

    // Smart: Helper to update ratings when a new review is added
    public void addReview(Review review) {
        this.reviews.add(review);
        this.totalReviews = reviews.size();
        // Logic to update averageRating would go in the Service layer
    }

}
