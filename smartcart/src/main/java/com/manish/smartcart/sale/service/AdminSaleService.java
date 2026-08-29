package com.manish.smartcart.sale.service;

import com.manish.smartcart.config.RabbitMQConfig;
import com.manish.smartcart.infrastructure.messaging.FlashSaleCreatedEvent;
import com.manish.smartcart.sale.dto.PlatformSaleEventRequest;
import com.manish.smartcart.sale.dto.PlatformSaleEventResponse;
import com.manish.smartcart.shared.enums.ApprovalStatus;
import com.manish.smartcart.shared.enums.EventStatus;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import com.manish.smartcart.sale.model.FlashSaleItem;
import com.manish.smartcart.sale.model.PlatformSaleEvent;
import com.manish.smartcart.sale.repository.FlashSaleItemRepository;
import com.manish.smartcart.sale.repository.PlatformSaleEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrator service for platform-wide promotional events and flash sales.
 * * Acts as the central authority for campaign scheduling and seller discount validation.
 * Employs event-driven architecture to decouple heavy background processes (e.g., mass communications)
 * from the primary HTTP request thread, ensuring high availability for the Admin portal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSaleService {

    private final PlatformSaleEventRepository eventRepository;
    private final FlashSaleItemRepository itemRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Provisions a new global platform sale event and broadcasts its creation.
     * * @param request The scheduling parameters for the new campaign.
     * @return PlatformSaleEventResponse The successfully provisioned event details.
     * @throws BusinessLogicException if the chronological ordering of timestamps is invalid.
     */
    @Transactional
    public PlatformSaleEventResponse createEvent(PlatformSaleEventRequest request){
        if (request.getEndTime().isBefore(request.getStartTime())) {
            log.error("Campaign provision failed: End time is strictly before the start time.");
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
        log.info("Provisioned new Platform Sale Event: {} [ID: {}]", savedEvent.getEventName(), savedEvent.getId());

        // Asynchronously broadcast campaign creation to downstream consumer groups (e.g., Seller Notification Service).
        // Failure to publish will be handled by RabbitMQ retries/DLQ, preventing primary transaction rollback.
        FlashSaleCreatedEvent rmqPayload = FlashSaleCreatedEvent.builder()
                .eventId(savedEvent.getId())
                .eventName(savedEvent.getEventName())
                .startTime(savedEvent.getStartTime())
                .endTime(savedEvent.getEndTime())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_MARKETING,
                RabbitMQConfig.ROUTING_KEY_FLASH_SALE,
                rmqPayload
        );
        log.info("Dispatched FlashSaleCreatedEvent to message broker [Exchange: {}]", RabbitMQConfig.EXCHANGE_MARKETING);
        return mapToEventResponse(savedEvent);
    }

    /**
     * Executes quality control validation on seller-submitted flash sale inventory.
     * Prevents unauthorized pricing manipulations and enforces marketplace compliance.
     *
     * @param itemPublicId The unique identifier of the seller's submitted inventory item.
     * @param status The final approval decision (APPROVED or REJECTED).
     * @throws BusinessLogicException if attempting to revert a finalized decision to PENDING.
     * @throws ResourceNotFoundException if the specified submission ID does not exist.
     */
    @Transactional
    public void reviewSellerSubmission(UUID itemPublicId, ApprovalStatus status){
        Long flashSaleItemId = itemRepository.findByPublicId(itemPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Flash sale item not found: " + itemPublicId))
                .getId();
        if (status == ApprovalStatus.PENDING) {
            log.warn("Attempted illegal state transition: Reverting reviewed item [{}] to PENDING.", flashSaleItemId);
            throw new BusinessLogicException("Cannot revert a reviewed item back to PENDING status.");
        }

        FlashSaleItem item = itemRepository.findById(flashSaleItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Flash Sale Submission not found for ID: " + flashSaleItemId));

        item.setApprovalStatus(status);
        itemRepository.save(item);

        log.info("Executed compliance review: Item [{}] assigned status [{}]", flashSaleItemId, status.name());
    }

    /**
     * Retrieves a complete manifest of all historical and upcoming platform sale events.
     *
     * @return List containing the summarized details of all platform events.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<PlatformSaleEventResponse> getAllEvents(){
        return eventRepository.findAll().stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all items submitted to a specific flash sale event (For Admin Dashboard).
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<com.manish.smartcart.sale.dto.FlashSaleItemResponse> getEventSubmissions(UUID eventPublicId) {
        return itemRepository.findByPlatformSaleEventPublicId(eventPublicId).stream()
                .map(item -> com.manish.smartcart.sale.dto.FlashSaleItemResponse.builder()
                        .publicId(item.getPublicId())
                        .saleEventPublicId(item.getPlatformSaleEvent().getPublicId())
                        .id(item.getId())
                        .eventId(item.getPlatformSaleEvent().getId())
                        .eventName(item.getPlatformSaleEvent().getEventName())
                        .variantId(item.getProductVariant().getId())
                        .sku(item.getProductVariant().getSku())
                        .discountPercentage(item.getDiscountPercentage())
                        .maxUnits(item.getMaxUnits())
                        .maxUnitsPerUser(item.getMaxUnitsPerUser())
                        .usedUnits(item.getUsedUnits())
                        .approvalStatus(item.getApprovalStatus())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Maps the internal domain entity to the external data transfer object.
     */
    private PlatformSaleEventResponse mapToEventResponse(PlatformSaleEvent event) {
        return PlatformSaleEventResponse.builder()
                .saleEventPublicId(event.getPublicId())
                .eventName(event.getEventName())
                .description(event.getDescription())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .status(event.getStatus())
                .build();
    }

}
