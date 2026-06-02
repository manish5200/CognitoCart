# CognitoCart — Master Reference Graph + Roadmap
> **Live Scan:** 2026-06-02 | **Stack:** Spring Boot 3.4.1 · Java 21 · PostgreSQL · Redis · RabbitMQ · Razorpay · Cloudinary · pgvector
> **Build Status:** SUCCESS ✅ | **Source Files:** 190+ | **Services:** 34 | **Migrations:** V29 (next = V30)

---

## 📐 PROJECT STRUCTURE

```
com.manish.smartcart
├── config/          ← 16 classes: Security, Redis, RabbitMQ, Razorpay, Swagger, CORS, Rate-limit, ShedLock
├── controller/      ← 15 REST controllers (added NotificationController)
├── dto/             ← 14 sub-packages, ~45 DTOs (added notification/, customer/, event/, error/)
├── enums/           ← 16 enums (added NotificationType, ReturnReason)
├── exception/       ← 3 custom exceptions + GlobalExceptionHandler
├── mapper/          ← 3 mappers (Order, Product, Review)
├── model/           ← 9 sub-packages, ~22 entities (added notification/, base/)
├── repository/      ← 19 repositories + specifications/ (added NotificationRepository)
├── scheduler/       ← 4 scheduled jobs (added ReviewSummarizationScheduler, OrderCleanupScheduler)
├── service/         ← 30 root services + email/ + notifications/ + order/ sub-dirs (34 total)
│   ├── email/       ← EmailTemplateBuilder
│   ├── notifications/ ← InAppNotificationService, OrderNotificationService, OrderRabbitListener, SmsNotificationService
│   └── order/       ← OrderService, OrderQueryService, OrderReturnService, ReturnAdminService (SPLIT ✅)
└── util/            ← AppConstants, JwtUtil, FileValidator, PhoneUtil, VectorAttributeConverter
```

---

## ✅ COMPLETED REFACTORS (vs. Previous Graph)

| Refactor | Status | Evidence |
|---|---|---|
| **Split OrderService God Class** | ✅ DONE | `service/order/` now has 4 focused classes |
| **OrderQueryService extracted** | ✅ DONE | `OrderQueryService.java` — paginated history, 2-query pagination |
| **OrderReturnService extracted** | ✅ DONE | Customer-facing return submission |
| **ReturnAdminService extracted** | ✅ DONE | `approveReturn()`, `approveReplacement()`, `rejectReturn()`, `getPendingReturnRequests()` |
| **ReturnReason enum** | ✅ DONE | `enums/ReturnReason.java` — `DEFECTIVE(true)`, `WRONG_ITEM(true)`, `DAMAGED_IN_TRANSIT(true)`, `CHANGED_MIND(false)`, `NOT_AS_DESCRIBED(false)`, `SIZE_MISMATCH(false)`, `OTHER(false)` + `requiresImageProof()` |
| **IMMUTABLE_STATES bug fixed** | ✅ DONE | `AdminService` now includes `RETURN_REQUESTED`, `REPLACEMENT_REQUESTED`, `EXCHANGE_REQUESTED`, `REPLACEMENT_SHIPPED` |
| **cancelOrder typed exceptions** | ✅ DONE | Now uses `ResourceNotFoundException` + `BusinessLogicException` |
| **In-app Notification system** | ✅ DONE | `Notification` entity, `NotificationRepository`, `InAppNotificationService`, `NotificationController`, `NotificationType` enum |
| **SMS Stub service** | ✅ DONE | `SmsNotificationService` — Twilio stub, ready for integration |
| **Admin reject-return endpoint** | ✅ DONE | `PUT /api/v1/admin/{id}/reject-return` via `ReturnAdminService.rejectReturn()` |
| **Admin pending-returns endpoint** | ✅ DONE | `GET /api/v1/admin/orders/pending-returns` via `ReturnAdminService.getPendingReturnRequests()` |
| **Notifications V28+V29 migrations** | ✅ DONE | `V28__create_notifications_table.sql`, `V29__add_audit_columns_to_notifications.sql` |
| **Return proof images migration** | ✅ DONE | `V26__add_return_proof_images_to_orders.sql` |
| **ReturnReason check constraint** | ✅ DONE | `V27__add_return_reason_check_constraint.sql` |
| **Return policy per-product snapshot** | ✅ DONE | Multi-product map: `Map<Long, PolicySnapshot>` serialized to JSONB |
| **OrderCleanupScheduler** | ✅ DONE | Cancels stale PAYMENT_PENDING orders |
| **ReviewSummarizationScheduler** | ✅ DONE | AI review summaries on schedule |
| **Prometheus metrics** | ✅ DONE | `cognitocart.orders.placed` counter in `OrderService` |
| **OrderRabbitListener** | ✅ DONE | Consumes cart.abandonment + wishlist.sale RabbitMQ events |

### ⚠️ STILL OPEN (not yet done)
| # | Issue | Location | Impact |
|---|---|---|---|
| 1 | `changeTheStatusOfOrders()` returns `Order` entity not `OrderResponse` DTO | `AdminService.java:107` | Entity leak — low security risk but violates layering |
| 2 | `FileService.java` still exists (dead code, 1.3KB) | `service/FileService.java` | Confuses readers — Cloudinary is live |
| 3 | `OrderService` still uses `@AllArgsConstructor` | `service/order/OrderService.java:39` | All others use `@RequiredArgsConstructor` |
| 4 | `cancelOrder` refund failure throws raw `RuntimeException` | `OrderService.java:303` | Should be `BusinessLogicException` |

---

## LAYER 1 — ENUMS (16 total)

| Enum | Values | Notes |
|------|--------|-------|
| `OrderStatus` | `CREATED, PAYMENT_PENDING, PAID, CONFIRMED, PACKED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED, RETURN_REQUESTED, REPLACEMENT_REQUESTED, EXCHANGE_REQUESTED, RETURNED, REFUNDED, REPLACEMENT_SHIPPED` | Full state machine |
| `PaymentStatus` | `PENDING, PAID, FAILED, REFUNDED` | — |
| `ReturnType` | `RETURN, REPLACEMENT, EXCHANGE` | — |
| `ReturnReason` | `DEFECTIVE(imageRequired), WRONG_ITEM(imageRequired), DAMAGED_IN_TRANSIT(imageRequired), CHANGED_MIND, NOT_AS_DESCRIBED, SIZE_MISMATCH, OTHER` | ✅ NEW — type-safe, replaces magic strings |
| `NotificationType` | (notification bell categories) | ✅ NEW |
| `PolicyType` | `RETURN_AND_EXCHANGE, RETURN_ONLY, EXCHANGE_ONLY, REPLACEMENT_ONLY, NON_RETURNABLE` | — |
| `KycStatus` | `PENDING, IN_REVIEW, VERIFIED, REJECTED, SUSPENDED` | — |
| `Role` | `ROLE_CUSTOMER, ROLE_SELLER, ROLE_ADMIN` | — |
| `AuthProvider` | `LOCAL, GOOGLE` | — |
| `CartStatus` | `ACTIVE, CHECKED_OUT` | — |
| `DiscountType` | `PERCENTAGE, FLAT` | — |
| `DlqStatus` | `PENDING, RESOLVED, REPLAYED` | — |
| `ErrorCode` | custom error codes | — |
| `Gender` | `MALE, FEMALE, OTHER` | — |
| `ProductStatus` | `ACTIVE, INACTIVE` | — |
| `ShipmentStatus` | `IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, RETURNED, FAILED` | — |

---

## LAYER 2 — MODELS (Entities)

### model/user/
| Entity | Table | Key Fields | Notes |
|--------|-------|-----------|-------|
| `Users` | `users` | `id, email, password, role, fullName, authProvider, emailVerified, isActive, passwordChangedAt` | `@SoftDelete(is_deleted)` |
| `Address` | `addresses` | `id, user→Users, fullName, phone, streetAddress, city, state, zipCode, country, isDefault` | `@SoftDelete(is_deleted)` |
| `SellerProfile` | `seller_profiles` | `id, user→Users, businessName, storeName, kycStatus, gstin, panNumber` | — |
| `CustomerProfile` | `customer_profiles` | `id, user→Users, gender, dateOfBirth` | — |
| `Wishlist` | `wishlists` | `id, user→Users, product→Product` | — |

### model/product/
| Entity | Table | Key Fields | Notes |
|--------|-------|-----------|-------|
| `Product` | `products` | `id, productName, slug, description, price, stockQuantity, category→Category, seller→Users, status, averageRating, reviewCount, embedding(float[]), sku` | `@SoftDelete(is_deleted)` |
| `Category` | `categories` | `id, name, description, parentCategory→Category` | `@SoftDelete(is_deleted)` |
| `ProductReturnPolicy` | `product_return_policy` | `id, product→Product(EAGER), category→Category(EAGER), policyType, returnWindowDays, returnAllowed, exchangeAllowed, replacementAllowed, pickupAvailable` | ⚠️ Must use EAGER — `@SoftDelete` on Product/Category blocks LAZY |
| `ProductInsights` | `product_insights` | `id, product→Product, views, addToCartCount, purchaseCount` | — |

### model/order/
| Entity | Table | Key Fields | Notes |
|--------|-------|-----------|-------|
| `Order` | `orders` | `id, user, orderItems, orderDate, totalAmount, couponCode, discountAmount, deliveryFee, orderStatus, paymentStatus, razorpayOrderId, razorpayPaymentId, razorpaySignature, shipping*(snapshot), returnReason(ReturnReason enum), returnDescription, returnRequestedAt, deliveredAt, returnPolicySnapshot(JSONB Map), returnRequestType(ReturnType), returnProofImages` | Policy snapshot = `Map<productId, PolicySnapshot>` |
| `OrderItem` | `order_items` | `id, order, product, quantity, priceAtPurchase` | — |
| `Shipment` | `shipments` | `id, order, courierName, trackingNumber, trackingUrl, estimatedDeliveryDate, dispatchedBy, shipmentStatus` | — |
| `Coupon` | `coupons` | `id, code, discountType, discountValue, minOrderAmount, maxUses, usedCount, active, expiryDate` | `@SoftDelete(is_deleted)` |
| `UserCouponUsage` | `user_coupon_usage` | `id, user, coupon, usage` | — |

### model/cart/
| Entity | Table | Key Fields |
|--------|-------|-----------|
| `Cart` | `carts` | `id, user, cartItems, couponCode, discountAmount, deliveryFee, cartStatus` |
| `CartItem` | `cart_items` | `id, cart, product, quantity, priceAtAdding` |
| `GuestCart` | Redis | `sessionId(key), items, couponCode` |
| `GuestCartItem` | — | `productId, quantity` |

### model/feedback/
| Entity | Table |
|--------|-------|
| `Review` | `reviews` — `id, user, product, rating, comment, verified(purchased)` |

### model/notification/ ✅ NEW
| Entity | Table | Key Fields |
|--------|-------|-----------|
| `Notification` | `notifications` | `id, user→Users, type(NotificationType), title, message, isRead` — extends `BaseEntity` |

### model/admin/
| Entity | Table |
|--------|-------|
| `FailedWebhookEvent` | `failed_webhook_events` — `id, eventType, payload, status(DlqStatus), retryCount, lastError` |

### model/
| Entity | Table |
|--------|-------|
| `RefreshToken` | `refresh_tokens` — `id, user, token, expiresAt` |

---

## LAYER 3 — REPOSITORIES (19 total)

| Repository | Key Custom Methods | Notes |
|------------|-------------------|-------|
| `UsersRepository` | `findByEmail()` | — |
| `AddressRepository` | `findByUserId()`, `findDefaultByUserId()` | — |
| `ProductRepository` | `findBySlug()`, `findByIdForUpdate()`(pessimistic lock), `findByCategoryId()`, semantic vector search, `findToSellingProducts()`, `findByStockQuantityLessThan()`, `getSku()` | — |
| `CategoryRepository` | `findByName()` | — |
| `CartRepository` | `findByUserId()`, `findActiveCartByUserId()` | — |
| `CartItemRepository` | `findByCartAndProduct()` | — |
| `OrderRepository` | `findOrderIdsByUserId(Pageable)`, `findOrdersWithItemsByIds()`, `findByIdWithItems()`, `findByOrderStatusInWithItems()`, `calculateRevenue()`, `countByOrderStatus()`, `getDailyRevenueTrend()` | Two-query pagination pattern |
| `ShipmentRepository` | `findByOrder_Id()`, `findByTrackingNumberWithOrderAndUser()` | — |
| `ProductReturnPolicyRepository` | `findByProduct_Id()`, `findByCategory_Id()`, `findAllByProductSellerId()`, `findByIdAndProductSellerId()` | — |
| `ReviewRepository` | `findByProductId()`, `findRatingDistribution()` | — |
| `CouponRepository` | `findByCode()`, `findActiveByCode()` | — |
| `UserCouponUsageRepository` | `findByUserIdAndCouponId()` | — |
| `SellerProfileRepository` | `findByUserId()`, `findByKycStatus()`, `findAllWithUser()`, `findByKycStatusIn()` | — |
| `FailedWebhookEventRepository` | `findByStatus()` | — |
| `WishlistRepository` | `findByUserIdAndProductId()`, `findAllByUserId()` | — |
| `RefreshTokenRepository` | `findByToken()`, `deleteByUser()` | — |
| `ProductInsightsRepository` | `findByProductId()` | — |
| `NotificationRepository` | `findByUserIdOrderByCreatedAtDesc(Pageable)`, `countByUserIdAndIsReadFalse()`, `markAllAsReadForUser()` | ✅ NEW |
| `GuestCartRepository` | Redis-backed (not JPA) | Spring Data warns on startup — safe to ignore |

---

## LAYER 4 — SERVICES (34 total)

### service/order/ (Split Completed ✅)
| Service | Responsibility | Key Methods |
|---------|---------------|-------------|
| `OrderService` | Checkout only | `placeOrder()`, `cancelOrder()`, `cancelAndReleaseStock()` — 332 lines |
| `OrderQueryService` | Read queries | `getOrderHistoryForUser(Pageable)` — two-query pagination |
| `OrderReturnService` | Customer return submission | `requestReturn()` — full guard chain |
| `ReturnAdminService` | Admin return decisions | `approveReturn()`, `approveReplacement()`, `rejectReturn()`, `getPendingReturnRequests()` |

### Core Business Services
| Service | Key Methods | Notes |
|---------|------------|-------|
| `CartService` | `addToCart()`, `removeFromCart()`, `applyDiscount()`, `mergeGuestCart()`, `getCartForUser()`, `clearTheCart()` | — |
| `ProductService` | `createProduct()`, `updateProduct()`, `deleteProduct()`, `searchProducts()`, `getBySlug()` | — |
| `ReviewService` | `addReview()`, `getReviews()`, `getRatingDistribution()` | Verified purchase check, optimistic locking + Spring Retry |
| `AuthService` | `register()`, `login()`, `verifyEmail()`, `refreshToken()`, `logout()` | — |
| `AdminService` | `getAdminStats()`, `changeTheStatusOfOrders()` ⚠️ returns `Order`, `getAllSellers()`, `getPendingKycSellers()`, `updateSellerKyc()` | IMMUTABLE_STATES now includes return states |
| `SellerService` | `getSellerDashboard()` | — |
| `CouponService` | `createCoupon()`, `applyCoupon()`, `incrementUsage()`, `getCouponByCode()`, `toggleActive()` | — |
| `ReturnPolicyService` | `getPolicySnapshotForCheckout()`, `getLivePolicyForProduct()`, `getLivePolicyResponse()`, `createPolicy()`, `updatePolicy()`, `deletePolicy()`, `getMyPolicies()` | Chain: product→category→NON_RETURNABLE |

### Payment & Refund Services
| Service | Key Methods | Notes |
|---------|------------|-------|
| `PaymentService` | `createRazorpayOrder()` | Creates Razorpay order for checkout |
| `RazorpayRefundService` | `initiateFullRefund(paymentId, amount)` | ⚠️ Requires empty `JSONObject` — SDK 1.4.x quirk |
| `WebhookProcessingService` | `processPaymentCaptured()`, `processPaymentFailed()` | Handles Razorpay webhooks |
| `WebhookDlqService` | `getPendingFailures()`, `replayFailedWebhook()` | Dead-letter queue management |

### Shipping Services
| Service | Key Methods | Notes |
|---------|------------|-------|
| `ShipmentService` | `attachShipmentAndShip()`, `processLogisticsUpdate()` | Sets `deliveredAt` on DELIVERED; idempotency guard; terminal state guard |

### Auth & Security Services
| Service | Key Methods |
|---------|------------|
| `OtpService` | `generateOtp()`, `verifyOtp()` — Redis TTL 15min |
| `PasswordResetService` | `initiateReset()`, `resetPassword()` — Redis token 15min |
| `RefreshTokenService` | `createToken()`, `validateToken()`, `deleteByUser()` |
| `TokenBlacklistService` | `blacklist(token)`, `isBlacklisted()` — Redis |

### Notification Services (service/notifications/)
| Service | Key Methods | Notes |
|---------|------------|-------|
| `InAppNotificationService` | `createNotification()`, `getUserNotifications(Pageable)`, `markAsRead()`, `markAllAsRead()` | ✅ NEW — full in-app bell |
| `OrderNotificationService` | `sendEmailNotification()`, `sendStatusUpdateEmail()`, `sendRefundEmail()`, `sendInvoiceEmail(@Async)`, `sendDeliveryConfirmationEmail()`, `sendReturnRejectedEmail()` | — |
| `SmsNotificationService` | `sendSms(phone, message)` | ✅ NEW — Twilio stub, mocks to log |
| `OrderRabbitListener` | RabbitMQ consumer for cart abandonment + wishlist sale | — |

### Utility & Infrastructure Services
| Service | Key Methods |
|---------|------------|
| `EmailService` | `sendMail()`, `sendMailWithAttachment()` |
| `InvoiceService` | `generateInvoicePdf(order)` — iText7 |
| `CloudinaryService` | `upload()`, `delete()` |
| `EmbeddingService` | `generateEmbedding(text)` — pgvector |
| `AiSummarizationService` | `summarize(reviews)` |
| `PromotionEngineService` | `applyPromotion(cart)` |
| `CustomerService` | Profile management |
| `AddressService` | CRUD + default address |
| `WishlistService` | Add/remove/list |
| `GuestCartService` | Redis-backed pre-login cart |
| `SellerAnalyticsExportService` | Streaming CSV export |
| `CategoryService` | CRUD categories |
| `FileService` | ⚠️ DEAD CODE — delete this |

---

## LAYER 5 — CONTROLLERS (15 total)

| Controller | Base Path | Auth | Key Endpoints |
|------------|-----------|------|---------------|
| `AuthController` | `/api/v1/auth` | Public | `POST /register/customer`, `/register/seller`, `/login`, `/verify-email`, `/resend-otp`, `/refresh-token`, `/logout`, `/forgot-password`, `/reset-password` |
| `OrderController` | `/api/v1/orders` | JWT | `POST /checkout`, `GET /history`, `PUT /{id}/cancel`, `POST /{id}/request-return` |
| `AdminController` | `/api/v1/admin` | ADMIN | `GET /stats`, `PATCH /{id}/status`, `POST /coupons`, `GET /coupons`, `PATCH /coupons/{id}/toggle`, `POST /{id}/shipment`, `GET /webhooks/dlq/pending`, `POST /webhooks/dlq/{id}/replay`, `GET /sellers`, `GET /sellers/kyc/pending`, `PATCH /sellers/{id}/kyc`, `PUT /{id}/approve-return`, `PUT /{id}/approve-replacement`, `PUT /{id}/reject-return` ✅, `GET /orders/pending-returns` ✅ |
| `ProductController` | `/api/v1/products` | Mixed | `GET /` (public), `GET /search` (public), `GET /{slug}` (public), `POST /` (SELLER), `PUT /{id}`, `DELETE /{id}`, `POST /{id}/image`, `GET /semantic` (public), `GET /{id}/return-policy` (public) |
| `PaymentController` | `/api/v1/payments` | Mixed | `POST /create-order`(JWT), `POST /verify`(Public), `POST /webhook`(Public) |
| `CartController` | `/api/v1/cart` | JWT | `POST /add`, `DELETE /remove/{itemId}`, `GET /`, `POST /apply-coupon`, `DELETE /remove-coupon`, `POST /merge-guest` |
| `ReviewController` | `/api/v1/reviews` | Mixed | `POST /` (JWT), `GET /product/{id}` (public), `GET /product/{id}/distribution` (public), `PUT /{id}`, `DELETE /{id}` |
| `AddressController` | `/api/v1/addresses` | JWT | CRUD + set default |
| `CategoryController` | `/api/v1/categories` | Mixed | `GET /` (public), `POST /`(ADMIN), `PUT /{id}`(ADMIN), `DELETE /{id}`(ADMIN) |
| `WishlistController` | `/api/v1/wishlist` | JWT | Add, remove, list |
| `SellerController` | `/api/v1/sellers` | SELLER | Dashboard, analytics CSV export, `POST /return-policy`, `GET /return-policy`, `PUT /return-policy/{id}`, `DELETE /return-policy/{id}` |
| `CustomerController` | `/api/v1/customer` | JWT | Profile view/update |
| `GuestCartController` | `/api/v1/guest-cart` | Public | Add, remove, list |
| `LogisticsWebhookController` | `/api/v1/webhooks/logistics` | Public | `POST /` — carrier status push |
| `NotificationController` | `/api/v1/notifications` | JWT | `GET /` (paginated + unread count) ✅, `PATCH /{id}/read` ✅, `PATCH /read-all` ✅ |

---

## LAYER 6 — DTOs

### dto/order/ (9 files)
| DTO | Direction | Key Fields |
|-----|-----------|-----------|
| `OrderRequest` | IN | `shippingAddressId`, `shippingAddress` (inline) |
| `OrderResponse` | OUT | `orderId, email, customerName, orderDate, totalAmount, couponCode, discountAmount, deliveryFee, status, paymentStatus, shippingAddress, shipmentTracking, razorpayOrderId, items, returnRequestType, returnReason, returnRequestedAt` |
| `ReturnRequestDTO` | IN | `returnType(@NotNull ReturnType)`, `returnReason(@NotNull ReturnReason)`, `returnDescription(@Size max=500, optional)` |
| `PolicySnapshot` | Internal | `policyType, returnWindowDays, returnAllowed, exchangeAllowed, replacementAllowed, pickupAvailable` |
| `ShipmentRequest` | IN | `courierName, trackingNumber, trackingUrl, estimatedDeliveryDate, dispatchedBy` |
| `ShipmentTrackingDTO` | OUT | `courierName, trackingNumber, trackingUrl, estimatedDeliveryDate` |
| `ShippingAddressRequest` | IN | address fields |
| `PaymentVerificationRequest` | IN | `razorpayPaymentId, razorpayOrderId, razorpaySignature` |
| `PromotionResult` | Internal | `discountAmount, deliveryFee` |

### dto/notification/ (2 files) ✅ NEW
| DTO | Direction |
|-----|-----------|
| `NotificationResponse` | OUT — `id, type, title, message, isRead, createdAt` |
| `NotificationListResponse` | OUT — `unreadCount, notifications(Page<NotificationResponse>)` |

### Other DTO sub-packages
| Package | Files |
|---------|-------|
| `dto/auth/` | `AuthRequest, CustomerAuthRequest, SellerAuthRequest, LoginRequest, LoginResponse, RegisterResponse, ForgotPasswordRequest, ResetPasswordRequest, TokenRefreshRequest, TokenRefreshResponse, VerifyEmailRequest` |
| `dto/product/` | `ProductRequest(IN), ProductResponse(OUT), ProductSearchDTO, WishlistSummaryDTO, ReturnPolicyRequest(IN), ReturnPolicyResponse(OUT)` |
| `dto/cart/` | `CartRequest(IN), CartResponse(OUT)` |
| `dto/coupon/` | `CouponRequest(IN), CouponResponse(OUT)` |
| `dto/feedback/` | `ReviewRequestDTO(IN), ReviewResponseDTO(OUT), RatingDistributionDTO(OUT)` |
| `dto/seller/` | `SellerDashboardResponse(OUT), SellerSummaryResponse(OUT)` |
| `dto/admin/` | `DashboardResponse(OUT), DailyRevenueDTO, LowStockResponse, StatusChangeRequest(IN), TopProductDTO, KycUpdateRequest(IN)` |
| `dto/webhook/` | `LogisticsWebhookRequest(IN)` |

---

## LAYER 7 — MAPPERS (3 total)

| Mapper | Maps | Key Methods |
|--------|------|-------------|
| `OrderMapper` | `Order → OrderResponse` | `toOrderResponse(order)`, `mapShipment(response, shipment)` — maps all return fields |
| `ProductMapper` | `Product → ProductResponse` | `toProductResponse(product)` |
| `ReviewMapper` | `Review → ReviewResponseDTO` | `toDto(review)` |

---

## LAYER 8 — CONFIG (18 total)

| Config Class | Purpose |
|-------------|---------|
| `SecurityConfig` | JWT stateless, OAuth2 Google, CORS wildcard (dev), rate-limit + JWT filter chain |
| `RedisConfig` | `StringRedisTemplate`, `RedisTemplate<String,Object>` |
| `RabbitMQConfig` | Queues: `cart.abandonment.queue`, `wishlist.sale.queue` — TTL, DLQ, exchange bindings |
| `RazorpayConfig` | `RazorpayClient` bean |
| `CloudinaryConfig` | `Cloudinary` bean |
| `SwaggerConfig` | Bearer auth, OpenAPI metadata |
| `ShedLockConfig` | Distributed lock provider |
| `SchedulerConfig` | `@EnableScheduling` |
| `AuditorAwareImpl` | `BaseEntity` auditing — `createdBy`, `modifiedBy` from `SecurityContext` |
| `OAuth2LoginSuccessHandler` | Google login → find/create user → issue JWT → redirect with token |
| `RateLimitFilter` | Bucket4j + Redis — 60 req/min per IP |
| `RateLimitConfig` | Bucket4j configuration |
| `JwtFilter` | Validates `Authorization: Bearer <token>` |
| `LoggingCacheManager` | Wraps cache puts/evicts with log |
| `WebConfig` | CORS for MVC |
| `CustomUserDetails` | Spring Security principal wrapper |
| `CustomUserDetailsService` | Loads user for auth |
| `config/filter/` | Additional request filters |
| `config/initializer/` | App startup hooks |

---

## LAYER 9 — SECURITY RULES

```
PUBLIC (no token):
  /api/v1/auth/**                 ← all auth flows
  /api/v1/guest-cart/**           ← pre-login shopping
  /api/v1/payments/verify         ← Razorpay frontend callback
  /api/v1/payments/webhook        ← Razorpay backend webhook
  /api/v1/webhooks/logistics      ← carrier status push
  /oauth2/**, /login/oauth2/**    ← Google OAuth2 flow
  /swagger-ui/**, /v3/api-docs/** ← API docs
  /actuator/**                    ← health + prometheus ⚠️ should restrict in prod

PUBLIC (GET only):
  /api/v1/products, /api/v1/products/{slug}, /api/v1/products/semantic
  /api/v1/categories
  /api/v1/reviews/**
  /api/v1/products/{id}/return-policy

AUTHENTICATED (any JWT role):
  /api/v1/notifications/**        ← notification bell
  Everything else

ADMIN only (@PreAuthorize on AdminController):
  /api/v1/admin/**

SELLER only:
  /api/v1/sellers/**
```

---

## LAYER 10 — SCHEDULERS (4 total)

| Scheduler | Trigger | Job | ShedLock |
|-----------|---------|-----|---------|
| `CartAbandonmentJob` | Cron | Finds abandoned carts → sends email via RabbitMQ | ✅ |
| `WishlistConversionScheduler` | Cron | Price drop alert → sends wishlist email via RabbitMQ | ✅ |
| `ReviewSummarizationScheduler` | Cron | AI summarizes product reviews | ✅ NEW |
| `OrderCleanupScheduler` | Cron | Cancels stale `PAYMENT_PENDING` orders via `cancelAndReleaseStock()` | ✅ NEW |

---

## LAYER 11 — DATABASE MIGRATIONS (V29 complete, next = V30)

| Version | File | What It Does |
|---------|------|-------------|
| V1 | `initial_schema.sql` | Core tables: users, products, categories, orders, order_items, cart, cart_items, addresses |
| V2 | `security_and_profiles.sql` | seller_profiles, customer_profiles, refresh_tokens, wishlists |
| V3 | `update_kyc_status.sql` | KycStatus enum migration |
| V4 | `performance_indexes.sql` | All performance indexes |
| V5 | `add_coupon_system.sql` | coupons, user_coupon_usage |
| V6 | `add_coupon_audit_columns.sql` | BaseEntity columns on coupon_usage |
| V7 | `add_razorpay_fields.sql` | razorpay_order_id, payment_id, signature |
| V8 | `upgrade_coupons_and_delivery.sql` | delivery_fee, coupon improvements |
| V9 | `add_base_entity_columns_to_coupon_usage.sql` | BaseEntity on coupon_usage |
| V10 | `add_payment_status.sql` | payment_status column |
| V11 | `add_password_changed_at.sql` | password_changed_at |
| V12 | `add_email_verified.sql` | email_verified |
| V13 | `add_shipments_table.sql` | shipments table |
| V14 | `add_failed_webhook_events.sql` | failed_webhook_events (DLQ) |
| V15 | `add_soft_deletes.sql` | is_deleted on users, products, categories, addresses, coupons |
| V16 | `advanced_promotions.sql` | promotion rules |
| V18 | `wishlist_notifications.sql` | wishlist table |
| V19 | `add_pgvector.sql` | pgvector extension + embedding column |
| V20 | `resize_embedding.sql` | resize embedding dimension |
| V21 | `create_product_insights.sql` | product_insights table |
| V22 | `add_shedlock_table.sql` | shedlock distributed lock table |
| V23 | `prepare_for_oauth2.sql` | auth_provider, oauth2_id |
| V24 | `create_product_return_policy.sql` | product_return_policy + CHECK constraint |
| V25 | `add_return_fields_to_orders.sql` | return_reason, return_policy_snapshot(JSONB), return_request_type, delivered_at |
| V26 | `add_return_proof_images_to_orders.sql` | return_proof_images column ✅ NEW |
| V27 | `add_return_reason_check_constraint.sql` | DB-level enum constraint on return_reason ✅ NEW |
| V28 | `create_notifications_table.sql` | notifications table ✅ NEW |
| V29 | `add_audit_columns_to_notifications.sql` | BaseEntity cols on notifications ✅ NEW |

> **⚡ NEXT MIGRATION = V30**

---

## LAYER 12 — EMAIL TEMPLATES (10 templates)

| Template File | Trigger | Builder Method |
|---------------|---------|----------------|
| `welcome-email.html` | Registration | `buildWelcomeEmail()` |
| `email-verification.html` | OTP | `buildEmailVerificationEmail()` |
| `order-confirmation.html` | Checkout success | `buildOrderConfirmation()` |
| `order-status.html` | Any status change | `buildOrderStatusUpdate()`, `buildDeliveryConfirmationEmail()` |
| `refund-processed.html` | Razorpay refund | `buildRefundEmail()` |
| `seller-kyc.html` | KYC decision | `buildSellerKycDecisionEmail()` |
| `password-reset.html` | Forgot password | `buildPasswordResetEmail()` |
| `password-changed.html` | Security alert | `buildPasswordChangedEmail()` |
| `cart-abandonment.html` | Scheduled job | `buildCartAbandonmentEmail()` |
| `wishlist-sale.html` | Price drop | `buildWishlistSaleEmail()` |

**`order-status.html` status coverage:** `CONFIRMED, PACKED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED, RETURN_REQUESTED, REPLACEMENT_REQUESTED, EXCHANGE_REQUESTED, REPLACEMENT_SHIPPED, RETURNED, REFUNDED` ✅

> ⚠️ `sendReturnRejectedEmail()` exists in `OrderNotificationService` — verify the template is wired correctly.

---

## LAYER 13 — KEY PATTERNS & CONSTRAINTS

### Design Patterns In Use
| Pattern | Where | Description |
|---------|-------|-------------|
| Snapshot / Frozen fields | `Order.priceAtPurchase`, `Order.returnPolicySnapshot` (Map), `Order.shipping*` | Data frozen at checkout |
| Chain of Responsibility | `ReturnPolicyService` | Product policy → Category policy → NON_RETURNABLE |
| Pessimistic Lock | `ProductRepository.findByIdForUpdate()` | Prevents oversell on concurrent checkout |
| Anti-Corruption Layer | `ShipmentService.mapShipmentStatusToOrderStatus()` | Isolates carrier vocab |
| Dead-Letter Queue | `FailedWebhookEvent` + `WebhookDlqService` | Failed Razorpay webhooks stored for admin replay |
| Idempotency Guard | `ShipmentService.processLogisticsUpdate()` | Carrier retries safely ignored |
| Terminal State Guard | `ShipmentService.isTerminalState()` + `AdminService.IMMUTABLE_STATES` | Multi-layer state protection |
| Two-Query Pagination | `OrderQueryService.getOrderHistoryForUser()` | Page IDs first, then fetch data |
| Optimistic Locking + Retry | `ReviewService` | Concurrent review updates handled safely |
| Prometheus Metrics | `OrderService` | `cognitocart.orders.placed` counter |

### Known Constraints / Gotchas
| # | Constraint | Detail |
|---|-----------|--------|
| 1 | `@SoftDelete` + `LAZY` crash | Hibernate 6: `@ManyToOne(LAZY)` on entity with `@SoftDelete` = `UnsupportedMappingException`. **Must use EAGER** on `ProductReturnPolicy.product` and `.category` |
| 2 | Razorpay SDK 1.4.x | `initiateFullRefund()` requires empty `JSONObject` as 2nd arg — not null |
| 3 | `GuestCartRepository` not JPA | Redis-backed — Spring Data warns on startup (safe to ignore) |
| 4 | RabbitMQ queues | `cart.abandonment.queue`, `wishlist.sale.queue` must exist before consumers start |
| 5 | Policy snapshot map | `Map<Long productId, PolicySnapshot>` — multi-product orders each get their own policy |
| 6 | `AdminService.changeTheStatusOfOrders()` | Returns `Order` entity (not DTO) — leaks entity to controller |

### AppConstants
| Constant | Value |
|----------|-------|
| `LOW_STOCK_THRESHOLD` | `5` |
| `MAXIMUM_COUPON_DISCOUNT` | `100.0` |
| `MAX_FILE_SIZE` | `5MB` |
| `ALLOWED_EXTENSIONS` | `jpg, jpeg, png, webp` |
| `INVOICE_DATE_FORMAT` | `dd MMM yyyy, hh:mm a` |

---

## 🗺️ FEATURE STATUS TRACKER

| Feature | Status | Branch | Notes |
|---------|--------|--------|-------|
| User Auth (JWT + refresh + OTP + OAuth2) | ✅ Complete | — | Google OAuth2, email verification |
| Product CRUD + Image Upload | ✅ Complete | — | Cloudinary, slug-based GET |
| Category Management | ✅ Complete | — | Soft-delete, nested categories |
| Cart (Guest + Auth) | ✅ Complete | — | Redis guest cart, merge on login |
| Coupon System | ✅ Complete | — | Per-user + global limits |
| Checkout + Razorpay Payment | ✅ Complete | — | Order → Razorpay → verify signature |
| Order History + Cancel | ✅ Complete | — | DELIVERED/SHIPPED = no cancel |
| Shipment + Carrier Webhook | ✅ Complete | — | AWB tracking, idempotency guard |
| Wishlist + Price Drop Email | ✅ Complete | — | RabbitMQ triggered |
| Cart Abandonment Email | ✅ Complete | — | ShedLock + RabbitMQ |
| Admin Dashboard + KYC | ✅ Complete | — | Top sellers, low stock, KYC FSM |
| Seller Dashboard + CSV Export | ✅ Complete | — | Streaming CSV |
| Reviews + Rating Distribution | ✅ Complete | — | Verified purchase + optimistic lock |
| AI Semantic Search | ✅ Complete | — | pgvector + EmbeddingService |
| AI Review Summarization | ✅ Complete | — | Scheduled job |
| PDF Invoice Email | ✅ Complete | — | iText7, async |
| Webhook DLQ + Replay | ✅ Complete | — | Admin can replay failed Razorpay webhooks |
| Return / Replacement / Exchange | ✅ Complete | `feat/returns` | Full policy matrix, refund, stock restore |
| Seller Return Policy Config | ✅ Complete | `feat/seller-return-policy` | Full CRUD on `/api/v1/sellers/return-policy` |
| Admin: Reject Return | ✅ Complete | `feat/returns` | Resets to DELIVERED + rejection email |
| Admin: List Pending Returns | ✅ Complete | `feat/returns` | `GET /api/v1/admin/orders/pending-returns` |
| Public: Product Return Policy Badge | ✅ Complete | `feat/seller-return-policy` | Live policy for product page |
| **In-App Notification System** | ✅ Complete | `feat/in-app-notifications` | Bell icon, paginated, mark-read |
| **SMS Stub Service** | ✅ Complete | — | Twilio stub wired, mocks to log |
| **ReturnReason Enum** | ✅ Complete | `refactor/return-reason-enum` | `requiresImageProof()` built-in |
| **OrderService Split (SRP)** | ✅ Complete | `refactor/split-order-service` | 4 focused classes in service/order/ |
| **IMMUTABLE_STATES bug** | ✅ Complete | — | Return states now protected |
| **Order Cleanup Scheduler** | ✅ Complete | — | Stale PAYMENT_PENDING orders auto-cancelled |
| **Review Summarization Scheduler** | ✅ Complete | — | AI job with ShedLock |
| **Return proof images** | ✅ Complete | — | V26 migration, stored in Order |
| Product Variants | ⬜ Not Started | `feat/product-variants` | Phase 4A |
| Flash Sales | ⬜ Not Started | `feat/flash-sales` | Phase 4B |
| Order Tracking Timeline | ⬜ Not Started | `feat/order-tracking` | Phase 4C |
| Loyalty Points | ⬜ Not Started | `feat/loyalty-points` | Phase 4D |
| Return Policy Dispute | ⬜ Not Started | `feat/return-dispute` | Phase 4E |
| Platform Hardening | ⬜ Not Started | `refactor/platform-hardening` | Phase 5 |

---

## 📋 REMAINING ROADMAP (Phases 3–5)

### Immediate Cleanup (Do before Phase 3)

| Task | File | Effort |
|------|------|--------|
| `AdminService.changeTheStatusOfOrders()` → return `OrderResponse` not `Order` | `AdminService.java:107` + `AdminController` | 30 min |
| Delete `FileService.java` | `service/FileService.java` | 5 min |
| `OrderService` → change `@AllArgsConstructor` to `@RequiredArgsConstructor` | `service/order/OrderService.java:39` | 5 min |
| `cancelOrder` refund failure → `BusinessLogicException` | `service/order/OrderService.java:303` | 5 min |

---

### 📋 PHASE 3 — Analytics & Intelligence
**Branch:** `feat/analytics-dashboard`
*Separates a shopping cart from a platform.*

#### 3A — Admin Return Analytics
```
GET /api/v1/admin/analytics/returns
→ totalReturnRequests, approvalRate, topReturnReasons[], avgResolutionDays, returnsByCategory[]
```
- Query: `OrderRepository` JPQL aggregate over return-status orders
- New DTO: `ReturnAnalyticsResponse`

#### 3B — Seller Return Rate per Product
```
GET /api/v1/seller/analytics/returns
→ productId, productName, totalOrders, totalReturns, returnRate%
```

#### 3C — Customer Lifetime Value (CLV)
```
GET /api/v1/admin/analytics/customers
→ topCustomers[]: userId, name, totalOrders, totalSpent, avgOrderValue
```

#### 3D — Revenue Breakdown by Category
- Extend existing `dailyRevenueTrend` with category dimension
- `GET /api/v1/admin/analytics/revenue?groupBy=category`

**Migration needed:** V30 — may need analytics views/indexes for performance

---

### 📋 PHASE 4 — Advanced Commerce Features

#### 4A — Product Variants (Branch: `feat/product-variants`)
**The single largest missing feature vs real e-commerce.**

**New entity:** `ProductVariant` — `id, product→Product, size, color, material, sku, stockQuantity, priceModifier`

| Impact Area | Change |
|-------------|--------|
| `CartItem` | Add `variantId` field |
| `OrderItem` | Add `variantId` field |
| Checkout | Deduct from variant stock, not product stock |
| Exchange | Now makes full semantic sense — swap between variants |
| Product API | `GET /api/v1/products/{id}/variants` |
| Migration | V30: `product_variants` table |

#### 4B — Flash Sales / Time-limited Deals (Branch: `feat/flash-sales`)
- `FlashSale` entity: `productId, discountPercentage, startTime, endTime, maxUnits, usedUnits`
- ShedLock job: activate/deactivate based on time window
- `GET /api/v1/products/flash-sale` — public, Redis cached (TTL = sale end)
- Migration: V31: `flash_sales` table

#### 4C — Order Tracking Timeline (Branch: `feat/order-tracking`)
- `GET /api/v1/orders/{id}/tracking` — full shipment timeline
- Timeline events: `PLACED → PAID → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED`
- Each event with `timestamp` — new `OrderEvent` entity or query from order status history

#### 4D — Referral & Loyalty Points (Branch: `feat/loyalty-points`)
- New entity: `LoyaltyAccount` — `userId, points, totalEarned, totalRedeemed`
- Earn: 1 point per ₹10 spent (post-delivery)
- Redeem: points as discount at checkout (extends `CouponService` or `PromotionEngineService`)
- Expire: 1 year TTL via ShedLock job
- Migration: V32: `loyalty_accounts`, `loyalty_transactions`

#### 4E — Return Policy Dispute (Branch: `feat/return-dispute`)
- `ReturnDispute` entity: `orderId, customerId, evidence(Cloudinary URLs), adminDecision, resolvedAt`
- `POST /api/v1/orders/{id}/dispute` — customer raises if return was rejected
- `GET /api/v1/admin/disputes/pending`
- `PUT /api/v1/admin/disputes/{id}/resolve`
- Closes the loop on admin reject → customer has recourse

---

### 📋 PHASE 5 — Platform Hardening
**Branch:** `refactor/platform-hardening`
*Makes the platform production-deployable, not just demo-ready.*

| Area | Action | Priority |
|------|--------|----------|
| **CORS** | Replace wildcard `*` with env-specific allowed origins | 🔴 Critical |
| **Actuator** | Restrict `/actuator/**` — currently fully public, needs ADMIN auth | 🔴 Critical |
| **Request Logging** | MDC filter — every request gets a `traceId` in logs | 🟠 High |
| **Idempotency Keys** | `Idempotency-Key` header on checkout + payment endpoints | 🟠 High |
| **HTTPS enforcement** | `server.ssl.*` config + HTTP→HTTPS redirect | 🟠 High |
| **HikariCP tuning** | `maximum-pool-size`, `connection-timeout`, `idle-timeout` in `application.yml` | 🟡 Medium |
| **Redis TTL audit** | Document all Redis key TTLs — currently scattered | 🟡 Medium |
| **API Versioning docs** | Formal `@ApiVersion` strategy document | 🟢 Low |

---

## 🌿 RECOMMENDED BRANCH EXECUTION ORDER

```
main
 └── cleanup/minor-fixes            ← AdminService DTO fix + delete FileService (45 min)
      └── feat/analytics-dashboard  ← Phase 3: Return analytics + CLV + revenue
           └── feat/product-variants ← Phase 4A: Biggest architectural addition
                └── feat/flash-sales
                     └── feat/order-tracking
                          └── feat/loyalty-points
                               └── feat/return-dispute
                                    └── refactor/platform-hardening ← Phase 5: Prod-ready
```

---

## 📊 CURRENT SCORE vs. TARGET

| Dimension | Current (Post-Phase 2) | After Phase 3 | After Phase 4 | After Phase 5 |
|---|---|---|---|---|
| Feature Completeness | **82%** | 88% | 97% | 97% |
| Code Quality | **85%** | 87% | 90% | 95% |
| Production Readiness | **65%** | 70% | 78% | 92% |
| Competitive Differentiation | **85%** | 90% | 97% | 98% |

> **Current Standouts (vs. 90% of e-commerce portfolios):**
> pgvector AI semantic search, pessimistic locking, policy snapshotting, DLQ + replay, SRP service split, in-app notification bell, full return/replacement/exchange lifecycle with proof images, SMS stub, Prometheus metrics, two-query pagination, ShedLock distributed jobs.
>
> **After Phase 4:** Product variants + loyalty points puts this on par with mid-tier production apps (Myntra/Meesho feature level).

---

## 📝 HOW TO KEEP THIS GRAPH CURRENT

When you add a feature, update these sections:

1. **✅ COMPLETED REFACTORS** — check off the item
2. **LAYER 1–13** — add new entity/service/controller/DTO/migration
3. **FEATURE STATUS TRACKER** — mark `✅ Complete`
4. **REMAINING ROADMAP** — move item from Phase to tracker
5. **MIGRATION** — increment version (current last = V29, next = V30)
6. **SCORE** — re-estimate percentages after major phase

> This file lives at: `d:\Projects\APIs\cognitocart\MASTER_REFERENCE.md`
> Update it with every feature branch merge.
