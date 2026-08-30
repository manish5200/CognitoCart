package com.manish.smartcart.product.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manish.smartcart.shared.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.util.ArrayList;
import java.util.List;

/**
 * The Category Entity (Recursive Tree)
 * Allows for infinite sublevels (Electronics > Audio > Headphones).
 **/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "categories")
@SequenceGenerator(name = "entity_seq", sequenceName = "category_seq",  allocationSize = 50)
@SQLDelete(sql = "UPDATE categories SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted = false")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Category extends BaseEntity {

    @NotBlank
    private String name;

    /** Smart: For SEO URLs (e.g., /category/laptops) */
    @NotBlank
    @Column(unique = true)
    private String slug;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;


    /**
     * serializing this side, preventing infinite circular JSON loops.
     */
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parentCategory;


    /**
     * Children — serialized via CategoryDTO.from() in API responses.
     */
    @JsonIgnore   // ← add back — entity never returned from controller, only DTO is
    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Category> subCategories = new ArrayList<>();


}
