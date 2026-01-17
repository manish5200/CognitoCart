package com.manish.smartcart.model.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manish.smartcart.model.base.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * The Category Entity (Recursive Tree)
 * This allows for infinite sublevels (Electronics > Audio > Headphones).
 **/

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "categories")
// Add this at the top of the class to skip any weird unknown JSON fields
@JsonIgnoreProperties(ignoreUnknown = true)
public class Category extends BaseEntity{

    @NotBlank
    private String name;

    @NotBlank
    @Column(unique = true)
    private String slug; // Smart: e.g., "men-footwear" // Smart: For SEO URLs (e.g., /category/laptops)

    // Prevents "no session" errors when serializing the parent-child relationship.
    // This allows the JSON to show the category details without crashing on
    // Hibernate's lazy-loading proxies.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Category parentCategory; //// The "Parent" node

    @JsonIgnore
    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL)
    private List<Category> subCategories = new ArrayList<>();
}
