# 🛒 CognitoCart — AI-Driven E-Commerce Backend API

[![Build](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen?logo=springboot)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)]()
[![Redis](https://img.shields.io/badge/Redis-Upstash-red?logo=redis)]()
[![Flyway](https://img.shields.io/badge/Flyway-migrations-red?logo=flyway)]()
[![Swagger](https://img.shields.io/badge/Swagger-OpenAPI%203-green?logo=swagger)]()
[![License](https://img.shields.io/badge/license-MIT-green)]()

> **An enterprise-grade e-commerce REST API built with Java & Spring Boot — designed for performance, security, and real-world scalability.**

---

## 📖 Overview

**CognitoCart** is a full-featured e-commerce backend that handles the complete shopping lifecycle: product discovery, cart management, wishlist, order processing, reviews, seller management, and admin operations — all secured with JWT authentication and RBAC.

The core philosophy is **"Production by Design"** — every decision is made with scalability, security, and maintainability in mind.

---

## ✨ Features

| Domain | Capabilities |
|--------|-------------|
| 🔐 **Auth** | JWT access tokens, refresh tokens, role-based access (ADMIN / SELLER / CUSTOMER) |
| 📦 **Products** | CRUD, image upload, soft delete, slug-based URLs, full-text search |
| 🗂️ **Categories** | Infinite-depth recursive tree (Electronics → Audio → Headphones) |
| 🛒 **Cart** | Add/update/remove items, total auto-calculation, atomic wishlist-to-cart transfer |
| ❤️ **Wishlist** | Add/remove products, move to cart as a single transaction |
| 📋 **Orders** | Place order with real-time stock deduction, status management (PLACED → SHIPPED → DELIVERED) |
| ⭐ **Reviews** | Submit review, O(1) rating recalculation via incremental moving average |
| 📍 **Addresses** | Add/update/delete delivery addresses per user |
| 👤 **Customer** | Customer profile, loyalty points, dashboard |
| 🏪 **Seller** | Seller profile, KYC status, product management |
| 🛠️ **Admin** | Dashboard analytics, low stock alerts, top products, user/order management |
| 📧 **Email** | Async SMTP email service (order notifications, welcome emails) |
| ⚡ **Redis Cache** | Cache-aside pattern via Upstash — products, categories, recommendations cached with TTL |

---

## 🏗️ Architecture & Key Concepts

### 1. ⚡ O(1) Rating Math (Incremental Moving Average)
Instead of `SELECT AVG(rating)` on every request (which gets slower as reviews grow), we store `averageRating` and `totalReviews` directly on the `Product` entity and update them using math on each new review — **constant time, zero full-table scans**.

### 2. 🔒 Hardened Security
- **Stateless JWT** — 256-bit BASE64URL secret, short-lived access tokens (15 min) + long-lived refresh tokens (1 hr)
- **RBAC** — `ADMIN`, `SELLER`, `CUSTOMER` roles enforced via `@PreAuthorize`
- **Non-root Docker** — container runs as unprivileged `spring` user

### 3. 🔄 Transactional Integrity
`@Transactional` guarantees atomicity across critical flows:
- **Place Order** → stock deducted + order record created as one atomic unit; rolls back if stock is insufficient
- **Wishlist → Cart** → items moved across tables without orphaned data

### 4. 🗃️ Schema-First with Flyway
All database schema changes are versioned SQL migrations via **Flyway**. Hibernate is set to `validate` only — it never auto-creates or modifies tables. This ensures safe production deployments and a reproducible schema.

### 5. ⚡ Redis Caching (Upstash) — Cache-Aside Pattern

Every high-traffic read endpoint is cached in **Redis** (hosted on [Upstash](https://upstash.com) — serverless, free tier). The implementation uses Spring's declarative caching abstraction (`@Cacheable` / `@CacheEvict`) wired to a custom `RedisCacheManager` with per-cache TTLs and JSON serialization.

#### 🗂️ What Gets Cached

| Cache Name | Method | TTL | Invalidated When |
|---|---|---|---|
| `products` | `getAllProducts()` | 10 min | Product created / deleted / toggled |
| `products` | `getProductsByCategoryIds(ids)` | 10 min | Product created / deleted / toggled |
| `product-slug` | `getProductBySlug(slug)` | 10 min | Product updated / deleted |
| `categories` | `getAllCategories()` | 60 min | Category created (bulk or single) |
| `product-recommendations` | *(future AI feature)* | 60 min | Pre-registered, ready to use |

#### 🔄 How It Works — Cache-Aside Pattern

```
📥 Incoming Request
        │
        ▼
  ┌─────────────┐
  │ Redis Cache │◄──── key exists? ────► ✅ Cache HIT → Return instantly (< 1ms)
  └─────────────┘
        │
      MISS
        │
        ▼
  ┌─────────────┐
  │ PostgreSQL  │──── fetch data ────► 💾 Store in Redis with TTL
  └─────────────┘
        │
        ▼
   Return Response

🔥 On Write (Create / Update / Delete)
        │
        ▼
  Update Database → 🗑️ Evict Redis entries → Next read re-populates cache
```

#### 📟 Real-Time Cache Console Logging

Every cache operation is logged to the console via a custom **`LoggingCacheManager`** decorator — a transparent wrapper around `RedisCacheManager` that intercepts every `get`, `put`, `evict`, and `clear` call.

> No annotation or service code changes needed — the logging layer is entirely in the infrastructure.

**First request (cache MISS → DB query → stored in Redis):**
```log
🔴 CACHE MISS → [products] :: key='all' | Querying database...
💾 CACHE PUT  → [products] :: key='all' | Storing result in Redis
```

**Second request (cache HIT → served from Redis, zero DB query):**
```log
✅ CACHE HIT  → [products] :: key='all'
```

**When a product is created or deleted (cache eviction):**
```log
🗑️  CACHE CLEAR → [products] :: All entries evicted
🗑️  CACHE CLEAR → [product-slug] :: All entries evicted
```

**Category cache (60 min TTL — rarely invalidated):**
```log
🔴 CACHE MISS → [categories] :: key='all' | Querying database...
💾 CACHE PUT  → [categories] :: key='all' | Storing result in Redis
✅ CACHE HIT  → [categories] :: key='all'   ← all subsequent requests
```

#### 🛠️ Implementation Architecture

```
 RedisConfig.java
       │
       ├── redisObjectMapper()          → Jackson: LocalDateTime + type-info support
       ├── defaultCacheConfig()         → String keys + JSON values + no null caching  
       └── cacheManager() [@Primary]    → RedisCacheManager wrapped in LoggingCacheManager

 LoggingCacheManager.java (Decorator Pattern)
       │
       └── LoggingCache (inner class)   → Intercepts get/put/evict, logs with emoji prefix

 ProductService.java
       ├── @Cacheable  getAllProducts()              → cache: products  key: 'all'
       ├── @Cacheable  getProductBySlug(slug)        → cache: product-slug  key: #slug
       ├── @Cacheable  getProductsByCategoryIds(ids) → cache: products  key: hashCode
       ├── @CacheEvict createProduct()              → evicts: products + product-slug
       ├── @CacheEvict toggleAvailability()         → evicts: products + product-slug
       └── @CacheEvict deleteProduct()              → evicts: products + product-slug

 CategoryService.java
       ├── @Cacheable  getAllCategories()            → cache: categories  key: 'all'
       ├── @CacheEvict createCategory()             → evicts: categories
       └── @CacheEvict createCategoriesBulk()       → evicts: categories
```

#### ⚙️ Redis Configuration (Upstash Setup)

1. Sign up at **[upstash.com](https://upstash.com)** (free, no credit card)
2. Create a database → Region: `ap-south-1` (Mumbai)
3. Copy your `REDIS_URL` from the **Details** tab
4. Add to `application-demo.yml`:

```yaml
spring:
  data:
    redis:
      url: rediss://default:PASSWORD@HOST.upstash.io:6379
      ssl:
        enabled: true
  cache:
    type: redis
    redis:
      key-prefix: "cognitocart::"
      use-key-prefix: true
      cache-null-values: false
```

> **Key design decisions:**
> - `rediss://` (double `s`) = TLS connection required by Upstash
> - `key-prefix: cognitocart::` — namespaces all keys in the shared Redis DB (e.g. `cognitocart::products::all`)
> - `cache-null-values: false` — never waste Redis memory on null results
> - JSON serialization (not Java binary) — keys are human-readable in **Upstash Data Browser**
> - `ProductMapper` copies `tags` and `imageUrls` to plain `HashSet`/`ArrayList` before caching — Hibernate `PersistentSet` is session-bound and cannot be serialized after the session closes

---

### 6. 🏛️ Clean Layered Architecture
```
Controller → Service → Repository → Database
     ↕              ↕
   DTO           Domain Model
```
- **DTOs** for all request/response — entities never leak to the API layer
- **`AppConstants`** — single source of truth for magic strings and thresholds
- **`@RestControllerAdvice`** — centralized, standardized JSON error responses
- **`@Async` Email** — email sending is non-blocking, runs in a separate thread pool

---

## 💻 Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1 |
| Security | Spring Security + JJWT 0.12.6 |
| Database | PostgreSQL 15 |
| ORM | Spring Data JPA / Hibernate |
| Cache | Redis (Upstash) + Spring Cache |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| Documentation | SpringDoc OpenAPI 3 / Swagger UI |
| Email | Spring Mail (Jakarta Mail / SMTP) |
| Build | Maven |
| Utilities | Lombok, Spring Retry, Spring AOP |
| Observability | Spring Actuator |

---

## 📁 Project Structure

```
smartcart/
├── src/main/java/com/manish/smartcart/
│   ├── config/                  # Security, JWT, Web, Swagger, Data Initializer
│   │   ├── jwt/                 # JwtUtil, JwtFilter
│   │   └── initializer/         # AdminProperties, DataInitializer
│   ├── controller/              # REST endpoints (10 controllers)
│   ├── service/                 # Business logic (13 services)
│   ├── repository/              # Spring Data JPA repositories
│   ├── model/                   # JPA entities
│   │   ├── base/                # BaseEntity (id, timestamps, auditing)
│   │   ├── user/                # Users, CustomerProfile, SellerProfile, Address
│   │   ├── product/             # Product, Category
│   │   ├── cart/                # Cart, CartItem
│   │   ├── order/               # Order, OrderItem
│   │   └── feedback/            # Review
│   ├── dto/                     # Request/Response DTOs
│   ├── enums/                   # Role, OrderStatus, PaymentStatus, KycStatus etc.
│   └── util/                    # AppConstants, PhoneUtil, FileValidator
├── src/main/resources/
│   ├── application-demo.yml     # ← Reference config with fake values (safe to commit)
│   ├── application.yml          # ← Your real config (gitignored — never committed)
│   ├── application-dev.yml      # ← Your dev config (gitignored — never committed)
│   └── db/migration/            # Flyway SQL migration scripts (V1, V2, V3...)
├── Dockerfile                   # Multi-stage Docker build (gitignored — WIP)
└── docker-compose.yml           # App + PostgreSQL (gitignored — WIP)
```

---

## 🚀 Getting Started (Local Development)

### Prerequisites
- **Java 17+** (Eclipse Temurin / OpenJDK)
- **Maven 3.8+** (or use the included `mvnw` wrapper)
- **PostgreSQL 15+** running locally

### 1. Clone the Repository
```bash
git clone https://github.com/manish5200/CognitoCart.git
cd CognitoCart/smartcart
```

### 2. Set Up the Database
```sql
-- Run in psql or pgAdmin
CREATE DATABASE cognitocart;
CREATE USER cognitocart WITH PASSWORD 'cognitocart';
GRANT ALL PRIVILEGES ON DATABASE cognitocart TO cognitocart;
```

### 3. Create Your Local Config Files

The repository ships with **`application-demo.yml`** (fake placeholder values — safe for Git).  
The real config files (`application.yml`, `application-dev.yml`) are **gitignored** and contain your actual credentials.

```bash
# Create real config files by copying the demo template
# Windows
copy src\main\resources\application-demo.yml src\main\resources\application.yml
copy src\main\resources\application-demo.yml src\main\resources\application-dev.yml

# macOS / Linux
cp src/main/resources/application-demo.yml src/main/resources/application.yml
cp src/main/resources/application-demo.yml src/main/resources/application-dev.yml
```

Then open **`application.yml`** and **`application-dev.yml`** and fill in your real values:

| Config Key | What to put |
|------------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/cognitocart` |
| `spring.datasource.username` | Your PostgreSQL username |
| `spring.datasource.password` | Your PostgreSQL password |
| `application.security.jwt.secret-key` | 256-bit BASE64URL key — generate: `openssl rand -base64 32` |
| `spring.mail.username` | Your Gmail address |
| `spring.mail.password` | Gmail **App Password** → [generate here](https://myaccount.google.com/apppasswords) |
| `admin.email` | Seed admin email (auto-created on first startup) |
| `admin.password` | Seed admin password |

> ⚠️ **Never commit** `application.yml` or `application-dev.yml` — they are gitignored to protect your secrets.

### 4. Run the Application
```bash
# Using Maven wrapper (recommended)
.\mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # Windows
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # macOS/Linux
```

### 5. Verify
| URL | What to expect |
|-----|---------------|
| `http://localhost:8080/swagger-ui.html` | Interactive API documentation |
| `http://localhost:8080/actuator/health` | `{"status":"UP"}` |

> On first startup, Flyway runs all migrations and the admin account is auto-seeded.

---

## 📬 API Endpoints Overview

| Group | Base Path | Access |
|-------|-----------|--------|
| Auth | `/api/v1/auth/**` | Public |
| Products (browse) | `GET /api/v1/products` | Public |
| Products (manage) | `/api/v1/products/**` | SELLER / ADMIN |
| Categories | `/api/v1/categories/**` | ADMIN (write), Public (read) |
| Cart | `/api/v1/cart/**` | CUSTOMER |
| Wishlist | `/api/v1/wishlist/**` | CUSTOMER |
| Orders | `/api/v1/orders/**` | CUSTOMER / ADMIN |
| Reviews | `/api/v1/reviews/**` | CUSTOMER (write), Public (read) |
| Addresses | `/api/v1/addresses/**` | CUSTOMER |
| Customer | `/api/v1/customers/**` | CUSTOMER |
| Admin | `/api/v1/admin/**` | ADMIN |

> Full interactive documentation at **[Swagger UI](http://localhost:8080/swagger-ui.html)** when running locally.

---

## 🐋 Docker (Coming Soon)

> Docker support is **work in progress** — `Dockerfile` and `docker-compose.yml` are excluded from the repository until the deployment phase is ready.
>
> For now, run the app natively using Maven for the fastest development loop (hot-reload via `spring-boot-devtools`).

---

## 🔐 Security Notes

- `application.yml` and `application-dev.yml` are **gitignored** — only `application-demo.yml` (fake values) is committed
- JWT secrets must be at least **256-bit** (32 bytes), encoded as BASE64URL — generate with `openssl rand -base64 32`
- Use **Gmail App Passwords** — never your real account password
- Admin seed credentials should be changed after the first login in production

---

## 👨‍💻 Author

**Manish Kumar Singh**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-blue?style=for-the-badge&logo=linkedin)](https://www.linkedin.com/in/maniish5200/)

> Built with ☕ during the **#100DaysOfCode** challenge — aiming for production-grade quality from day one.
