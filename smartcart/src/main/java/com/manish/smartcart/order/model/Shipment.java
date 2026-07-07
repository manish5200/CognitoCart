package com.manish.smartcart.order.model;

import com.manish.smartcart.shared.model.BaseEntity;
import com.manish.smartcart.shared.util.HumanIdGenerator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Manages the logistics, carrier tracking, and fulfillment lifecycle for a finalized order.
 * * DESIGN NOTE: This entity maintains a strict One-to-One relationship with the Order.
 * By isolating fulfillment data here, the core orders table remains clean and strictly focused
 * on pricing, state management, and line items.
 */

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SequenceGenerator(name = "entity_seq", sequenceName = "shipment_seq", allocationSize = 50)
public class Shipment extends BaseEntity {

    /**
     * Internal, customer-readable tracking identifier (Format: SHP-YYYYMMDD-XXXXXX).
     * Automatically generated on persistence.
     */
    @Column(name = "tracking_code", unique = true, nullable = false, length = 33)
    private String trackingCode;

    // CONCEPT: One-to-One relationship.
    // We put the foreign key (order_id) here in the shipments table.
    // This keeps the `orders` table clean and strictly focused on pricing/items.
    @OneToOne
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    // e.g., "BlueDart", "Delhivery", "Amazon Shipping"
    private String courierName;

    // The AWB (Airway Bill) number used to track the package
    private String trackingNumber;

    // The exact URL where the user can click and see live tracking
    private String trackingUrl;

    // When the customer should expect the package
    private LocalDate estimatedDeliveryDate;

    // Who actually packed or dispatched this (useful for audit logs in admin panel)
    private String dispatchedBy;

    /**
     * Auto-generates the human-readable tracking ID on first database insert.
     * Includes a guard clause to prevent accidental overwrite if the entity is re-persisted.
     */
    @PrePersist
    private void generateHumanID(){
        if(this.trackingCode == null){
            this.trackingCode = HumanIdGenerator.generate("SHP");
        }
    }
}
