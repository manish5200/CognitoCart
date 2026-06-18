package com.manish.smartcart.repository;

import com.manish.smartcart.enums.EventStatus;
import com.manish.smartcart.model.product.PlatformSaleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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
}
