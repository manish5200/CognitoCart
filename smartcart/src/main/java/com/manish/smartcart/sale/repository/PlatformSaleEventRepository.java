package com.manish.smartcart.sale.repository;

import com.manish.smartcart.shared.enums.EventStatus;
import com.manish.smartcart.sale.model.PlatformSaleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformSaleEventRepository extends JpaRepository<PlatformSaleEvent, Long> {

    /**
     * Called by ShedLock: Identifies events whose startTime has arrived but are still SCHEDULED.
     */
    @Query("SELECT e FROM PlatformSaleEvent e WHERE e.status = 'SCHEDULED' AND e.startTime <= :now")
    List<PlatformSaleEvent> findReadyToActivate(@Param("now") LocalDateTime now);

    /**
     * Called by ShedLock: Identifies events whose endTime has passed but are still ACTIVE.
     */
    @Query("SELECT e FROM PlatformSaleEvent e WHERE e.status = 'ACTIVE' AND e.endTime <= :now")
    List<PlatformSaleEvent> findReadyToDeactivate(@Param("now") LocalDateTime now);

    // Simple fetch for Admin dashboards
    List<PlatformSaleEvent> findByStatus(EventStatus status);

    // PUBLIC ID LOOKUP
    Optional<PlatformSaleEvent> findByPublicId(UUID publicId);

    // HUMAN ID LOOKUP
    Optional<PlatformSaleEvent> findByEventCode(String eventCode);
}
