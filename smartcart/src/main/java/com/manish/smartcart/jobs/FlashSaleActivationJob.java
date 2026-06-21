package com.manish.smartcart.jobs;

import com.manish.smartcart.shared.enums.EventStatus;
import com.manish.smartcart.sale.model.PlatformSaleEvent;
import com.manish.smartcart.sale.repository.PlatformSaleEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Automates the Lifecycle of PlatformSaleEvents.
 * Uses ShedLock so if you run 5 instances of CognitoCart, this job only runs once!
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleActivationJob {

    private final PlatformSaleEventRepository eventRepository;

    @Scheduled(cron = "0 * * * * * ")// Runs at second :00 of every minute
    @SchedulerLock(name = "FlashSaleActivationJob_Lock", lockAtLeastFor = "30s", lockAtMostFor = "50s")
    @Transactional
    public void processSaleEvents(){
        LocalDateTime now = LocalDateTime.now();
        log.debug("Running Flash Sale Activation Job at {}", now);

        // 1. Activate Scheduled Events that have reached their start time
        List<PlatformSaleEvent> readyToActivate = eventRepository.findReadyToActivate(now);
        for(PlatformSaleEvent event : readyToActivate){
            event.setStatus(EventStatus.ACTIVE);
            eventRepository.save(event);
            log.info("🚀 FLASH SALE IS LIVE: Activated Event [{}]", event.getEventName());

            // Phase 4C Note: Here is where you would flush the Redis Cache or push a WebSocket event to users
        }

        // 2. Deactivate Active Events that have reached their end time
        List<PlatformSaleEvent> readyToDeactivate = eventRepository.findReadyToDeactivate(now);
        for (PlatformSaleEvent event : readyToDeactivate) {
            event.setStatus(EventStatus.ENDED);
            eventRepository.save(event);
            log.info("🛑 FLASH SALE ENDED: Deactivated Event [{}]", event.getEventName());
        }
    }
}
