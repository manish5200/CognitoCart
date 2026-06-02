# Return / Replacement / Exchange Feature — Full Codebase Map
> Scanned: 2026-05-27 | Project: CognitoCart

---

## LAYER 1 — DATABASE (Flyway Migrations)

| File | Status | What it does |
|------|--------|-------------|
| `V24__create_product_return_policy.sql` | ✅ DONE | Creates `product_return_policy` table with all 6 boolean/enum columns + CHECK constraint (product XOR category) + indexes |
| `V25__add_return_fields_to_orders.sql` | ✅ DONE | Adds `return_reason`, `return_description`, `return_requested_at`, `delivered_at`, `return_policy_snapshot` (JSONB), `return_request_type` to `orders` table |

---

## LAYER 2 — ENUMS

| File | Status | Values |
|------|--------|--------|
| `enums/PolicyType.java` | ✅ DONE | `RETURN_AND_EXCHANGE`, `RETURN_ONLY`, `EXCHANGE_ONLY`, `REPLACEMENT_ONLY`, `NON_RETURNABLE` |
| `enums/ReturnType.java` | ✅ DONE | `RETURN`, `REPLACEMENT`, `EXCHANGE` — fully documented |
| `enums/OrderStatus.java` | ✅ DONE | Has: `RETURN_REQUESTED`, `REPLACEMENT_REQUESTED`, `EXCHANGE_REQUESTED`, `RETURNED`, `REFUNDED`, `REPLACEMENT_SHIPPED` |
| `enums/PaymentStatus.java` | ✅ DONE | Has `REFUNDED` — used in `approveReturn()` |

---

## LAYER 3 — MODEL (JPA Entities)

| File | Status | Notes |
|------|--------|-------|
| `model/product/ProductReturnPolicy.java` | ✅ DONE | Has all fields: `policyType`, `returnWindowDays`, `returnAllowed`, `exchangeAllowed`, `replacementAllowed`, `pickupAvailable`. Chain: product → category → default |
| `model/order/Order.java` | ✅ DONE | Has: `returnReason`, `returnDescription`, `returnRequestedAt`, `deliveredAt`, `returnPolicySnapshot` (JSONB String), `returnRequestType` (ReturnType enum) |
| `model/order/OrderItem.java` | ✅ DONE | No changes needed — `product` relationship used in stock check |

---

## LAYER 4 — REPOSITORY

| File | Status | Methods |
|------|--------|---------|
| `ProductReturnPolicyRepository.java` | ✅ DONE | `findByProduct_Id(Long)`, `findByCategory_Id(Long)` |
| `OrderRepository.java` | ✅ DONE | `findById()`, `findByIdWithItems()` — both used correctly |

---

## LAYER 5 — DTOs

| File | Status | Notes |
|------|--------|-------|
| `dto/order/PolicySnapshot.java` | ✅ DONE | `policyType`, `returnWindowDays`, `returnAllowed`, `exchangeAllowed`, `replacementAllowed`, `pickupAvailable` — serialized to JSONB at checkout |
| `dto/order/ReturnRequestDTO.java` | ✅ DONE | `returnType` (@NotNull), `returnReason` (@NotBlank), `returnDescription` (@NotBlank) |
| `dto/order/OrderResponse.java` | ⚠️ GAP | Does NOT expose `returnRequestType`, `returnReason`, `returnDescription`, `returnRequestedAt` — customer sees no confirmation detail in response |

---

## LAYER 6 — SERVICES

### ReturnPolicyService.java ✅ DONE
- `getApplicablePolicy(product)` — chain of responsibility: product → category → NON_RETURNABLE default
- `getPolicySnapshotForCheckout(product)` — called at checkout
- `getLivePolicyForProduct(product)` — for product page display

### OrderService.java — Return methods

| Method | Status | Notes |
|--------|--------|-------|
| Policy snapshot at checkout (`placeOrder`) | ⚠️ DESIGN NOTE | Snapshots only `orderItems.get(0)` (first product's policy). Fine for now, but multi-product orders with different seller policies will use first product's policy for entire order |
| `requestReturn(userId, orderId, ReturnType, reason, description)` | ✅ DONE | Full Guards: ownership ✅, DELIVERED status ✅, duplicate check ✅, window deadline ✅, NON_RETURNABLE hard stop ✅, policy matrix switch ✅, live stock check for REPLACEMENT ✅, `buildAvailableOptionsHint()` hint ✅, notification email sent ✅ |
| `approveReturn(orderId)` | ✅ DONE | Guard: RETURN_REQUESTED state ✅, stock restored ✅, Razorpay refund ✅, graceful refund failure (saves RETURNED, throws) ✅, refund email ✅ |
| `approveReplacement(orderId)` | ✅ DONE | Guard: REPLACEMENT_REQUESTED or EXCHANGE_REQUESTED ✅, live stock re-check ✅, stock deducted ✅, status = REPLACEMENT_SHIPPED ✅, status email ✅ |

### ShipmentService.java

| Location | Status | Notes |
|----------|--------|-------|
| `processLogisticsUpdate()` — set `deliveredAt` | ✅ DONE | Line 167-169: `if (newOrderStatus == DELIVERED) order.setDeliveredAt(LocalDateTime.now())` — correctly stamped |

### RazorpayRefundService.java ✅ DONE
- `initiateFullRefund(paymentId, amount)` — empty JSONObject, full refund, returns `refundId`

---

## LAYER 7 — CONTROLLERS

| Endpoint | Method | Status | Notes |
|----------|--------|--------|-------|
| `POST /api/v1/orders/{orderId}/request-return` | `OrderController.requestReturn()` | ✅ DONE | JWT secured, @Valid, unpacks DTO → calls `orderService.requestReturn()` |
| `PUT /api/v1/admin/{orderId}/approve-return` | `AdminController.approveReturn()` | ✅ DONE | ADMIN role, calls `orderService.approveReturn()` |
| `PUT /api/v1/admin/{orderId}/approve-replacement` | `AdminController.approveReplacement()` | ✅ DONE | ADMIN role, handles REPLACEMENT_REQUESTED + EXCHANGE_REQUESTED |

---

## LAYER 8 — NOTIFICATIONS / EMAIL

| Email | Status | Notes |
|-------|--------|-------|
| `sendStatusUpdateEmail()` | ✅ DONE | Used by `requestReturn()` + `approveReplacement()` |
| `sendRefundEmail()` | ✅ DONE | Used by `approveReturn()` when Razorpay refund succeeds |
| `buildOrderStatusUpdate()` switch in EmailTemplateBuilder | ⚠️ GAP | Has `RETURN_REQUESTED` ✅, `RETURNED` ✅, `REFUNDED` ✅ — **MISSING**: `REPLACEMENT_REQUESTED`, `EXCHANGE_REQUESTED`, `REPLACEMENT_SHIPPED` → falls to `default` ("status updated, visit the app") — works but not specific |

---

## GAPS SUMMARY

### 🔴 HIGH — Will cause wrong behavior
| # | Gap | Where | Impact |
|---|-----|-------|--------|
| 1 | `OrderResponse` does not expose return fields | `OrderResponse.java` + `OrderMapper.java` | Customer's `/request-return` response shows nothing about their request — they see no `returnType`, `returnReason`, `status` detail |

### 🟡 MEDIUM — Functional but suboptimal
| # | Gap | Where | Impact |
|---|-----|-------|--------|
| 2 | Email status messages missing for `REPLACEMENT_REQUESTED`, `EXCHANGE_REQUESTED`, `REPLACEMENT_SHIPPED` | `EmailTemplateBuilder.buildOrderStatusUpdate()` | Customer gets a generic "status updated" email instead of a meaningful message |
| 3 | `returnDescription` is `@NotBlank` in DTO | `ReturnRequestDTO.java` | Should be optional — customer shouldn't be forced to write a description |
| 4 | Policy snapshot only uses `orderItems.get(0)` | `OrderService.placeOrder()` | Multi-product order with different seller policies → entire order governed by first product's policy |

### 🟢 LOW — Nice to have
| # | Gap | Where | Impact |
|---|-----|-------|--------|
| 5 | No `GET /api/v1/admin/orders/pending-returns` endpoint | AdminController | Admin has no way to list all pending return requests in one call |
| 6 | No admin "reject return" endpoint | AdminController | Admin can only approve, not explicitly reject (customers in RETURN_REQUESTED state forever if admin doesn't approve) |
| 7 | `approveReturn()` has no `@Transactional` | `OrderService.java` | If refund API call fails mid-method, stock is already restored but order status not saved cleanly |

---

## COMPLETE FEATURE FLOW (As Built)

```
CHECKOUT ──────────────────────────────────────────────────────────────
  placeOrder()
    └─ returnPolicyService.getPolicySnapshotForCheckout(firstProduct)
    └─ objectMapper.writeValueAsString(snapshot) → order.returnPolicySnapshot (JSONB)

DELIVERY ───────────────────────────────────────────────────────────────
  ShipmentService.processLogisticsUpdate() [carrier webhook]
    └─ if DELIVERED → order.deliveredAt = LocalDateTime.now()  ✅

CUSTOMER REQUEST ────────────────────────────────────────────────────────
  POST /api/v1/orders/{orderId}/request-return
    └─ OrderController.requestReturn()
    └─ OrderService.requestReturn()
        ├─ Guard 1: ownership check
        ├─ Guard 2: status == DELIVERED
        ├─ Guard 3: no duplicate request (returnRequestedAt == null)
        ├─ Guard 4: parse returnPolicySnapshot (JSONB → PolicySnapshot)
        ├─ Guard 5: return window (deliveredAt + returnWindowDays > now)
        ├─ Guard 6: NON_RETURNABLE hard stop
        ├─ Guard 7: policy matrix switch
        │   ├─ RETURN     → returnAllowed?  → RETURN_REQUESTED
        │   ├─ REPLACEMENT→ replacementAllowed? + live stock > 0? → REPLACEMENT_REQUESTED
        │   └─ EXCHANGE   → exchangeAllowed? → EXCHANGE_REQUESTED
        └─ sendStatusUpdateEmail()

ADMIN APPROVAL (RETURN) ─────────────────────────────────────────────────
  PUT /api/v1/admin/{orderId}/approve-return
    └─ OrderService.approveReturn()
        ├─ Guard: status == RETURN_REQUESTED
        ├─ Restore stock for all items
        ├─ Razorpay refund → REFUNDED
        └─ sendRefundEmail()

ADMIN APPROVAL (REPLACEMENT / EXCHANGE) ────────────────────────────────
  PUT /api/v1/admin/{orderId}/approve-replacement
    └─ OrderService.approveReplacement()
        ├─ Guard: status == REPLACEMENT_REQUESTED OR EXCHANGE_REQUESTED
        ├─ Re-check live stock (may have dropped since request)
        ├─ Deduct stock again
        ├─ status → REPLACEMENT_SHIPPED
        ├─ sendStatusUpdateEmail()
        └─ [Admin then calls POST /admin/{orderId}/shipment to attach tracking]
```

---

## WHAT TO FIX NEXT (Priority Order)

1. **`OrderResponse` + `OrderMapper`** — Add return fields to response (HIGH)
2. **`EmailTemplateBuilder`** — Add `REPLACEMENT_REQUESTED`, `EXCHANGE_REQUESTED`, `REPLACEMENT_SHIPPED` messages (MEDIUM)
3. **`ReturnRequestDTO`** — Make `returnDescription` optional / `@NotBlank` → remove or use `@Size` (MEDIUM)
4. **`approveReturn()` @Transactional** — Add annotation (LOW but safe)
5. **Admin: reject return endpoint** — `PUT /admin/{orderId}/reject-return` → sets status back to DELIVERED (LOW)
6. **Admin: list pending returns** — `GET /admin/orders/pending-returns` (LOW)
