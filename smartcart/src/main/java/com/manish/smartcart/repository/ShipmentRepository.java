package com.manish.smartcart.repository;

import com.manish.smartcart.model.order.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    // CONCEPT: Spring Data auto-generates the SQL from the method name.
    // "findByOrder_Id" → SELECT * FROM shipments WHERE order_id = ?
    // We use _ to navigate through the nested relationship: Shipment.order.id
    Optional<Shipment> findByOrder_Id(Long orderId);

    /**
     * Finds a Shipment by AWB tracking number, eagerly loading the linked Order and User.

     * WHY JOIN FETCH:
     * The logistics webhook needs: Shipment → Order (to update status) → User (to send email).
     * Without JOIN FETCH, accessing order.getUser() outside a transaction throws
     * LazyInitializationException.
     * This single SQL query replaces three separate DB round-trips.

     * CONCEPT — AWB as the correlation key:
     * The carrier doesn't know our internal order IDs.
     * The tracking number (AWB) is the only shared identifier between their system and ours.
     */

    @Query("select s from Shipment s " +
            "join fetch s.order o " +
            "join fetch o.user " +
            "where s.trackingNumber = :trackingNumber")
    Optional<Shipment>findByTrackingNumberWithOrderAndUser(
            @Param("trackingNumber") String trackingNumber);
}
