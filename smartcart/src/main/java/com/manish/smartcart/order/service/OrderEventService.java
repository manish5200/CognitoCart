package com.manish.smartcart.order.service;

import com.manish.smartcart.order.dto.OrderEventResponse;
import com.manish.smartcart.order.model.Order;
import com.manish.smartcart.order.model.OrderEvent;
import com.manish.smartcart.order.repository.OrderEventRepository;
import com.manish.smartcart.order.repository.OrderRepository;
import com.manish.smartcart.shared.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventService {

    private final OrderEventRepository orderEventRepository;
    private final OrderRepository orderRepository;


    /**
     * Records an immutable audit event for an order status transition.
     * <p>
     * PROPAGATION.REQUIRES_NEW — intentional design:
     * OrderService uses TransactionTemplate (not @Transactional), so there is
     * no guaranteed active transaction at the call site. REQUIRES_NEW opens a
     * fresh, independent transaction each time — guaranteeing the event is
     * committed even if the outer operation fails. This means partial timeline
     * data (e.g., a PAYMENT_PENDING event for an order that was immediately
     * canceled) is preserved, which is exactly what an audit trail requires.
     *
     * @param orderId internal Long PK (never the public UUID)
     * @param status  the new OrderStatus being applied
     * @param actor   who triggered the transition: "SYSTEM" | "SELLER:42" | "CUSTOMER:7"
     * @param note    optional context e.g. "Razorpay: pay_xxx" or "AWB: BDL123456"
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long orderId, OrderStatus status, String actor, String note) {
        // getReferenceById — avoids a SELECT; we only need the FK association
        Order orderRef = orderRepository.getReferenceById(orderId);

        orderEventRepository.save(
                OrderEvent.builder()
                        .order(orderRef)
                        .status(status)
                        .actor(actor)
                        .note(note)
                        .build()
        );
        log.debug("[ORDER_EVENT] orderId={} status={} actor={}", orderId, status, actor);
    }

    /**
     * Returns the complete status timeline for an order, oldest-first.
     * Called by the timeline endpoint after ownership verification.
     *
     * @param orderId internal Long PK of the order
     */
    @Transactional(readOnly = true)
    public List<OrderEventResponse> getTimeline(Long orderId) {
        return orderEventRepository.findByOrderIdOrderByCreatedAtAsc(orderId)
                .stream()
                .map(e -> OrderEventResponse.builder()
                        .status(e.getStatus())
                        .actor(e.getActor())
                        .note(e.getNote())
                        .occurredAt(e.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
