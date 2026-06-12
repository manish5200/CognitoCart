# CognitoCart — AI-Driven E-Commerce Backend API

> **Production-grade Spring Boot 3 REST API** covering the complete e-commerce lifecycle:
> Product Catalog → Cart → Checkout → Payment → Returns → AI Search → Analytics

[![Java 17](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-blue)](https://github.com/pgvector/pgvector)
[![Redis](https://img.shields.io/badge/Redis-Upstash-red)](https://upstash.com/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-DLQ-orange)](https://www.rabbitmq.com/)

---

## 🏗️ Architecture Overview

```
Client (Postman / Frontend)
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│                     Spring Boot API                          │
│                                                             │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐  ┌──────────┐ │
│  │  Auth    │   │ Products │   │   Cart   │  │  Orders  │ │
│  │ JWT+OTP  │   │ Variants │   │ PG+Redis │  │ Checkout │ │
│  │ Google   │   │ pgvector │   │ GuestTTL │  │ Razorpay │ │
│  └──────────┘   └──────────┘   └──────────┘  └──────────┘ │
│                                                             │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐  ┌──────────┐ │
│  │ Returns  │   │Analytics │   │   Infra  │  │ Async    │ │
│  │ Policy   │   │ CLV/Churn│   │ Bucket4j │  │ RabbitMQ │ │
│  │ Snapshot │   │ CSV/Dash │   │ ShedLock │  │ Invoice  │ │
│  └──────────┘   └──────────┘   └──────────┘  └──────────┘ │
└─────────────────────────────────────────────────────────────┘
        │                           │
        ▼                           ▼
   PostgreSQL                    Redis
   + pgvector                  (Upstash)
```

---

## 🚀 Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| Framework | Spring Boot 3.4.1 | Core API |
| Language | Java 17 | Records, Sealed classes, Text Blocks |
| Database | PostgreSQL + pgvector | Relational data + vector similarity search |
| Cache | Redis (Upstash) | Guest carts (TTL), Product caching, Rate limit buckets |
| ORM | Spring Data JPA + Hibernate 6 | `@SoftDelete`, `@Lock`, `@QueryHints` |
| Migrations | Flyway | Versioned schema migrations |
| Security | Spring Security + JWT (jjwt 0.12.6) | Stateless auth |
| OAuth2 | Google Sign-In | Social login |
| Payments | Razorpay | Payment gateway + webhook |
| Messaging | RabbitMQ | Async invoice + DLQ pattern |
| Email | Spring Mail + HTML templates | Transactional emails |
| PDF | iText 7 | Invoice generation |
| CDN | Cloudinary | Product image upload/delete |
| AI Embeddings | HuggingFace API | 384-dim semantic search vectors |
| Rate Limiting | Bucket4j + Redis | Per-IP token bucket |
| Distributed Lock | ShedLock + PostgreSQL | Prevents duplicate scheduler execution |
| Observability | Micrometer + Prometheus + Actuator | Metrics, health checks |
| API Docs | SpringDoc OpenAPI 3 | Swagger UI |

---

## 📦 Module Structure

```
src/main/java/com/manish/smartcart/
├── config/              # Security, Redis, RabbitMQ, ShedLock, Cloudinary, Swagger
│   ├── SecurityConfig.java         # JWT filter chain, CORS, OAuth2, Rate limit
│   ├── RabbitMQConfig.java         # Main queue + DLQ + TopicExchange
│   ├── RedisConfig.java            # Cache + Guest cart (Jackson serializer)
│   └── ShedLockConfig.java         # Distributed scheduler lock
├── controller/          # 15 REST controllers (no business logic)
├── service/             # 29 services (all business logic)
│   ├── order/           # OrderService, OrderReturnService, ReturnAdminService
│   ├── email/           # EmailTemplateBuilder (premium HTML)
│   └── notifications/   # OrderNotificationService
├── model/               # JPA entities
│   ├── product/         # Product, ProductVariant, Category, ProductInsights
│   ├── order/           # Order, OrderItem, Coupon, Shipment
│   ├── cart/            # Cart, CartItem, GuestCart, GuestCartItem
│   └── user/            # Users, Wishlist, Address, SellerProfile
├── repository/          # Spring Data JPA repositories (custom JPQL)
├── dto/                 # Request/Response DTOs
├── mapper/              # OrderMapper, ProductMapper, ReviewMapper
├── scheduler/           # ShedLock-protected batch jobs
├── exception/           # Global @ControllerAdvice
└── util/                # VectorAttributeConverter, AppConstants, FileValidator
```

---

## 🔑 Core Domain Concepts

### 1. Product → Variant Architecture
```
Product (Marketing Shell)
├── productName, description, slug, tags, price (base)
├── averageRating, totalReviews, imageUrls
└── ProductVariant[] (purchasable SKUs)
    ├── sku, barcode
    ├── attributes: { "Size": "L", "Color": "Red" }
    ├── stockQuantity, reservedQuantity
    ├── priceModifier (delta applied to base price)
    └── availableStock = stockQuantity - reservedQuantity
```

> **Rule:** Every Product auto-creates a default `{Type: Standard}` variant on creation.
> Cart and Checkout always work with `variantId` — never `productId`.

### 2. OrderItem — Dual-Layer Design
```
OrderItem
├── LIVE REFERENCE (nullable)
│   └── variant → ProductVariant (for stock ops, null if deleted)
│
└── IMMUTABLE SNAPSHOTS (frozen at checkout)
    ├── productNameSnapshot   → "Nike Air Max 90"
    ├── skuSnapshot           → "NIKE-AIR-L-RED"
    ├── variantLabelSnapshot  → "Navy Blue / UK 9"
    ├── priceAtPurchase       → 4999.00
    └── imageUrlSnapshot      → "https://cdn.../nike.jpg"
```

> Even if the seller renames the product or deletes the variant tomorrow,
> every invoice remains 100% accurate.

### 3. Cart Math Engine (`CartService.updateCartTotal`)
```
Gross Subtotal = Σ (priceAtAdding × quantity)
      ↓
Promotion Engine (FLAT / PERCENT / BOGO / FREE_SHIPPING)
      ↓
Net Subtotal = Gross - Discount
      ↓
Delivery Fee = Net < ₹599 ? ₹50 : ₹0  (overridden by FREE_SHIPPING coupon)
      ↓
Final Total = Net + Delivery
```

### 4. Checkout — Race Condition Protection
```
For each CartItem:
  1. SELECT * FROM product_variants WHERE id = ? FOR UPDATE  ← BLOCKS second thread
  2. Re-read availableStock = stockQuantity - reservedQuantity
  3. If insufficient → throw InsufficientStockException
  4. Deduct stockQuantity
  5. Freeze 5 snapshots → OrderItem
```

### 5. Return Policy Chain of Responsibility
```
getApplicablePolicy(product):
  1. Check product-level ReturnPolicy → if found, return it
  2. Check category-level ReturnPolicy → if found, return it
  3. Default → NON_RETURNABLE (never throws NPE)

→ Snapshot serialized as JSONB into Order.returnPolicySnapshot at checkout
→ Customer rights frozen at purchase time even if seller changes policy
```

### 6. Semantic AI Search Flow
```
User query: "earphones for studying in a noisy café"
     ↓
EmbeddingService → HuggingFace API → float[384] vector
     ↓
VectorAttributeConverter → "[0.021,-0.455,...]"
     ↓
ProductRepository.findBySimilarity():
  SELECT * FROM products
  WHERE embedding IS NOT NULL
  ORDER BY embedding <=> CAST(:queryVector AS vector)
  LIMIT :limit
     ↓
Returns: "Noise Cancelling Headphones" (0 keyword overlap)
```

---

## 🌐 API Reference (Key Endpoints)

### Authentication
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Register + OTP email |
| POST | `/api/v1/auth/login` | Public | JWT + Refresh token |
| POST | `/api/v1/auth/verify-otp` | Public | Email verification |
| POST | `/api/v1/auth/refresh-token` | Public | Rotate access token |
| GET | `/oauth2/authorization/google` | Public | Google OAuth2 login |

### Products & Search
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/products` | Public | Paginated catalog |
| GET | `/api/v1/products/{slug}` | Public | Product detail (cached) |
| GET | `/api/v1/products/search` | Public | Filter (price, rating, category) |
| GET | `/api/v1/products/search/semantic?q=...` | Public | AI semantic search |
| POST | `/api/v1/products` | SELLER | Create product + default variant |
| GET | `/api/v1/products/{productId}/return-policy` | Public | Live policy chain |

### Cart (Authenticated)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/cart/add` | USER | Add variant to cart (body: variantId, quantity) |
| GET | `/api/v1/cart/summary` | USER | Full cart with totals |
| DELETE | `/api/v1/cart/item/{variantId}` | USER | Remove specific variant |
| POST | `/api/v1/cart/apply-coupon?code=...` | USER | Apply promo code |

### Guest Cart (No Auth)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/guest-cart/{sessionId}/add` | Public | Add to Redis guest cart |
| GET | `/api/v1/guest-cart/{sessionId}` | Public | View guest cart |
| POST | `/api/v1/guest-cart/{sessionId}/merge` | USER | Merge on login |

### Orders & Checkout
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/orders/checkout` | USER | Place order → Razorpay ID |
| GET | `/api/v1/orders/my` | USER | Paginated order history |
| DELETE | `/api/v1/orders/{orderId}/cancel` | USER | Cancel + refund + restore stock |
| POST | `/api/v1/orders/{orderId}/return` | USER | Return/replacement/exchange request |

### Payments
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/payments/verify` | Public | Frontend payment confirmation |
| POST | `/api/v1/payments/webhook` | Public | Razorpay server-to-server webhook |

### Admin
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/admin/dashboard` | ADMIN | Revenue, low stock, top products, trend |
| GET | `/api/v1/admin/analytics/clv` | ADMIN | Customer Lifetime Value |
| GET | `/api/v1/admin/analytics/churn` | ADMIN | Churn risk customers |
| GET | `/api/v1/admin/analytics/category-revenue` | ADMIN | Category performance |

---

## ⚙️ Running Locally

### Prerequisites
- Java 17+
- PostgreSQL with `pgvector` extension enabled
- Redis instance
- RabbitMQ broker

### Setup
```bash
# Clone
git clone https://github.com/manish5200/CognitoCart.git
cd cognitocart/smartcart

# Configure application.properties or set environment variables:
# SPRING_DATASOURCE_URL, REDIS_URL, RABBITMQ_HOST
# RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET
# CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET
# HUGGINGFACE_API_KEY
# SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD

# Run
./mvnw spring-boot:run
```

### Docker Compose (PostgreSQL + Redis + RabbitMQ)
```bash
docker-compose up -d
```

### API Documentation
Visit: `http://localhost:8080/swagger-ui/index.html`

### Metrics
Visit: `http://localhost:8080/actuator/prometheus`

---

## 🔒 Security Model

| Layer | Mechanism |
|---|---|
| Auth | Stateless JWT (HS256, 15min expiry) + Refresh Token (30d, Redis blacklist) |
| Rate Limit | Bucket4j token bucket per IP — 20 req/10s (Redis-backed for cluster) |
| RBAC | Spring `@PreAuthorize("hasRole('SELLER')")` — method-level security |
| Input | `@Valid` + Bean Validation on all DTOs |
| Passwords | BCrypt (strength 10) |
| Payments | Razorpay HMAC-SHA256 signature verification |
| Upload | `FileValidator` checks MIME type + extension + size before Cloudinary |

---

## 🧪 Scheduled Jobs

| Job | Schedule | ShedLock? | Purpose |
|---|---|---|---|
| `OrderCleanupScheduler` | Every 5 min | ✅ | Cancel stale PAYMENT_PENDING orders > 15min |
| `CartAbandonmentJob` | Daily 10 PM | ✅ | Email users with items left in cart |
| `ReviewSummarizationScheduler` | Daily 3 AM | ✅ | AI batch summary of all product reviews |
| `WishlistConversionScheduler` | Daily 2 AM | ✅ | Price-drop email alerts (14-day cooldown) |

---

## 📊 Analytics Capabilities

- **Revenue:** Total + daily trend (last 7 days) + per-seller breakdown
- **Inventory:** Low stock alerts per variant (threshold configurable per SKU)
- **Products:** Top selling globally (Admin) + per seller
- **Customers:** CLV ranking · Churn risk detection
- **Returns:** Return reason distribution · Defect matrix per product
- **Exports:** Seller analytics CSV export (streaming, memory-safe)
