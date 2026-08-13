# 🛒 CognitoCart — Production-Grade E-Commerce Backend API

<div align="center">

> **A battle-hardened Spring Boot 3 REST API engineered for scale, security, and real-world e-commerce complexity.**
> Covers the complete lifecycle: Auth → Catalog → Cart → Checkout → Payment → Shipment → Returns → Analytics → AI Search

[![Java 17](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL%20+%20pgvector-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)
[![Redis](https://img.shields.io/badge/Redis-Upstash-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://upstash.com/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-CloudAMQP-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![Razorpay](https://img.shields.io/badge/Razorpay-Payments-3395FF?style=for-the-badge)](https://razorpay.com/)

</div>

---

## 📋 Table of Contents

- [Architecture Overview](#️-architecture-overview)
- [Technology Stack](#-technology-stack)
- [Module Structure](#-module-structure)
- [Core Domain Concepts](#-core-domain-concepts)
- [Security Model](#-security-model--idor-prevention)
- [Flash Sale Engine](#-flash-sale-engine)
- [API Reference](#-api-reference)
- [Scheduled Jobs](#-scheduled-jobs)
- [Analytics Capabilities](#-analytics-capabilities)
- [Running Locally](#️-running-locally)

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                       External Clients                               │
│         Postman · Web Frontend · Mobile App · Swagger UI             │
└───────────────────────────────┬─────────────────────────────────────┘
                                │  HTTPS
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Spring Boot 3.4.1 API Gateway                     │
│                                                                      │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐    │
│  │   Auth     │  │  Products  │  │    Cart    │  │   Orders   │    │
│  │ JWT + OTP  │  │ Variants   │  │ PG + Redis │  │  Checkout  │    │
│  │ Google SSO │  │ pgvector   │  │ Guest TTL  │  │  Razorpay  │    │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘    │
│                                                                      │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐    │
│  │  Returns   │  │ Analytics  │  │    Infra   │  │   Async    │    │
│  │ Policy     │  │ CLV/Churn  │  │ Bucket4j   │  │  RabbitMQ  │    │
│  │ Snapshot   │  │ CSV/Dash   │  │ ShedLock   │  │  Invoice   │    │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘    │
│                                                                      │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐    │
│  │ Flash Sale │  │ Wishlist   │  │  Reviews   │  │     AI     │    │
│  │ 3-Tier Eng │  │ Cart Conv. │  │ Summarizer │  │  Semantic  │    │
│  │ RabbitMQ   │  │ Price Drop │  │ AI Batch   │  │  Search    │    │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘    │
└────────────────────────┬──────────────────────┬─────────────────────┘
                         │                      │
              ┌──────────▼───────┐    ┌─────────▼────────┐
              │   PostgreSQL 18  │    │   Redis (Upstash) │
              │   + pgvector     │    │  Guest Carts (TTL)│
              │   + Flyway v35   │    │  Rate Limit Buckets│
              │   + ShedLock     │    │  Refresh Token BL │
              └──────────────────┘    └──────────────────┘
```

---

## 🚀 Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| **Framework** | Spring Boot 3.4.1 | Core API engine |
| **Language** | Java 17 | Records, Text Blocks, Pattern Matching |
| **Database** | PostgreSQL 18.2 + pgvector | Relational data + Vector similarity search |
| **Schema** | Flyway 10.20.1 (35 migrations) | Versioned, auditable DB schema evolution |
| **Cache** | Redis (Upstash) | Guest carts, rate limit buckets, token blacklist |
| **ORM** | Spring Data JPA + Hibernate 6 | `@SoftDelete`, `@Lock(PESSIMISTIC_WRITE)`, `@QueryHints` |
| **Security** | Spring Security 6 + JWT (jjwt 0.12.6) | Stateless auth, method-level RBAC |
| **OAuth2** | Google Sign-In | Social login with automatic account linking |
| **Payments** | Razorpay SDK | Order creation, webhook verification, auto-refund |
| **Messaging** | RabbitMQ (CloudAMQP) | Async invoice delivery, mass email, DLQ pattern |
| **Email** | Spring Mail + Thymeleaf HTML | Transactional emails with rich templates |
| **PDF** | iText 7 | Professional PDF invoice generation |
| **CDN** | Cloudinary | Product + variant image upload/delete |
| **AI Embeddings** | HuggingFace Inference API | 384-dim semantic search vectors |
| **Rate Limiting** | Bucket4j + Redis | Per-IP token bucket (cluster-safe) |
| **Distributed Lock** | ShedLock + PostgreSQL | One-node cron execution guarantee |
| **Observability** | Micrometer + Prometheus + Actuator | Metrics, health checks, Grafana-ready |
| **API Docs** | SpringDoc OpenAPI 3 | Interactive Swagger UI |

---

## 📦 Module Structure

```
src/main/java/com/manish/smartcart/
│
├── 🔐 auth/                    # Registration, login, OTP, Google OAuth2, token refresh
├── 🛍️ product/                 # Products, Variants, Categories, AI Embeddings
│   ├── controller/             # ProductController, ProductVariantController
│   ├── service/                # ProductService, EmbeddingService, CategoryService
│   ├── model/                  # Product, ProductVariant, Category, ProductInsights
│   └── repository/             # Custom JPQL + native SQL queries + pgvector
│
├── 🛒 cart/                    # Dual-cart system: PostgreSQL (auth) + Redis (guest)
│   ├── service/CartService     # Math Engine: Subtotals, Coupon, Delivery, Flash override
│   ├── service/GuestCartService # TTL-based anonymous cart with merge-on-login
│   └── model/                  # Cart, CartItem, GuestCart, GuestCartItem
│
├── 📦 order/                   # Full checkout, payments, returns, cancellations
│   ├── service/OrderService    # Saga Pattern: TransactionTemplate for safe Razorpay calls
│   ├── service/ReturnService   # Customer-facing return/replacement requests
│   ├── service/ReturnAdminService # Admin approvals, stock restoration, refund triggers
│   ├── service/ShipmentService # Courier tracking attachment
│   ├── coupon/                 # Coupon CRUD, validation engine, per-user usage tracking
│   └── model/                  # Order, OrderItem, Shipment, Coupon, UserCouponUsage
│
├── 💳 payment/                 # Razorpay integration
│   ├── service/PaymentService  # Order ID creation
│   ├── service/WebhookService  # HMAC verification, status promotion
│   ├── service/RazorpayRefundService # Full refund orchestration
│   └── service/WebhookDlqService    # Replay failed webhook events
│
├── ⚡ sale/                    # Flash Sale Engine (3-tier: Admin → Seller → User)
│   ├── service/AdminSaleService # Event CRUD + RabbitMQ publish
│   ├── service/SellerSaleService # Single-item submission
│   ├── service/SellerBulkSaleService # CSV bulk upload with IDOR ownership checks
│   └── job/FlashSaleActivationJob   # ShedLock-protected SCHEDULED→ACTIVE→ENDED
│
├── ❤️ wishlist/                # Wishlist toggle, one-click wishlist-to-cart, price-drop alerts
├── ⭐ review/                  # Verified-purchase reviews, upsert logic, rating distribution
│   └── ReviewImages            # Cloudinary CDN integration for customer review photos (max 3)
│
├── 👤 user/                    # User profile, address book
│   ├── service/CustomerService # Dashboard, Loyalty Points tally, Store Credit Wallet balance
│   └── service/AddressService  # Address CRUD with UUID-based IDOR protection
│
├── 🔔 notification/            # In-app notifications, SMS, Order notifications
│   ├── service/InAppNotificationService # Create, paginate, mark-as-read
│   └── service/OrderNotificationService # Confirmation, status update, refund, delivery
│
├── 🏛️ admin/                   # Platform management and analytics
│   ├── controller/AdminController # UUID-secured admin endpoints
│   └── service/AdminService    # Dashboard, CLV, churn, category revenue
│
├── 🏗️ infrastructure/          # Cross-cutting concerns
│   ├── ai/AiSummarizationService # AI batch review summarizer
│   ├── email/EmailTemplateBuilder # Thymeleaf HTML email templates
│   ├── invoice/InvoiceService  # iText 7 PDF generation + RabbitMQ dispatch
│   ├── returnpolicy/           # Chain of Responsibility policy resolution
│   └── ratelimit/              # Bucket4j per-IP rate limiting filter
│
├── 🔒 security/                # JWT filter, CustomUserDetails, SecurityConfig
├── 🗂️ shared/                  # Exceptions, Enums, Mappers, Constants
│   ├── exception/              # GlobalExceptionHandler, ResourceNotFoundException, etc.
│   └── mapper/                 # OrderMapper, ProductMapper
│
└── ⚙️ config/                  # RabbitMQ, Redis, ShedLock, Cloudinary, Swagger config
```

---

## 🧠 Core Domain Concepts

### 1. The 3-ID Security System (UUID → Long)

> **Problem solved:** Auto-incrementing `Long` IDs in URLs allow attackers to enumerate orders, guess user counts, and perform IDOR attacks.

```
External (Public API)         Internal (Database Engine)
─────────────────────         ─────────────────────────
UUID  →  Controller  →  Service (Translator)  →  Long PK
        (Edge Layer)    (Resolution Zone)       (Efficient Join)
```

Every entity carries **two identifiers**:
- `id` (Long): Internal PK — fast joins, never exposed in API responses
- `publicId` (UUID v4): External identifier — in all URLs, DTOs, and JSON responses

**Resolution happens inside `@Transactional` service methods**, maintaining referential integrity:

```java
// ✅ Pattern used across ALL services
public OrderResponse cancelOrder(UUID orderPublicId, Long userId) {
    Long orderId = orderRepository.findByPublicId(orderPublicId)
            .orElseThrow(() -> new ResourceNotFoundException(...))
            .getId();
    // ... business logic uses internal Long orderId
}
```

---

### 2. Product → Variant Architecture

```
Product  (Marketing Shell — what the catalog shows)
├── productName, slug, description, tags
├── price (base), averageRating, totalReviews
├── imageUrls[], embedding (384-dim pgvector)
└── ProductVariant[]  (purchasable SKUs — what the cart works with)
    ├── sku, barcode, displayLabel
    ├── attributes: { "Size": "L", "Color": "Red" }
    ├── stockQuantity, reservedQuantity
    ├── priceModifier (delta added to base price)
    ├── sortOrder (controls default variant selection)
    └── availableStock = stockQuantity - reservedQuantity
```

> **Rule:** Every cart operation, checkout, and stock deduction targets a `ProductVariant` by `publicId`, never a `Product` directly.
> The default variant is resolved **at the DB level** via `ORDER BY sort_order LIMIT 1` — not in application memory.

---

### 3. OrderItem — Dual-Layer Design (Snapshot Architecture)

```
OrderItem
├── LIVE REFERENCE (nullable — for stock ops)
│   └── variant → ProductVariant
│
└── IMMUTABLE SNAPSHOTS  ← Frozen at checkout, never changes
    ├── productNameSnapshot   "Nike Air Max 90"
    ├── skuSnapshot           "NIKE-AIR-L-RED"
    ├── variantLabelSnapshot  "Navy Blue / UK 9"
    ├── priceAtPurchase       ₹4,999.00
    └── imageUrlSnapshot      "https://cdn.cloudinary.com/..."
```

> Even if the seller renames the product, changes the price, or soft-deletes the variant tomorrow — every invoice and receipt remains 100% historically accurate.

---

### 4. Cart Math Engine

```
                ┌─────────────────────────────────┐
                │    CartService.updateCartTotal   │
                └─────────────────┬───────────────┘
                                  │
  Gross Subtotal  =  Σ (priceAtAdding × quantity)
                                  │
                    ┌─────────────▼─────────────┐
                    │    Promotion Engine        │
                    │  FLAT / PERCENT / BOGO /   │
                    │  FREE_SHIPPING coupon       │
                    └─────────────┬─────────────┘
                                  │
  Net Subtotal  =  Gross − Discount Amount
                                  │
  Delivery Fee:  Net < ₹599 → ₹50  |  Net ≥ ₹599 → ₹0
                 (overridden to ₹0 by FREE_SHIPPING coupon)
                                  │
  Final Total   =  Net Subtotal + Delivery Fee
```

**Flash Sale Override:** If any cart item has an active flash sale, the real-time discounted price is applied and `priceAtAdding` is updated before the checkout totals are recalculated.

---

### 5. Checkout — Orchestration-Based Saga Pattern

```
placeOrder()
    │
    ├── TX 1 (Fast, ~50ms): buildAndPersistOrder()
    │       ├── Load cart + user with email-verified guard
    │       ├── Sort CartItems by variantId → Deadlock prevention
    │       ├── Per item:
    │       │   ├── SELECT FOR UPDATE (Pessimistic Write Lock)
    │       │   ├── Re-check availableStock vs. quantity ordered
    │       │   ├── Check flash sale price deviation ≤ ₹0.05
    │       │   ├── Atomic flash sale unit increment (@Modifying native SQL)
    │       │   ├── Deduct stockQuantity
    │       │   └── Freeze 5 snapshots → OrderItem
    │       ├── Apply coupon, calculate total
    │       ├── Snapshot ReturnPolicy as JSONB
    │       └── SAVE Order → DB connection RELEASED ✅
    │
    ├── NETWORK I/O (High-latency, ~3–5s): Razorpay HTTP call
    │       ← Zero DB connections held during this phase
    │       └── If fails → compensating TX cancels order + restores stock
    │
    └── TX 2 (Fast, ~20ms): Attach razorpayOrderId + clearCart
```

---

### 6. Return Policy — Chain of Responsibility

```
getApplicablePolicy(product):
  1. Check product-level ReturnPolicy  → return if found
  2. Check category-level ReturnPolicy → return if found
  3. Default: NON_RETURNABLE            → never throws NPE

→ Policy JSONB snapshot frozen into Order.returnPolicySnapshot at checkout
→ Customer rights are frozen at purchase time, even if seller updates policy later
```

---

### 7. AI Semantic Search

```
User query: "earphones for studying in a noisy café"
      │
      ▼
EmbeddingService → HuggingFace API → float[384] vector
      │
      ▼
VectorAttributeConverter → "[0.021,-0.455,...]"
      │
      ▼
ProductRepository.findBySimilarity():
  SELECT * FROM products
  WHERE embedding IS NOT NULL
  ORDER BY embedding <=> CAST(:queryVector AS vector)
  LIMIT :limit
      │
      ▼
Returns: "Noise-Cancelling Headphones" (zero keyword overlap)
```

> AI Review Summarizer (`ReviewSummarizationScheduler`) runs nightly at 3 AM, batching all product reviews through the HuggingFace API to generate concise AI summaries stored back on the product record.

---

### 8. Split-Tender Checkout Engine & Store Credit

```
1. Gross Subtotal  = Σ (Price × Qty)
2. Net Subtotal    = Gross - Promo Code Discount - (Loyalty Points ÷ 10)
3. Final Total     = Net Subtotal + Delivery Fee
4. Gateway Payable = Final Total - Applied Store Credit Wallet
```

Checkout handles complex split payments seamlessly:
- **100% Wallet/Points**: If `Gateway Payable == 0`, the Razorpay network call is bypassed entirely, transitioning instantly to PAID.
- **Smart Refund Routing**: When cancelling, customers choose `WALLET` (instant 100% store credit) or `ORIGINAL`. If `ORIGINAL`, the system automatically splits the refund: returning the precise wallet portion to the Store Credit Wallet, and firing a Razorpay API call for the exact bank portion.

---

## 🛡️ Security Model & IDOR Prevention

| Layer | Mechanism | Detail |
|---|---|---|
| **Authentication** | Stateless JWT (HS256) | 15-min access token + 30-day refresh token |
| **Refresh Tokens** | Redis blacklist | Logout invalidates tokens instantly across cluster |
| **IDOR Prevention** | UUID public IDs | All external API URLs use UUIDs — internal Longs never exposed |
| **RBAC** | `@PreAuthorize` method-level | Separate role guards: CUSTOMER / SELLER / ADMIN |
| **Rate Limiting** | Bucket4j + Redis | 20 req/10s per IP, cluster-safe, configurable |
| **Input Validation** | `@Valid` + Bean Validation | Applied to 100% of request DTOs |
| **Passwords** | BCrypt strength 10 | Standard industry hashing |
| **Payment Integrity** | Razorpay HMAC-SHA256 | Every webhook and payment verified cryptographically |
| **Upload Security** | `FileValidator` | MIME type + extension + file size checked before Cloudinary |
| **Email Verification** | OTP on registration | Orders blocked until email is verified |
| **IDOR (Orders)** | 404 not 403 | Returning 404 for wrong-owner orders prevents valid ID enumeration |
| **Flash Sale Security** | `@Modifying` atomic SQL | Bypasses Hibernate L1 cache to prevent overselling in concurrent checkouts |

---

## ⚡ Flash Sale Engine

### 3-Tier Role Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│  TIER 1 — ADMIN                                                  │
│  Creates global sale events · Sets time window                   │
│  Approves / Rejects seller-submitted SKUs via QC review          │
├──────────────────────────────────────────────────────────────────┤
│  TIER 2 — SELLER                                                 │
│  Opts-in individual SKUs (single form or Bulk CSV upload)        │
│  Submissions default to PENDING until Admin approves             │
│  IDOR check: can only submit SKUs they own                       │
├──────────────────────────────────────────────────────────────────┤
│  TIER 3 — USER                                                   │
│  Cart auto-detects active flash sale → applies discounted price  │
│  Checkout re-validates price and atomically decrements quotas    │
└──────────────────────────────────────────────────────────────────┘
```

### Event Lifecycle (ShedLock, polls every 60s)

```
Admin creates event
      │
      ▼
 SCHEDULED ──── ShedLock job polls DB every 60s ───▶ ACTIVE ──▶ ENDED
                                                        │
                                             CartService.updateCartTotal
                                             applies live flash sale price
```

### Mass Email Fan-Out (RabbitMQ, Phase 4C)

```
POST /api/v1/admin/sales/events
      │
   ┌──┴───────────────────────────┐
   ▼                              ▼
 Save to DB             Publish FlashSaleCreatedEvent
 201 CREATED ✅         → exchange.marketing
 Admin unblocked!                 │
                                  ▼  (async background thread)
                    FlashSaleNotificationListener [ORCHESTRATOR]
                                  │
                     Page through Slice<SellerEmailProjection>
                     (only email + fullName — no OOM risk)
                     500 sellers per page
                     Publishes 1 SellerInviteEmailEvent per seller
                                  │
                        queue.seller.email.individual
                        (50,000 tiny messages, ~200 bytes each)
                                  │
              ┌───────────────────┼───────────────────┐
              ▼                   ▼                   ▼
        Worker Thread 1    Worker Thread 2      ... Thread N
        sendMail(seller1)  sendMail(seller2)   (concurrency = 5–10)
              │                   │
          Success ✅          Exception ❌
          Email sent         NACK → DLQ
                             (only that seller, others unaffected)
```

### Key Design Decisions

| Concern | Solution |
|---|---|
| 10k concurrent checkouts overselling | `@Modifying` native `atomicallyIncrementUsedUnits` — bypasses Hibernate L1 cache |
| Abandoned cart price exploit | Checkout rejects if price deviation > ₹0.05 from stored cart price |
| Bot draining entire flash stock | `maxUnitsPerUser` checked on **both** add-new AND update-quantity paths |
| Seller discounting competitor SKUs | IDOR ownership check per CSV row: `variant.getSellerId() == loggedInSellerId` |
| Admin blocked during 50k email send | RabbitMQ decouples event creation from delivery entirely |
| Email server down = invites lost | DLQ retains failed messages for safe manual replay |
| OOM loading 50k seller entities | `Slice<SellerEmailProjection>` — only `email` + `fullName`, 500/page |

---

## 🌐 API Reference

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Register + send OTP email |
| POST | `/api/v1/auth/login` | Public | JWT access + refresh token |
| POST | `/api/v1/auth/verify-otp` | Public | Email OTP verification |
| POST | `/api/v1/auth/refresh-token` | Public | Rotate access token |
| POST | `/api/v1/auth/logout` | Bearer | Blacklist refresh token |
| GET | `/oauth2/authorization/google` | Public | Google OAuth2 redirect |

### Products & Catalog

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/products` | Public | Paginated catalog |
| GET | `/api/v1/products/{slug}` | Public | Product detail (cached) |
| GET | `/api/v1/products/search` | Public | Filter by price, rating, category |
| GET | `/api/v1/products/search/semantic?q=...` | Public | AI semantic search |
| POST | `/api/v1/products` | SELLER | Create product + default variant |
| PUT | `/api/v1/products/{productPublicId}` | SELLER | Update product details |
| DELETE | `/api/v1/products/{productPublicId}` | SELLER/ADMIN | Soft delete product |
| GET | `/api/v1/products/{productPublicId}/return-policy` | Public | Live policy chain |

### Product Variants

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/products/{id}/variants` | Public | All active SKUs |
| POST | `/api/v1/products/{id}/variants` | SELLER | Add new variant SKU |
| PUT | `/api/v1/products/{id}/variants/{vId}` | SELLER | Update stock + price modifier |
| PATCH | `/api/v1/products/{id}/variants/{vId}/status` | SELLER/ADMIN | Toggle SKU availability |
| POST | `/api/v1/products/{id}/variants/{vId}/upload-image` | SELLER | Upload to Cloudinary CDN |

### Cart (Authenticated)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/cart/add` | CUSTOMER | Add variant (body: `variantPublicId`, `quantity`) |
| GET | `/api/v1/cart/summary` | CUSTOMER | Full cart with totals |
| DELETE | `/api/v1/cart/item/{variantPublicId}` | CUSTOMER | Remove specific variant |
| PATCH | `/api/v1/cart/item/{variantPublicId}` | CUSTOMER | Update item quantity |
| POST | `/api/v1/cart/apply-coupon?code=...` | CUSTOMER | Apply promo code |
| DELETE | `/api/v1/cart/remove-coupon` | CUSTOMER | Remove applied coupon |
| DELETE | `/api/v1/cart/clear` | CUSTOMER | Empty the cart |

### Guest Cart (No Auth)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/guest-cart/{sessionId}/add` | Public | Add variant to Redis guest cart |
| GET | `/api/v1/guest-cart/{sessionId}` | Public | View guest cart |
| DELETE | `/api/v1/guest-cart/{sessionId}/item/{variantPublicId}` | Public | Remove item |
| POST | `/api/v1/guest-cart/{sessionId}/merge` | CUSTOMER | Merge into user cart on login |

### Orders & Checkout

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/orders/checkout` | CUSTOMER | Place order → returns Razorpay order ID |
| GET | `/api/v1/orders/my` | CUSTOMER | Paginated order history |
| DELETE | `/api/v1/orders/{orderPublicId}/cancel` | CUSTOMER | Cancel + refund + restore stock |
| POST | `/api/v1/orders/{orderPublicId}/return` | CUSTOMER | Return/replacement/exchange request |

### Payments

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/payments/verify` | CUSTOMER | Frontend payment confirmation (HMAC check) |
| POST | `/api/v1/payments/webhook` | Public | Razorpay server-to-server webhook |

### Addresses

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/addresses` | CUSTOMER | Add new delivery address |
| GET | `/api/v1/addresses` | CUSTOMER | Get all saved addresses |
| PUT | `/api/v1/addresses/{addressPublicId}` | CUSTOMER | Update address |
| DELETE | `/api/v1/addresses/{addressPublicId}` | CUSTOMER | Delete address |
| PATCH | `/api/v1/addresses/{addressPublicId}/default` | CUSTOMER | Set as primary address |

### Reviews

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/reviews/{productPublicId}` | Public | All reviews for a product |
| GET | `/api/v1/reviews/{productPublicId}/distribution` | Public | Star rating histogram |
| POST | `/api/v1/reviews/{productPublicId}` | CUSTOMER | Submit review + up to 3 Images (multipart/form-data) |
| DELETE | `/api/v1/reviews/{reviewPublicId}` | CUSTOMER | Delete own review |
| DELETE | `/api/v1/reviews/admin/{reviewPublicId}` | ADMIN | Force-delete any review |

### Wishlist

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/wishlist/toggle/{productPublicId}` | CUSTOMER | Idempotent add/remove toggle |
| GET | `/api/v1/wishlist` | CUSTOMER | Get all wishlisted products |
| GET | `/api/v1/wishlist/summary` | CUSTOMER | Wishlist with total value calculation |
| POST | `/api/v1/wishlist/move-to-cart/{productPublicId}` | CUSTOMER | Atomic wishlist→cart transfer |

### Flash Sale

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/admin/sales/events` | ADMIN | Schedule platform-wide sale event |
| GET | `/api/v1/admin/sales/events` | ADMIN | View all events |
| PATCH | `/api/v1/admin/sales/items/{id}/review` | ADMIN | Approve / Reject seller SKU submission |
| POST | `/api/v1/seller/sales/items` | SELLER | Submit a single SKU |
| GET | `/api/v1/seller/sales/items` | SELLER | View own submissions + approval status |
| POST | `/api/v1/seller/sales/{eventPublicId}/bulk-upload` | SELLER | CSV bulk upload (multipart/form-data) |
| GET | `/api/v1/public/sales/live` | Public | Active events for frontend banners |
| GET | `/api/v1/public/sales/upcoming` | Public | Scheduled events for countdown timers |

### Notifications

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/notifications` | Bearer | Get paginated in-app notifications |
| PATCH | `/api/v1/notifications/{notifPublicId}/read` | Bearer | Mark single notification as read |
| PATCH | `/api/v1/notifications/read-all` | Bearer | Mark all notifications as read |

### Admin Dashboard & Analytics

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/admin/dashboard` | ADMIN | Revenue + low stock + top products |
| GET | `/api/v1/admin/analytics/clv` | ADMIN | Customer Lifetime Value ranking |
| GET | `/api/v1/admin/analytics/churn` | ADMIN | Churn risk detection |
| GET | `/api/v1/admin/analytics/category-revenue` | ADMIN | Category performance breakdown |
| GET | `/api/v1/admin/orders` | ADMIN | All orders (paginated) |
| PATCH | `/api/v1/admin/orders/{orderPublicId}/status` | ADMIN | Update order status |
| GET | `/api/v1/admin/returns` | ADMIN | Pending return requests |
| PATCH | `/api/v1/admin/returns/{returnId}/approve` | ADMIN | Approve return + trigger refund |
| PATCH | `/api/v1/admin/returns/{returnId}/reject` | ADMIN | Reject return with comment |

---

## ⏱️ Scheduled Jobs

| Job | Schedule | ShedLock | Purpose |
|---|---|---|---|
| `FlashSaleActivationJob` | Every 60s | ✅ | Activate / deactivate sale events by time window |
| `OrderCleanupScheduler` | Every 5 min | ✅ | Cancel stale `PAYMENT_PENDING` orders > 15 min |
| `CartAbandonmentJob` | Daily 10 PM | ✅ | Email users with items remaining in cart |
| `ReviewSummarizationScheduler` | Daily 3 AM | ✅ | AI batch summarization of all product reviews |
| `WishlistConversionScheduler` | Daily 2 AM | ✅ | Price-drop alert emails (14-day cooldown per product) |

> **Why ShedLock?** Unlike `@Scheduled` alone, ShedLock guarantees exactly-once execution across N horizontally scaled instances — no duplicate emails, no double stock deductions.

---

## 📊 Analytics Capabilities

| Domain | Metrics Available |
|---|---|
| **Revenue** | Total platform revenue · Daily trend (7-day) · Per-seller breakdown |
| **Inventory** | Low stock alerts per variant · Configurable threshold per SKU |
| **Products** | Top-selling globally (Admin) · Top-selling per seller |
| **Customers** | CLV ranking · Churn risk scoring · Dashboard (orders, spend, recent activity) |
| **Returns** | Return reason distribution · Defect matrix per product |
| **Exports** | Seller analytics CSV (streaming, memory-safe, no OOM) |

---

## ⚙️ Running Locally

### Prerequisites

- Java 17+
- PostgreSQL 15+ with `pgvector` extension enabled
- Redis instance
- RabbitMQ broker (or CloudAMQP free tier)

### Setup

```bash
# Clone the repo
git clone https://github.com/manish5200/CognitoCart.git
cd cognitocart/smartcart

# Configure environment variables (or application.properties):
# ─── Database ───
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/cognitocart
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=yourpassword

# ─── Redis ───
SPRING_DATA_REDIS_URL=redis://localhost:6379

# ─── RabbitMQ ───
SPRING_RABBITMQ_HOST=localhost

# ─── Razorpay ───
RAZORPAY_KEY_ID=rzp_test_...
RAZORPAY_KEY_SECRET=your_secret

# ─── Cloudinary ───
CLOUDINARY_CLOUD_NAME=your_cloud
CLOUDINARY_API_KEY=your_key
CLOUDINARY_API_SECRET=your_secret

# ─── AI ───
HUGGINGFACE_API_KEY=hf_...

# ─── Email ───
SPRING_MAIL_USERNAME=your@email.com
SPRING_MAIL_PASSWORD=yourpassword

# Run!
./mvnw spring-boot:run
```

### Docker Compose (local infra)

```bash
# Starts PostgreSQL + Redis + RabbitMQ
docker-compose up -d
```

### API Documentation

Full interactive API docs available via Swagger UI:

```
http://localhost:8080/swagger-ui/index.html
```

<br>

<div align="center">
  <img src="https://github.com/user-attachments/assets/0856ec28-129a-4f67-ac04-cce202e590c4" alt="Swagger UI Screenshot" width="800">
</div>

### Metrics Endpoint

```
http://localhost:8080/actuator/prometheus
```

---

## 🏆 Engineering Highlights

| Challenge | Solution | Pattern |
|---|---|---|
| Overselling during flash sales | `SELECT FOR UPDATE` + `@Modifying` atomic increment | Pessimistic Locking |
| Razorpay call holding DB connection for 5s | `TransactionTemplate` programmatic boundary splitting | Saga Pattern |
| Split-tender math corruption | Immutable DB trackers (`walletAmountPaid`, `gatewayAmountPaid`) + atomic rollback | Ledger Routing |
| 5-7 day refund delays | Store Credit Wallet for instant zero-latency refunds | Financial Proxy |
| Long IDs enabling IDOR attacks | UUID `publicId` on every entity, Long only internal | 3-ID Security System |
| 50k emails blocking the API thread | RabbitMQ fan-out, async worker threads, DLQ | Event-Driven Architecture |
| Price exploit via stale cart | Price deviation check ≤ ₹0.05 at checkout time | Checkout Validation |
| Bot depleting flash sale stock | `maxUnitsPerUser` enforced on add AND update paths | Rate Enforcement |
| Product deleted = broken invoice | Snapshot all 5 metadata fields at checkout | Immutable Snapshots |
| Memory blowup loading 50k sellers | `Slice<Projection>`, 500/page, lightweight DTOs | Streaming Pagination |
| N+1 on order shipment enrichment | Single `findByOrder_Id` per order in map phase | Query Optimization |
| Policy changed after purchase | JSONB policy snapshot in Order at checkout time | Event Sourcing Lite |

---

<div align="center">

**Built with ❤️ by [Manish Chauhan](https://github.com/manish5200)**

*Engineered for scale. Secured by design. Ready for production.*

</div>
