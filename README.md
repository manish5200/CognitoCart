# 🛒 CognitoCart — Production-Grade E-Commerce Backend API

<div align="center">

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-Upstash-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://upstash.com/)
[![Razorpay](https://img.shields.io/badge/Razorpay-Payment%20Gateway-072654?style=for-the-badge&logo=razorpay&logoColor=white)](https://razorpay.com/)
[![Swagger](https://img.shields.io/badge/Swagger-OpenAPI%203-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](https://swagger.io/)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)

**A fully-functional, production-designed e-commerce REST API built solo** — demonstrating real-world engineering across security, payments, caching, transactional integrity, and email notifications.

[📖 API Docs](#-api-endpoints-overview) · [🚀 Quick Start](#-getting-started-local-development) · [🏗️ Architecture](#️-architecture--design-decisions) · [💻 Stack](#-technology-stack)

</div>

---

## 💡 What Makes This Different

Most portfolio backends are CRUD wrappers. CognitoCart is built the way a real startup would build it — with deliberate decisions at every layer:

| Concern | What Was Done |
|---|---|
| **Security** | Stateless JWT + refresh token rotation + RBAC, not just "add Spring Security" |
| **Payments** | Full Razorpay integration — order creation, HMAC signature verification, async webhook, duplicate-payment guard |
| **Caching** | Cache-aside pattern with Upstash Redis — per-cache TTLs, JSON serialization, real-time console logging |
| **Data Integrity** | Schema-first (Flyway) + `@Transactional` across critical flows — stock deduction + order creation in one atomic unit |
| **Email** | Async, non-blocking email with beautiful HTML Thymeleaf templates for order confirmation, status updates, and KYC |
| **Architecture** | Clean layered design — entities never leak to API layer, DTOs everywhere, centralized error handling |

---

## ✨ Feature Set

### 🔐 Authentication & Authorization
- JWT access tokens (15 min) + refresh tokens (1 hr) with **token rotation**
- Role-based access control: `ADMIN` / `SELLER` / `CUSTOMER`
- `@PreAuthorize` method-level security on every sensitive operation
- Auto-seeded admin account on first startup via `DataInitializer`

### 📦 Product Catalogue
- Full CRUD with soft delete and visibility toggling
- **SEO-friendly slug-based URLs** (`/products/apple-iphone-15-pro`)
- Infinite-depth **recursive category tree** (Electronics → Smartphones → Gaming)
- Advanced search & filtering (keyword, category, min/max price, min rating) using JPA `Specification`
- Image upload with file type validation
- **O(1) rating recalculation** using incremental moving average — no `AVG()` full scans

### 🛒 Cart & Wishlist
- Add / remove / clear items with real-time total recalculation
- **5-step pricing pipeline**: gross subtotal → coupon discount → net subtotal → delivery fee → final total
- Coupon application with per-user usage tracking, global usage limits, `firstOrderOnly` flag, min order amount
- Atomic wishlist-to-cart transfer in a single transaction

### 💳 Payment Integration (Razorpay)
- Checkout creates a Razorpay order and snapshots critical data (prices, addresses) atomically
- `/verify` endpoint validates HMAC-SHA256 signature — **no signature, no status update**
- Async webhook listener `/webhook` for server-side payment confirmation
- **Duplicate payment guard** — idempotent, safe to call multiple times
- Order status promoted to `PAID` only after cryptographic verification

### 📋 Order Management
- Full lifecycle: `PAYMENT_PENDING` → `PAID` → `CONFIRMED` → `PACKED` → `SHIPPED` → `DELIVERED`
- Admin-controlled status transitions with **email notification on each change**
- Order cancellation with **automatic stock restoration**
- Abandoned order cleanup via a scheduled background task

### 📧 Email Notifications (Async)
- Beautiful HTML email templates via Thymeleaf + Spring Mail
- Order confirmation, status update (per transition), welcome email, seller KYC result
- Non-blocking `@Async` execution — payment verification never waits for SMTP

### 🏪 Seller Portal
- Seller registration with KYC fields (GSTIN, PAN, business address)
- Dashboard: revenue, pending revenue, total orders, active/out-of-stock products, top products
- Product management scoped to the authenticated seller

### 🛠️ Admin Dashboard
- Platform revenue (total and per-period)
- Low stock alerts and top-selling products
- KYC approval / rejection with email trigger
- Order status management

### ⚡ Redis Caching (Upstash)
- Cache-aside via Spring's declarative `@Cacheable` / `@CacheEvict`
- Custom `LoggingCacheManager` decorator — every HIT, MISS, PUT, EVICT logged to console
- Per-cache TTLs (products: 10 min, categories: 60 min)
- JSON serialization (human-readable in Upstash Data Browser, not binary)

### 🚦 Rate Limiting
- Per-IP rate limiting via **Bucket4j + Redis** (token bucket algorithm)
- Protects all endpoints transparently through a `OncePerRequestFilter`

---

## 🏗️ Architecture & Design Decisions

### Layered Architecture
```
┌─────────────────────────────────────────────────────────┐
│                    Client / API Consumer                │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTP
┌──────────────────────────▼──────────────────────────────┐
│   Security Layer   Rate Limit Filter → JWT Filter       │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│   Controller Layer   @RestController  (10 controllers)  │
│   DTOs only — entities never exposed directly           │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│   Service Layer   Business Logic  (13 services)         │
│   @Transactional · @Cacheable · @CacheEvict · @Async    │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│   Repository Layer   Spring Data JPA + JPQL queries     │
│   JOIN FETCH for lazy collections · custom @Query       │
└──────────────────────────┬──────────────────────────────┘
                           │
          ┌────────────────┴────────────────┐
          ▼                                 ▼
   ┌─────────────┐                  ┌─────────────┐
   │ PostgreSQL  │                  │  Redis      │
   │ (primary)   │                  │  (Upstash)  │
   └─────────────┘                  └─────────────┘
```

### Schema-First Development
Flyway manages all schema changes as versioned SQL scripts (`V1__`, `V2__`, ...). Hibernate is set to `validate` — it never auto-creates tables. Every production deployment has a documented, reproducible migration path.

### Transactional Integrity
Critical flows are protected with Spring's `@Transactional`:
- **Checkout**: stock deducted + order record created as one atomic unit — rolls back if stock is insufficient
- **Cart add**: item saved + totals recalculated + coupon re-validated in the same session
- **Payment verify**: signature validated + order status updated + email triggered atomically

### Payment Security
```
Frontend           Spring Boot            Razorpay
────────           ──────────            ────────
[Pay]      ──►    [Create order]   ──►   [Order created]
           ◄──    [razorpay_order_id]
[SDK pays]  ──►   [receipt server-side]
           ──►    [/verify]
                  [HMAC validate]
                  ✓ razorpay_signature = HMAC_SHA256(
                      orderId + "|" + paymentId,
                      webhookSecret
                  )
```

---

## 💻 Technology Stack

| Layer | Technology | Why |
|---|---|---|
| Language | Java 21 | LTS, virtual threads ready |
| Framework | Spring Boot 3.4.1 | Industry standard, production-proven |
| Security | Spring Security + JJWT 0.12.6 | Stateless JWT, fine-grained RBAC |
| Database | PostgreSQL 15 | ACID, relational, production-grade |
| ORM | Spring Data JPA / Hibernate | Repository pattern, JPQL, specifications |
| Cache | Redis via Upstash + Spring Cache | Serverless, free tier, sub-ms reads |
| Migrations | Flyway | Schema-first, reproducible, auditable |
| Payments | Razorpay Java SDK | HMAC verification, webhook, test mode |
| Email | Spring Mail + Thymeleaf | Async SMTP, templated HTML emails |
| Validation | Jakarta Bean Validation | `@Valid`, `@Pattern`, `@NotBlank` on all DTOs |
| Docs | SpringDoc OpenAPI 3 + Swagger UI | Interactive docs at `/swagger-ui.html` |
| Build | Maven | Standard Java build lifecycle |
| Utilities | Lombok, Spring AOP, Spring Retry | Less boilerplate, cross-cutting concerns |
| Observability | Spring Actuator | Health, metrics, environment endpoints |

---

## 📁 Project Structure

```
smartcart/
├── src/main/java/com/manish/smartcart/
│   ├── config/                  # Security, JWT, Redis, Swagger, Data Initializer
│   │   ├── jwt/                 # JwtUtil, JwtFilter
│   │   └── initializer/         # AdminProperties, DataInitializer
│   ├── controller/              # REST endpoints (10 controllers)
│   ├── service/                 # Business logic (13 services)
│   │   └── notifications/       # OrderNotificationService (async email)
│   ├── repository/              # Spring Data JPA repositories with JPQL
│   ├── model/                   # JPA entities
│   │   ├── base/                # BaseEntity (id, timestamps, auditing, soft delete)
│   │   ├── user/                # Users, CustomerProfile, SellerProfile, Address
│   │   ├── product/             # Product, Category
│   │   ├── cart/                # Cart, CartItem
│   │   ├── order/               # Order, OrderItem, Coupon
│   │   └── feedback/            # Review
│   ├── dto/                     # Request/Response DTOs (entities never leak to API)
│   ├── mapper/                  # Entity ↔ DTO mapper classes
│   ├── enums/                   # Role, OrderStatus, PaymentStatus, DiscountType, KycStatus
│   └── util/                    # AppConstants, PhoneUtil, FileValidator
├── src/main/resources/
│   ├── application-demo.yml     # Reference config (fake values — safe to commit)
│   ├── application.yml          # Real config (gitignored)
│   ├── templates/emails/        # Thymeleaf HTML email templates
│   └── db/migration/            # Flyway SQL scripts (V1__init → V10__...)
├── test-payment.html            # Razorpay sandbox tester (local dev use)
└── pom.xml
```

---

## 🚀 Getting Started (Local Development)

### Prerequisites
- **Java 21** (Eclipse Temurin recommended)
- **Maven 3.8+** (or use the included `./mvnw` wrapper)
- **PostgreSQL 15+** running locally
- **Redis** — Sign up free at [upstash.com](https://upstash.com) (no credit card)
- **Razorpay** test account at [razorpay.com](https://dashboard.razorpay.com/)
- **Gmail App Password** → [generate here](https://myaccount.google.com/apppasswords)

---

### 1. Clone
```bash
git clone https://github.com/manish5200/CognitoCart.git
cd CognitoCart/smartcart
```

### 2. Database Setup
```sql
CREATE DATABASE cognitocart;
CREATE USER cognitocart WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE cognitocart TO cognitocart;
```

### 3. Configure
```bash
# Windows
copy src\main\resources\application-demo.yml src\main\resources\application.yml

# macOS / Linux
cp src/main/resources/application-demo.yml src/main/resources/application.yml
```

Open `application.yml` and fill in your values:

| Key | Value |
|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/cognitocart` |
| `spring.datasource.username` | Your PostgreSQL user |
| `spring.datasource.password` | Your PostgreSQL password |
| `spring.data.redis.url` | Your Upstash Redis URL (`rediss://...`) |
| `spring.mail.username` | Gmail address |
| `spring.mail.password` | Gmail **App Password** |
| `application.security.jwt.secret-key` | `openssl rand -base64 32` |
| `RAZORPAY_KEY_ID` | Razorpay test key ID |
| `RAZORPAY_KEY_SECRET` | Razorpay test key secret |
| `RAZORPAY_WEBHOOK_SECRET` | Razorpay webhook secret |
| `admin.email` / `admin.password` | Seed admin credentials |

> ⚠️ `application.yml` is gitignored — never commit your credentials.

### 4. Run
```bash
# Windows
.\mvnw spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

### 5. Verify
| URL | Expected |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Interactive API docs |
| `http://localhost:8080/actuator/health` | `{"status":"UP"}` |

> On first startup: Flyway runs all migrations, admin account is auto-seeded, and the category tree (61 categories across 10 domains) is populated.

---

## 📬 API Endpoints Overview

| Domain | Base Path | Auth Required |
|---|---|---|
| Authentication | `/api/v1/auth/**` | Public |
| Products (browse) | `GET /api/v1/products/**` | Public |
| Products (manage) | `POST/DELETE /api/v1/products/**` | SELLER / ADMIN |
| Categories | `/api/v1/categories/**` | ADMIN (write), Public (read) |
| Cart | `/api/v1/cart/**` | CUSTOMER |
| Wishlist | `/api/v1/wishlist/**` | CUSTOMER |
| Orders | `/api/v1/orders/**` | CUSTOMER |
| Payments | `/api/v1/payments/verify` · `/webhook` | Public (signature-verified) |
| Reviews | `/api/v1/reviews/**` | CUSTOMER (write), Public (read) |
| Addresses | `/api/v1/addresses/**` | CUSTOMER |
| Seller | `/api/v1/seller/**` | SELLER |
| Admin | `/api/v1/admin/**` | ADMIN |

> Full interactive documentation: **[Swagger UI](http://localhost:8080/swagger-ui.html)**

---

## 🔐 Security Notes

- `application.yml` and `application-dev.yml` are **gitignored** — only `application-demo.yml` with placeholder values is committed
- JWT secrets must be ≥ 256-bit, BASE64URL-encoded — generate: `openssl rand -base64 32`
- Razorpay keys are consumed via environment variables, never hardcoded
- Webhook endpoint validates HMAC-SHA256 signature before any state change
- All write operations require authenticated JWT with the appropriate role

---

## 🗺️ Roadmap

The following features are planned for upcoming development phases:

- [ ] **Logout** — JWT blocklist via Redis for token revocation
- [ ] **Email Verification** — OTP on signup before account activation
- [ ] **Password Reset** — Secure time-limited reset token flow
- [ ] **Cloud Storage** — AWS S3 / Cloudinary for product images
- [ ] **Refund Flow** — Razorpay refund API integration on order cancellation
- [ ] **PDF Invoices** — Downloadable receipts per order
- [ ] **Shipment Tracking** — Tracking number on orders, courier integration
- [ ] **AI Recommendations** — Personalized product suggestions (embeddings)
- [ ] **Semantic Search** — Natural language product search (pgvector / OpenAI)
- [ ] **Fraud Detection** — Anomaly scoring on payment events

---

## 👨‍💻 Author

**Manish Kumar Singh**
*Backend Engineer — Java · Spring Boot · System Design*

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/maniish5200/)
[![GitHub](https://img.shields.io/badge/GitHub-Follow-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/manish5200)

> Built independently as a full solo project — every line of code, every design decision, every production pattern is original work.
> *#100DaysOfCode — building production-grade quality from day one.*

---

<div align="center">
  <sub>⭐ If this project helped you, consider giving it a star — it means a lot to a solo developer!</sub>
</div>
