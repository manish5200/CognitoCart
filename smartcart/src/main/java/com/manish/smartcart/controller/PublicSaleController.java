package com.manish.smartcart.controller;

import com.manish.smartcart.dto.product.PlatformSaleEventResponse;
import com.manish.smartcart.enums.EventStatus;
import com.manish.smartcart.model.product.PlatformSaleEvent;
import com.manish.smartcart.repository.PlatformSaleEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ─── WHAT IS THIS? ───────────────────────────────────────────────────────────
 * Public endpoints that the frontend uses to render sale banners.
 * No @PreAuthorize — guests and logged-in users can both access these.
 * The route /api/v1/public/sales/** is whitelisted in SecurityConfig permitAll().

 * ─── NOTE FOR PHASE 5 ────────────────────────────────────────────────────────
 * Currently this hits the DB directly. In Phase 5 (Hardening), this will read
 * from a Redis cache so millions of homepage visitors don't hammer PostgreSQL.
 */

@RestController
@RequestMapping("/api/v1/public/sales")
@RequiredArgsConstructor
public class PublicSaleController {

    private final PlatformSaleEventRepository eventRepository;

    // Frontend uses this to show the "🔥 Diwali Sale LIVE!" banner
    @GetMapping("/live")
    public ResponseEntity<List<PlatformSaleEventResponse>> getLiveEvents(){
        List<PlatformSaleEventResponse> active = eventRepository
                .findByStatus(EventStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(active);
    }

    // Frontend uses this to show countdown timers for upcoming sales
    @GetMapping("/upcoming")
    public ResponseEntity<List<PlatformSaleEventResponse>> getUpcomingEvents() {
        List<PlatformSaleEventResponse> scheduled = eventRepository
                .findByStatus(EventStatus.SCHEDULED)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(scheduled);
    }

    private PlatformSaleEventResponse mapToResponse(PlatformSaleEvent event) {
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
