package com.manish.smartcart.sale.service;

import com.manish.smartcart.sale.dto.FlashSaleItemRequest;
import com.manish.smartcart.sale.dto.FlashSaleItemResponse;
import com.manish.smartcart.shared.enums.ApprovalStatus;
import com.manish.smartcart.shared.enums.EventStatus;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import com.manish.smartcart.sale.model.FlashSaleItem;
import com.manish.smartcart.sale.model.PlatformSaleEvent;
import com.manish.smartcart.product.model.ProductVariant;
import com.manish.smartcart.sale.repository.FlashSaleItemRepository;
import com.manish.smartcart.sale.repository.PlatformSaleEventRepository;
import com.manish.smartcart.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Enterprise Seller Service for handling Seller opt-ins to global events.
 * Enforces strict ownership validation to prevent IDOR vulnerabilities.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerSaleService {

    private final FlashSaleItemRepository itemRepository;
    private final PlatformSaleEventRepository eventRepository;
    private final ProductVariantRepository variantRepository;

    /**
     * Allows a Seller to manually submit a single SKU to an upcoming Admin Event.
     * Note: Phase 4C will introduce a Bulk CSV Importer that calls this logic asynchronously.
     */
    @Transactional
    public FlashSaleItemResponse submitFlashSaleItem(FlashSaleItemRequest request, Long sellerId){

        // 1. Ensure the Event exists and is still accepting submissions (Not Ended)
        PlatformSaleEvent event = eventRepository.findByPublicId(request.getSaleEventPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if(event.getStatus() == EventStatus.ENDED){
            throw new BusinessLogicException("Cannot submit items to an ended event.");
        }

        // 2. Ensure the Variant actually exists in the database
        ProductVariant variant = variantRepository.findByPublicId(request.getVariantPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));

        // 3. CRITICAL SECURITY (IDOR Prevention):
        // A malicious seller could try to submit a competitor's product variant ID to put it on a 90% sale!
        // We MUST verify that the logged-in Seller actually owns the Product tied to this Variant.
        if(!variant.getProduct().getSellerId().equals(sellerId)){
            throw new BusinessLogicException("SECURITY ALERT: You cannot submit a product you do not own.");
        }

        // 4. Create the submission (Defaults to PENDING for Admin QC)
        FlashSaleItem item = FlashSaleItem.builder()
                .platformSaleEvent(event)
                .productVariant(variant)
                .sellerId(sellerId)
                .discountPercentage(request.getDiscountPercentage())
                .maxUnits(request.getMaxUnits())
                .maxUnitsPerUser(request.getMaxUnitsPerUser())
                .approvalStatus(ApprovalStatus.PENDING)
                .build();

        FlashSaleItem savedItem = itemRepository.save(item);
        log.info("Seller ID: {} submitted Variant ID: {} to Event: {}", sellerId, variant.getId(), event.getEventName());
        return mapToItemResponse(savedItem);
    }

    /**
     * Fetches all submissions for the Seller Dashboard.
     */
    @Transactional(readOnly = true)
    public List<FlashSaleItemResponse> getSellerSubmissions(Long sellerId) {
        return itemRepository.findBySellerId(sellerId).stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }


    private FlashSaleItemResponse mapToItemResponse(FlashSaleItem item) {
        return FlashSaleItemResponse.builder()
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
                .build();
    }

}
