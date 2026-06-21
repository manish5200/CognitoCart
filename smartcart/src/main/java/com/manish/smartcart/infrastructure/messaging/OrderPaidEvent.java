package com.manish.smartcart.infrastructure.messaging;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidEvent {
    private Long orderId;
}
