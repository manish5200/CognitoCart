package com.manish.smartcart.model.user;

import com.manish.smartcart.model.base.BaseEntity;
import com.manish.smartcart.model.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
@Table(name = "user_wishlist")
public class Wishlist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // CONCEPT: The "Anti-Spam Lock". Once we email this user for this specific product,
    // we stamp this with the exact time. The Scheduler uses this to enforce a 14-day cooldown so
    // the user doesn't get 14 emails in a row if the sale lasts two weeks!
    @Column(name = "last_price_drop_notified_at")
    private LocalDateTime lastPriceDropNotifiedAt;

}
