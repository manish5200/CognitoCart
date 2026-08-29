package com.manish.smartcart.order.repository;

import com.manish.smartcart.order.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    /**
     * Finds a single shipment by its Order ID.
     */
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
    Optional<Shipment> findByTrackingNumberWithOrderAndUser(
            @Param("trackingNumber") String trackingNumber);

    // PUBLIC ID LOOKUP: UUID-based secure lookup for internal API endpoints.
    Optional<Shipment> findByPublicId(UUID publicId);

    /**
     * Human ID lookup — used by the PUBLIC tracking page (no login required).
     * GET /api/track/SHP-20260710-T9R4NB
     * The customer gets this code in their dispatch email and uses it to track delivery.
     */
    Optional<Shipment> findByTrackingCode(String trackingCode);

    /**
     * Batch-loads shipments for multiple order IDs in ONE query.
     * Eliminates the N+1 problem in seller order list: 20 orders = 1 query, not 20.
     * Called ONCE before the order→DTO mapping loop, then looked up by orderId in a Map.
     */
    @Query("SELECT s FROM Shipment s WHERE s.order.id IN :orderIds")
    List<Shipment> findByOrderIds(@Param("orderIds") List<Long> orderIds);
}
