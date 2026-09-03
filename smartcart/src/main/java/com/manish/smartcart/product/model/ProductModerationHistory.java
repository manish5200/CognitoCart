package com.manish.smartcart.product.model;

import com.manish.smartcart.shared.enums.product.ActorType;
import com.manish.smartcart.shared.enums.product.ModerationAction;
import com.manish.smartcart.shared.enums.product.ProductApprovalStatus;
import com.manish.smartcart.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * An immutable ledger tracking every state transition in a product's lifecycle.
 * Prevents disputes by storing a permanent record of who approved/rejected what and why.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "product_moderation_history")
@SequenceGenerator(name = "entity_seq", sequenceName = "product_moderation_history_seq", allocationSize = 50)
public class ProductModerationHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // The Admin who made the decision. Nullable because the SYSTEM actor can automate this.
    @Column(name = "admin_id")
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private ActorType actorType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ModerationAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status_from", length = 30)
    private ProductApprovalStatus approvalStatusFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status_to", nullable = false, length = 30)
    private ProductApprovalStatus approvalStatusTo;

    // Detailed feedback left for the seller regarding required changes or rejections
    @Column(columnDefinition = "TEXT")
    private String reason;
}
