package com.manish.smartcart.seller.service;

import com.manish.smartcart.order.model.Order;
import com.manish.smartcart.order.model.OrderItem;
import com.manish.smartcart.order.model.Shipment;
import com.manish.smartcart.order.repository.OrderRepository;
import com.manish.smartcart.order.repository.ShipmentRepository;
import com.manish.smartcart.seller.dto.SellerOrderPageResponse;
import com.manish.smartcart.seller.dto.SellerOrderSummaryDTO;
import com.manish.smartcart.shared.enums.OrderStatus;
import com.manish.smartcart.shared.exception.BusinessLogicException;
import com.manish.smartcart.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Seller-facing order visibility service.
 * <p>
 * PRIVACY CONTRACT: Every method guarantees a seller sees ONLY their items
 * within an order — never a competitor's items, prices, or quantities.
 * <p>
 * PRODUCTION PATTERNS:
 * 1. Two-step pagination    → avoids Hibernate HHH90003004 Cartesian product warning
 * 2. Batch shipment load    → 1 query per page (not 1 per order — eliminates N+1)
 * 3. Null-safe item filter  → handles soft-deleted variants/products gracefully
 * 4. Proportional discount  → accurate net revenue per seller (not inflated gross)
 * 5. 404 on unauth access   → prevents order ID enumeration attacks
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // All reads — writes explicitly annotated
public class SellerOrderService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;

    // Orders needing seller action (pack + hand to courier)
    private static final List<OrderStatus> ACTION_REQUIRED = List.of(OrderStatus.CONFIRMED);

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Paginated seller order list with optional status and date range filters.
     * <p>
     * FLOW:
     * Step 1 → ID slice (lightweight pagination — no entity hydration yet)
     * Step 2 → Hydrate ONLY this page's orders with ALL associations in ONE query
     * Step 3 → Batch-load ALL shipments for this page in ONE query (eliminates N+1)
     * Step 4 → Map to DTO (filter to seller's items, prorate discount)
     * Step 5 → Fetch status counts for sidebar badges (single GROUP BY query)
     *
     * @param from  start of date range (inclusive) — null = no lower bound
     * @param to    end of date range (inclusive) — null = no upper bound
     *              Both accept 2026-08-12 or 20260812 via FlexibleDateConverter.
     */
    public SellerOrderPageResponse getMyOrders(Long sellerId, OrderStatus status,
                                               LocalDate from, LocalDate to, Pageable pageable) {
        log.info("[SELLER-ORDERS] sellerId={} status={} from={} to={} page={}",
                sellerId, status, from, to, pageable.getPageNumber());

        // LocalDate → LocalDateTime: "from Aug 1" means 00:00:00, "to Aug 16" means 23:59:59
        LocalDateTime fromDt = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime toDt   = (to   != null) ? to.atTime(23, 59, 59) : null;

        // STEP 1: Lightweight paginated ID slice — avoids Hibernate Cartesian product warning
        Page<Long> idPage = orderRepository.findSellerOrderIds(sellerId, status, fromDt, toDt, pageable);

        if (idPage.isEmpty()) {
            log.info("[SELLER-ORDERS] No orders found for sellerId={}", sellerId);
            return buildEmptyResponse(pageable, getStatusCounts(sellerId));
        }

        // STEP 2: Hydrate ONLY this page's orders with all needed associations (1 query)
        List<Order> orders = orderRepository.findOrdersWithFullDetailsByIds(idPage.getContent());

        // STEP 3: Batch-load ALL shipments for this page in ONE query — eliminates N+1
        // Without this: 20 orders = 20 separate shipment DB calls. With this: 1.
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, Shipment> shipmentByOrderId = orderIds.isEmpty() ? 
                java.util.Collections.emptyMap() : 
                shipmentRepository.findByOrderIds(orderIds)
                        .stream()
                        .collect(Collectors.toMap(s -> s.getOrder().getId(), s -> s));

        // STEP 4: Map to seller-scoped DTOs (only this seller's items, revenue prorated)
        List<SellerOrderSummaryDTO> dtos = orders.stream()
                .map(order -> toSummaryDTO(order, sellerId, shipmentByOrderId.get(order.getId())))
                .toList();

        // STEP 5: Status counts for sidebar badges (single GROUP BY — not N separate queries)
        Map<String, Long> statusCounts = getStatusCounts(sellerId);
        long actionRequired = dtos.stream().filter(SellerOrderSummaryDTO::isActionRequired).count();
        BigDecimal filteredRevenue = dtos.stream()
                .map(SellerOrderSummaryDTO::getMyItemsNetTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("[SELLER-ORDERS] Returning {} orders | actionRequired={} | revenue={}",
                dtos.size(), actionRequired, filteredRevenue);

        return SellerOrderPageResponse.builder()
                .orders(dtos)
                .currentPage(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalOrders(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .hasNext(idPage.hasNext())
                .hasPrevious(idPage.hasPrevious())
                .statusCounts(statusCounts)
                .filteredViewRevenue(filteredRevenue)
                .actionRequiredCount(actionRequired)
                .build();
    }

    /**
     * Single order detail — seller's items only.
     * <p>
     * Uses findByPublicIdWithFullItems (eager fetch) — prevents LazyInitializationException.
     * Returns 404 (not 403) if seller has no items → prevents order ID enumeration.
     */
    public SellerOrderSummaryDTO getMyOrderDetail(UUID orderPublicId, Long sellerId) {
        log.info("[SELLER-ORDERS] Detail: orderPublicId={} sellerId={}", orderPublicId, sellerId);

        Order order = orderRepository.findByPublicIdWithFullItems(orderPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderPublicId));

        assertSellerHasItems(order, sellerId, orderPublicId.toString());

        Shipment shipment = shipmentRepository.findByOrder_Id(order.getId()).orElse(null);
        return toSummaryDTO(order, sellerId, shipment);
    }

    /**
     * Search by human-readable order number (e.g., ORD-20260816-K7P2MQ).
     * Use case: Customer gives seller their order number during a support call.
     */
    public SellerOrderSummaryDTO searchByOrderNumber(String orderNumber, Long sellerId) {
        log.info("[SELLER-ORDERS] Search: orderNumber={} sellerId={}", orderNumber, sellerId);

        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));

        assertSellerHasItems(order, sellerId, orderNumber);

        Shipment shipment = shipmentRepository.findByOrder_Id(order.getId()).orElse(null);
        return toSummaryDTO(order, sellerId, shipment);
    }

    /**
     * Seller marks order as PACKED — the one status transition sellers can trigger.
     * <p>
     * STATE MACHINE: CONFIRMED → PACKED (only)
     * All other transitions (SHIPPED, DELIVERED, CANCELLED) are admin-only.
     * Admin then knows: PACKED = ready for courier assignment.
     */
    @Transactional // Write operation — needs full transaction, not readOnly
    public SellerOrderSummaryDTO markAsPacked(UUID orderPublicId, Long sellerId) {
        log.info("[SELLER-ORDERS] Mark PACKED: orderPublicId={} sellerId={}", orderPublicId, sellerId);

        Order order = orderRepository.findByPublicIdWithFullItems(orderPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderPublicId));

        assertSellerHasItems(order, sellerId, orderPublicId.toString());

        if (order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessLogicException(
                    "Cannot mark as PACKED. Current status: " + order.getOrderStatus() +
                            ". Only CONFIRMED orders can be packed.");
        }

        order.setOrderStatus(OrderStatus.PACKED);
        orderRepository.save(order);

        log.info("[SELLER-ORDERS] Order#{} → PACKED by sellerId={}", order.getOrderNumber(), sellerId);

        Shipment shipment = shipmentRepository.findByOrder_Id(order.getId()).orElse(null);
        return toSummaryDTO(order, sellerId, shipment);
    }

    // ─── Private Mapping ──────────────────────────────────────────────────────

    /** Maps Order → DTO, filtering to seller's items and prorating coupon discount. */
    private SellerOrderSummaryDTO toSummaryDTO(Order order, Long sellerId, Shipment shipment) {

        // Only this seller's items — null-safe chain for soft-deleted variants/products
        List<OrderItem> myRawItems = order.getOrderItems().stream()
                .filter(item -> isSellerItem(item, sellerId))
                .toList();

        List<SellerOrderSummaryDTO.SellerOrderItemDTO> myItemDTOs = myRawItems.stream()
                .map(this::toItemDTO)
                .toList();

        BigDecimal myGross = myRawItems.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Proportional discount: if coupon gives 10% off ₹1000 order and seller's share = ₹600 (60%)
        // → seller's discount = ₹60, net revenue = ₹540
        BigDecimal myDiscountShare = calculateProportionalDiscount(order, myGross);
        BigDecimal myNet = myGross.subtract(myDiscountShare).max(BigDecimal.ZERO);

        return SellerOrderSummaryDTO.builder()
                .orderPublicId(order.getPublicId())
                .orderNumber(order.getOrderNumber())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .actionRequired(ACTION_REQUIRED.contains(order.getOrderStatus()))
                .customerFirstName(extractFirstName(order.getShippingFullName()))
                .shippingCity(order.getShippingCity())
                .shippingState(order.getShippingState())
                .shippingZipCode(order.getShippingZipCode())
                .myItems(myItemDTOs)
                .myItemsCount(myRawItems.stream().mapToInt(OrderItem::getQuantity).sum())
                .myItemsGrossTotal(myGross)
                .myItemsNetTotal(myNet)
                .myDiscountShare(myDiscountShare)
                .returnRequestType(order.getReturnRequestType())
                .returnRequestedAt(order.getReturnRequestedAt())
                // Tracking — null-safe, only available after admin ships the order
                .trackingNumber(shipment != null ? shipment.getTrackingNumber() : null)
                .trackingUrl(shipment != null ? shipment.getTrackingUrl() : null)
                .courierName(shipment != null ? shipment.getCourierName() : null)
                .estimatedDeliveryDate(shipment != null ? shipment.getEstimatedDeliveryDate() : null)
                .build();
    }

    /**
     * Prorates the coupon discount proportionally to this seller's revenue share.
     * FORMULA: myShare = totalDiscount × (myGross / orderSubtotal)
     * Guard: returns 0 if no coupon, or if orderSubtotal is 0 (fully free order).
     */
    private BigDecimal calculateProportionalDiscount(Order order, BigDecimal myGross) {
        BigDecimal discount = order.getDiscountAmount();
        if (discount == null || discount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        // Reconstruct pre-discount subtotal: totalAmount + discount - deliveryFee
        BigDecimal subtotal = order.getTotalAmount()
                .add(discount)
                .subtract(order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO);

        if (subtotal.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO; // Guard: division by zero

        return discount.multiply(myGross).divide(subtotal, 2, RoundingMode.HALF_UP);
    }

    /**
     * Null-safe check: does this OrderItem belong to this seller?
     * Handles soft-deleted products where variant or product may be null.
     */
    private boolean isSellerItem(OrderItem item, Long sellerId) {
        return item.getVariant() != null
                && item.getVariant().getProduct() != null
                && sellerId.equals(item.getVariant().getProduct().getSellerId());
    }

    /**
     * Security gate — 404 (not 403) if seller has no items in this order.
     * WHY 404: 403 would confirm the order exists, enabling order ID enumeration.
     */
    private void assertSellerHasItems(Order order, Long sellerId, String identifier) {
        boolean hasItems = order.getOrderItems().stream().anyMatch(item -> isSellerItem(item, sellerId));
        if (!hasItems) {
            log.warn("[SELLER-ORDERS][SECURITY] sellerId={} has no items in order={}", sellerId, identifier);
            throw new ResourceNotFoundException("Order not found: " + identifier);
        }
    }

    /** Single GROUP BY query → status counts for sidebar badges. */
    private Map<String, Long> getStatusCounts(Long sellerId) {
        return orderRepository.countOrdersByStatusForSeller(sellerId)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((OrderStatus) row[0]).name(),
                        row -> (Long) row[1]
                ));
    }

    private SellerOrderSummaryDTO.SellerOrderItemDTO toItemDTO(OrderItem item) {
        return SellerOrderSummaryDTO.SellerOrderItemDTO.builder()
                .productName(item.getProductNameSnapshot())
                .variantLabel(item.getVariantLabelSnapshot())
                .sku(item.getSkuSnapshot())
                .quantity(item.getQuantity())
                .priceAtPurchase(item.getPriceAtPurchase())
                .lineTotal(item.getLineTotal())
                .imageUrl(item.getImageUrlSnapshot())
                .build();
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Customer";
        return fullName.trim().split("\\s+")[0]; // First word only
    }

    private SellerOrderPageResponse buildEmptyResponse(Pageable pageable, Map<String, Long> statusCounts) {
        return SellerOrderPageResponse.builder()
                .orders(List.of())
                .currentPage(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalOrders(0L).totalPages(0)
                .hasNext(false).hasPrevious(false)
                .statusCounts(statusCounts)
                .filteredViewRevenue(BigDecimal.ZERO)
                .actionRequiredCount(0L)
                .build();
    }
}
