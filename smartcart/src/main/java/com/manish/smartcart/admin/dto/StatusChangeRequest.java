package com.manish.smartcart.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class StatusChangeRequest {

    @NotNull(message = "Order ID is required")
    private java.util.UUID orderPublicId;

    @NotNull
    private String orderStatus;
}
