package com.manish.smartcart.model.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * The Category Entity (Recursive Tree)
 * This allows for infinite sublevels (Electronics > Audio > Headphones).
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categories")
// Add this at the top of the class to skip any weird unknown JSON fields
@JsonIgnoreProperties(ignoreUnknown = true)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
