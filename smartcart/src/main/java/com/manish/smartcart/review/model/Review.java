package com.manish.smartcart.review.model;

import com.manish.smartcart.shared.model.BaseEntity;
import com.manish.smartcart.product.model.Product;
import com.manish.smartcart.user.model.Users;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(
                name = "uq_review_user_product",
                columnNames = {"user_id", "product_id"}
))
@SequenceGenerator(name = "entity_seq", sequenceName = "review_seq",  allocationSize = 50)
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    @NotNull
    private Integer rating;

    @Column(length = 1000)
    private String comment;
}
