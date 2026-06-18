package com.manish.smartcart.service;

import com.manish.smartcart.dto.product.PlatformSaleEventRequest;
import com.manish.smartcart.dto.product.PlatformSaleEventResponse;
import com.manish.smartcart.enums.ApprovalStatus;
import com.manish.smartcart.enums.EventStatus;
import com.manish.smartcart.exception.BusinessLogicException;
import com.manish.smartcart.exception.ResourceNotFoundException;
import com.manish.smartcart.model.product.FlashSaleItem;
import com.manish.smartcart.model.product.PlatformSaleEvent;
import com.manish.smartcart.repository.FlashSaleItemRepository;
import com.manish.smartcart.repository.PlatformSaleEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Enterprise Admin Service for orchestrating Platform-wide Flash Sales.
 * Controls the global marketing events and serves as the Quality Control gatekeeper.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSaleService {

    private final PlatformSaleEventRepository eventRepository;
    private final FlashSaleItemRepository itemRepository;

    /**
     * Schedules a massive global event (e.g., "Big Billion Days").
     * The ShedLock background job will automatically activate this when startTime hits.
     */
    @Transactional
    public PlatformSaleEventResponse createEvent(PlatformSaleEventRequest request){
        if(request.getEndTime().isBefore(request.getStartTime())){
            log.error("End time must be before start time");
            throw new BusinessLogicException("Event end time must be strictly after the start time.");
        }

        PlatformSaleEvent event = PlatformSaleEvent.builder()
                .eventName(request.getEventName())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(EventStatus.SCHEDULED)
                .build();

        PlatformSaleEvent savedEvent =  eventRepository.save(event);

        log.info("Admin scheduled new Platform Sale Event: {} [ID: {}]", savedEvent.getEventName(), savedEvent.getId());
        // Note: In Phase 4C, we will trigger a RabbitMQ event here to Mass-Email all Sellers!

        return mapToEventResponse(savedEvent);
    }

    /**
     * The QC Gatekeeper: Approves or Rejects a Seller's submitted discount.
     * Prevents sellers from offering fake discounts (e.g. raising base price 50%, then discounting 50%).
     */
    @Transactional
    public void reviewSellerSubmission(Long flashSaleItemId, ApprovalStatus status){
        if(status == ApprovalStatus.PENDING){
            throw new BusinessLogicException("Cannot revert a reviewed item back to PENDING status.");
        }

        FlashSaleItem item = itemRepository.findById(flashSaleItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Flash Sale Submission not found"));

        item.setApprovalStatus(status);
        itemRepository.save(item);

        log.info("Admin {} submission ID: {}", status.name(), flashSaleItemId);
    }

    /**
     * Fetches all events for the Admin Dashboard.
     */
    public List<PlatformSaleEventResponse> getAllEvents(){
        return eventRepository.findAll().stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    private PlatformSaleEventResponse mapToEventResponse(PlatformSaleEvent event) {
        return PlatformSaleEventResponse.builder()
                .id(event.getId())
                .eventName(event.getEventName())
                .description(event.getDescription())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .status(event.getStatus())
                .build();
    }

}
