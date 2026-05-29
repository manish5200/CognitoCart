// dto/order/ReturnRequestDTO.java
package com.manish.smartcart.dto.order;

import com.manish.smartcart.enums.ReturnReason;
import com.manish.smartcart.enums.ReturnType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReturnRequestDTO {

    @NotNull(message = "returnType is required: RETURN, REPLACEMENT, or EXCHANGE")
    private ReturnType returnType;

    // Predefined category: "DEFECTIVE", "WRONG_ITEM", "CHANGED_MIND", "NOT_AS_DESCRIBED"
    @NotNull(message = "returnReason is required. Valid: DEFECTIVE, WRONG_ITEM, "
            + "DAMAGED_IN_TRANSIT, CHANGED_MIND, NOT_AS_DESCRIBED, SIZE_MISMATCH")
    private ReturnReason returnReason;

    // Customer's optional explanation in their own words
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String returnDescription;
}
