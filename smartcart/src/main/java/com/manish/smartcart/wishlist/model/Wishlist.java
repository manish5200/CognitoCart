package com.manish.smartcart.wishlist.model;

import com.manish.smartcart.user.model.Users;
import com.manish.smartcart.shared.model.BaseEntity;
import com.manish.smartcart.product.model.Product;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@SuperBuilder
@Table(name = "user_wishlist")
@SequenceGenerator(name = "entity_seq", sequenceName = "wishlist_seq", allocationSize = 50)
public class Wishlist extends BaseEntity {

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
