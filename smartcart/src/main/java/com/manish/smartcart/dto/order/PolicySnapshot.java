package com.manish.smartcart.dto.order;

import com.manish.smartcart.enums.PolicyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Immutable snapshot of return policy captured at the moment an order is placed.
 *
 * WHY SNAPSHOT?
 * If a seller changes their return policy tomorrow, orders placed today must
 * still honor the policy that was active when the customer checked out.
 * This DTO is serialized to JSONB in orders.return_policy_snapshot.
 *
 * NO JPA annotations here — this is a plain Java DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicySnapshot {
    private PolicyType policyType;
    private int returnWindowDays;
    private boolean returnAllowed;
    private boolean exchangeAllowed;
    private boolean replacementAllowed;
    private boolean pickupAvailable;
}
