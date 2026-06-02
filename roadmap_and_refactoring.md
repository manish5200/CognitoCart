# CognitoCart — Phase-wise Roadmap & Refactoring Status
> **Last Verified:** 2026-06-02 | **Build:** SUCCESS ✅ | **Migrations:** V29 complete (next = V30)

---

## 🔴 CRITICAL BUGS — STATUS

| Bug | Status | Detail |
|-----|--------|--------|
| Bug 1: Null pointer in `requestReturn` (`||` → `&&`) | ✅ FIXED | `ReturnReason` enum + `requiresImageProof()` replaces the broken boolean entirely |
| Bug 2: `cancelOrder` raw `RuntimeException` for 404/403 | ✅ FIXED | Now uses `ResourceNotFoundException` + `BusinessLogicException` |
| Bug 2b: `cancelOrder` refund failure still throws `RuntimeException` | ⚠️ OPEN | `OrderService.java:303` — should be `BusinessLogicException` |
| Bug 3: `IMMUTABLE_STATES` missing return/exchange states | ✅ FIXED | `AdminService` now includes `RETURN_REQUESTED`, `REPLACEMENT_REQUESTED`, `EXCHANGE_REQUESTED`, `REPLACEMENT_SHIPPED` |

---

## 🟠 REFACTORING — STATUS

| Refactor | Status | Notes |
|----------|--------|-------|
| Refactor 1: Split `OrderService` God Class | ✅ DONE | `service/order/` → `OrderService`, `OrderQueryService`, `OrderReturnService`, `ReturnAdminService` |
| Refactor 2: Extract `ReturnReasonEnum` | ✅ DONE | `enums/ReturnReason.java` — type-safe, `requiresImageProof()` built-in |
| Refactor 3: Delete `FileService.java` (dead code) | ⚠️ OPEN | File still exists — 5 min to delete |
| Refactor 4: `@AllArgsConstructor` → `@RequiredArgsConstructor` | ⚠️ OPEN | `service/order/OrderService.java:39` still uses `@AllArgsConstructor` |
| Refactor 5: `AdminService.changeTheStatusOfOrders()` returns `OrderResponse` | ⚠️ OPEN | Still returns `Order` entity — layering violation |

---

## ✅ PHASE 1 — Seller Self-Service Return Policy
**Branch:** `feat/seller-return-policy` | **Status: COMPLETE ✅**

| Feature | Endpoint | Status |
|---------|----------|--------|
| Seller creates return policy | `POST /api/v1/sellers/return-policy` | ✅ Done |
| Seller views their policies | `GET /api/v1/sellers/return-policy` | ✅ Done |
| Seller updates a policy | `PUT /api/v1/sellers/return-policy/{id}` | ✅ Done |
| Seller deletes a policy | `DELETE /api/v1/sellers/return-policy/{id}` | ✅ Done |
| Public: live policy for product page | `GET /api/v1/products/{id}/return-policy` | ✅ Done |
| Policy snapshot at checkout (per-product map) | Internal | ✅ Done — `Map<productId, PolicySnapshot>` stored as JSONB |
| Full return/replacement/exchange lifecycle | Multiple | ✅ Done — guards, proof images, Razorpay refund, stock restore |
| Admin: approve return | `PUT /api/v1/admin/{id}/approve-return` | ✅ Done |
| Admin: approve replacement | `PUT /api/v1/admin/{id}/approve-replacement` | ✅ Done |
| Admin: reject return | `PUT /api/v1/admin/{id}/reject-return` | ✅ Done |
| Admin: list pending returns | `GET /api/v1/admin/orders/pending-returns` | ✅ Done |

---

## ✅ PHASE 2 — Notifications & Communication Hub
**Branch:** `feat/in-app-notifications` | **Status: COMPLETE ✅**

| Feature | Status | Notes |
|---------|--------|-------|
| In-app `Notification` entity (V28+V29 migrations) | ✅ Done | `notifications` table, BaseEntity |
| `InAppNotificationService` | ✅ Done | create, list paginated, markAsRead, markAllAsRead |
| `NotificationController` | ✅ Done | `GET /api/v1/notifications`, `PATCH /{id}/read`, `PATCH /read-all` |
| `NotificationRepository` | ✅ Done | paginated, unread count, bulk mark-read |
| `NotificationType` enum | ✅ Done | — |
| `NotificationResponse` + `NotificationListResponse` DTOs | ✅ Done | includes `unreadCount` |
| Return rejection email (`sendReturnRejectedEmail()`) | ✅ Done | `ReturnAdminService.rejectReturn()` fires it |
| SMS stub service (`SmsNotificationService`) | ✅ Done | Twilio stub — mocks to log, ready to wire |

---

## ⬜ PHASE 3 — Analytics & Intelligence
**Branch:** `feat/analytics-dashboard` | **Status: NOT STARTED**

| Feature | Endpoint | Priority |
|---------|----------|----------|
| Admin: Return analytics | `GET /api/v1/admin/analytics/returns` | 🔴 High |
| Seller: Return rate per product | `GET /api/v1/seller/analytics/returns` | 🔴 High |
| Admin: Customer Lifetime Value (CLV) | `GET /api/v1/admin/analytics/customers` | 🟠 Medium |
| Admin: Revenue breakdown by category | `GET /api/v1/admin/analytics/revenue?groupBy=category` | 🟡 Low |

**New DTOs needed:** `ReturnAnalyticsResponse`, `CustomerLifetimeValueDTO`, `CategoryRevenueDTO`
**Migration:** V30 — analytics indexes on `orders` table

---

## ⬜ PHASE 4 — Advanced Commerce Features
**Status: NOT STARTED** *(separate branches per feature)*

### 4A — Product Variants (Branch: `feat/product-variants`)
**Highest architectural impact — changes Cart, Order, and Stock layers.**

- New entity: `ProductVariant` — `id, product, size, color, material, sku, stockQuantity, priceModifier`
- `CartItem` + `OrderItem` get `variantId` field
- Stock deducted from variant, not product
- Exchange makes full semantic sense — swap between variants
- Migration: V30/V31: `product_variants` table

### 4B — Flash Sales (Branch: `feat/flash-sales`)
- `FlashSale` entity: `productId, discountPct, startTime, endTime, maxUnits, usedUnits`
- ShedLock job: activate/deactivate on schedule
- `GET /api/v1/products/flash-sale` — Redis cached (TTL = sale end)

### 4C — Order Tracking Timeline (Branch: `feat/order-tracking`)
- `GET /api/v1/orders/{id}/tracking` — full status timeline with timestamps
- Events: `PLACED → PAID → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED`

### 4D — Loyalty Points (Branch: `feat/loyalty-points`)
- `LoyaltyAccount`: `userId, points, totalEarned, totalRedeemed`
- Earn: 1 point per ₹10 spent (post-delivery, via ShedLock job)
- Redeem: discount at checkout (extends `PromotionEngineService`)
- Points expire after 1 year (ShedLock cleanup job)

### 4E — Return Policy Dispute (Branch: `feat/return-dispute`)
- `ReturnDispute` entity: links orderId + customerId + Cloudinary proof images
- `POST /api/v1/orders/{id}/dispute` — customer appeal after admin rejection
- `GET/PUT /api/v1/admin/disputes/**` — admin resolution

---

## ⬜ PHASE 5 — Platform Hardening
**Branch:** `refactor/platform-hardening` | **Status: NOT STARTED**

| Area | Action | Priority |
|------|--------|----------|
| **CORS** | Replace wildcard `*` with env-specific origins | 🔴 Critical |
| **Actuator** | Restrict `/actuator/**` to ADMIN auth | 🔴 Critical |
| **Request Logging** | MDC filter — `traceId` on every request | 🟠 High |
| **Idempotency Keys** | `Idempotency-Key` header on checkout + payment | 🟠 High |
| **HTTPS** | `server.ssl.*` + HTTP→HTTPS redirect | 🟠 High |
| **HikariCP tuning** | `maximum-pool-size`, `connection-timeout`, `idle-timeout` | 🟡 Medium |
| **Redis TTL audit** | Document all TTLs — currently scattered | 🟡 Medium |
| **API versioning docs** | Formal strategy document for `/api/v1/` → `/api/v2/` path | 🟢 Low |

---

## 🌿 Branch Execution Order

```
main
 └── cleanup/minor-fixes              ← AdminService DTO + delete FileService (45 min)
      └── feat/analytics-dashboard   ← Phase 3
           └── feat/product-variants ← Phase 4A (largest)
                └── feat/flash-sales
                     └── feat/order-tracking
                          └── feat/loyalty-points
                               └── feat/return-dispute
                                    └── refactor/platform-hardening ← Phase 5
```

---

## 📊 Progress Score

| Dimension | v1 (original) | Now (post-Phase 2) | After Phase 4 | After Phase 5 |
|---|---|---|---|---|
| Feature Completeness | 70% | **82%** | 97% | 97% |
| Code Quality | 75% | **85%** | 90% | 95% |
| Production Readiness | 60% | **65%** | 78% | 92% |
| Competitive Differentiation | 80% | **85%** | 97% | 98% |

> **See `MASTER_REFERENCE.md` for the full codebase layer graph (entities, repos, services, controllers, DTOs, migrations, patterns).**
