package com.manish.smartcart.repository;

import com.manish.smartcart.model.order.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    // CONCEPT: Spring Data auto-generates the SQL from the method name.
    // "findByOrder_Id" → SELECT * FROM shipments WHERE order_id = ?
    // We use _ to navigate through the nested relationship: Shipment.order.id
    Optional<Shipment> findByOrder_Id(Long orderId);
}
