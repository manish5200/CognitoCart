package com.manish.smartcart.dto.admin;

import com.manish.smartcart.enums.KycStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class KycUpdateRequest {
    @NotNull(message = "KYC status is required")
    private KycStatus status;

    @Size(max = 500, message = "Admin comment must be under 500 characters")
    private String adminComment;
}
