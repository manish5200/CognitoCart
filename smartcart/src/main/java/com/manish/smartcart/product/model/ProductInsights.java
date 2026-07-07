package com.manish.smartcart.product.model;

import com.manish.smartcart.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "product_insights")
@SequenceGenerator(name = "entity_seq", sequenceName = "product_insights_seq",  allocationSize = 50)
public class ProductInsights extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", unique = true, nullable = false)
    private Product product;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    private Long totalReviews;

    private LocalDateTime lastGenerated;
}
