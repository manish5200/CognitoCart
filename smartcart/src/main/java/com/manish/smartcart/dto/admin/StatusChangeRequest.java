package com.manish.smartcart.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class StatusChangeRequest {

    @NotNull
    @NotBlank(message = "Order ID is required")
    private Long orderId;

    @NotNull
    private String orderStatus;
}
