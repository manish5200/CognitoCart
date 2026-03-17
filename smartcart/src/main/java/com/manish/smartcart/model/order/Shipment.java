package com.manish.smartcart.model.order;

import com.manish.smartcart.model.base.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Shipment extends BaseEntity {

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
}
