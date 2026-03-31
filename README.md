<div align="center">

# 🛒 CognitoCart
### **Enterprise-Grade E-Commerce API · AI Search · RabbitMQ Event-Driven · Prometheus Observability · Distributed Systems**

[![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL_pgvector-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis_Upstash-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://upstash.com/)
[![HuggingFace](https://img.shields.io/badge/HuggingFace_AI-FFD21E?style=for-the-badge&logo=huggingface&logoColor=black)](https://huggingface.co/)
[![Razorpay](https://img.shields.io/badge/Razorpay_API-072654?style=for-the-badge&logo=razorpay&logoColor=white)](https://razorpay.com/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)](https://prometheus.io/)
[![Grafana](https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white)](https://grafana.com/)

> Most portfolio projects stop at basic CRUD.
> **CognitoCart** tackles the brutal edge cases that define real production systems — preventing stock manipulation under concurrency, surviving double-charge payment failures, decoupling heavy work via RabbitMQ event streams, monitoring every JVM metric live on Grafana Cloud, and finding products by **mathematical meaning** using AI embeddings and pgvector.

[Core Architecture](#️-the-engineering) · [AI Features](#-phase-4--ai-features) · [API Domains](#-api-infrastructure) · [Quick Start](#-quick-start-guide)

</div>

---

## 🏗️ The Engineering

CognitoCart rejects simplified logic in favor of robust, distributed architecture. Here is exactly what it handles gracefully:

### 🛡️ Transactional Integrity & Concurrency
> **The Threat:** Two users click 'Checkout' on the last unit simultaneously.
> **The Solution:** **Pessimistic Locking (`SELECT FOR UPDATE`)** in PostgreSQL — the checkout flow mathematically locks the row, evaluates promotions, deducts stock, and generates the invoice within a single immutable `@Transactional` boundary.

### 💳 Webhook Idempotency & Resiliency
> **The Threat:** A user's internet drops after paying Razorpay, or the webhook fires twice simultaneously.
> **The Solution:** Dual lifecycle modeling (`orderStatus` & `paymentStatus`) + a strictly **idempotent Redis `SETNX` lock layer**. An async DLQ processes HMAC-SHA256 verified webhooks — if a node fails mid-transaction, it retries safely without double-charging.

### 🔒 Enterprise Identity Architecture (True Logout)
> **The Threat:** Stolen JWTs or "Logout" that doesn't actually invalidate tokens.
> **The Solution:** **Redis-backed JWT blacklist** — every token's `jti` identifier is stamped into Redis with a TTL matching its remaining lifespan. Every backend entry-point intercepts and denies blacklisted tokens aggressively.

### 🤖 AI Semantic Search (Phase 4 — Live)
> **The Threat:** Traditional `LIKE '%keyword%'` search fails when users think in sentences, not keywords.
> **The Solution:** Product descriptions are converted to **384-dimensional mathematical vectors** via HuggingFace AI and stored in PostgreSQL using the `pgvector` extension. Search queries undergo the same transformation, and **cosine similarity** finds products by meaning — not by character matching.

---

## 🤖 Phase 4 — AI Features

### Semantic Vector Search

CognitoCart goes beyond keyword matching. When a product is created, its name + description + tags are converted into a 384-number mathematical "meaning vector" by a HuggingFace AI model. When a user searches, their query is transformed the same way, and PostgreSQL finds the closest vectors using cosine similarity.

**Real Proof — Zero Keyword Overlap:**

| User Searches For | Top Result Returned | Why It Works |
|---|---|---|
| `"earphones for blocking noise while studying"` | Noise Cancelling Headphones | AI maps "earphones" → "headphones", "blocking noise" → "noise cancellation" |
| `"something to brew hot drinks at the office"` | Espresso Coffee Machine | AI maps "brew hot drinks" → "espresso/coffee", "office" → "home-office" |
| `"comfortable footwear for morning fitness routine"` | Running Shoes | AI maps "footwear" → "shoes", "fitness routine" → "marathon/jogging" |

**None of the search words appear in the product names or descriptions. The AI understands meaning.**

**How to Test:**
```bash
# No authentication required — open to all users
GET /api/v1/products/search/semantic?q=comfortable footwear for morning fitness routine&limit=5

# Response: Returns Running Shoes ranked #1, even though "footwear" ≠ "shoes" in the product name
```

**How It Works Internally:**
```
User Query: "earphones for blocking noise while studying"
     ↓ HuggingFace all-MiniLM-L6-v2
Float Vector: [0.021, -0.455, 0.891, ... 384 numbers]
     ↓ PostgreSQL pgvector
SELECT * FROM products ORDER BY embedding <=> CAST(:query AS vector) LIMIT 10
     ↓
Top Match: "Noise Cancelling Headphones" (cosine distance: 0.12 — very close)
```

### AI Review Summarization (Sentiment Insights)

Users don't have time to read hundreds of reviews. CognitoCart uses the **HuggingFace BART (Large CNN)** model to aggregate raw review clusters into a single, high-impact sentiment summary. 

- **Intelligence:** Automatically identifies recurring pros/cons (e.g., "Loved the bass, but the ear-tips are stiff").
- **Performance:** Summaries are pre-computed by a Spring `@Scheduled` background worker and saved to a dedicated `ProductInsights` table for instant retrieval.
- **Scale:** Uses a `@Transactional` + `JOIN FETCH` optimized repository query to process the entire catalog in a single SQL operation, avoiding N+1 bottlenecks.

**How to Test:**
```bash
# Check the 'aiSummary' field in any product response payload
GET /api/v1/products/{slug}
```

---

## ✨ System Capabilities

<details open>
<summary><b>B2B / B2C Operations</b></summary>
<br>

- **Multi-Tenant Scopes:** `ADMIN`, `SELLER`, and `CUSTOMER` endpoint isolation with role-restricted payloads.
- **Dynamic Pricing Engine:** 5-stage checkout pipeline — Base Price → Coupon Validation → BOGO/Category Discounts → Delivery Offset → Net Payable.
- **Seller KYC Pipeline:** Approval lifecycle for third-party sellers, gated by global Admins.
- **Wishlist Intelligence:** Autonomous scheduler cross-referencing wishlists against active sales, sending personalised HTML digest emails.

</details>

<details open>
<summary><b>Performance & Scale</b></summary>
<br>

- **AI Semantic Search:** pgvector IVFFlat index for sub-millisecond Approximate Nearest Neighbor cosine similarity across the entire product catalog.
- **Sub-Millisecond Caching:** `@Cacheable` directives tied to **Upstash Redis**, dramatically offloading product and category DB reads with native eviction triggers.
- **DDoS Mitigation:** Per-IP Token Bucket rate limiting via **Bucket4j** built into the Spring Security filter chain.
- **Cloud Content Delivery:** Direct binary integration with the **Cloudinary CDN** — zero local disk dependency.
- **Background Processing:** Spring `@Scheduled` threads scan for Flash Sales every 10 seconds, generating native HTML FOMO emails via Thymeleaf with timestamp-locks to prevent spam.

</details>

<details open>
<summary><b>Automated Logistics</b></summary>
<br>

- **Instant Refund Processing:** Cancellation protocols invoke Razorpay's Reversal API and dispatch `rfnd_XXXXX` receipts automatically.
- **Dynamic PDF Invoices:** On-the-fly PDF generation via **iText7** — GSTIN variables, zebra-striped tables, mathematical delivery dates.
- **State Machine Enforcement:** Shipments are forced through `PLACED → SHIPPED → OUT_FOR_DELIVERY` — illegal backward state transitions are rejected at the API layer.

</details>

---

## 🗺️ System Architecture

```mermaid
graph TD
    Client((Client App)) --> RL[⚙️ Bucket4j Rate Limiter]
    RL --> JWT[🔒 JWT Auth Filter]
    JWT --> API[REST Controllers]

    API --> SVC[Service Layer]
    API -->|OrderPaidEvent| MQ[🐇 RabbitMQ CloudAMQP]

    SVC <--> Cache[(⚡ Upstash Redis)]
    SVC <--> DB[(🐘 PostgreSQL + pgvector + ShedLock)]
    SVC --> AI[🤖 HuggingFace Embeddings API]

    MQ -->|async consumer| Worker[📦 OrderRabbitListener]
    Worker --> PDF[iText7 PDF Engine]
    Worker --> Email[Gmail SMTP]

    AI -->|float384 vector| DB
    DB -->|Cosine Similarity| SEARCH[Semantic Search Results]
    DB -.->|Distributed Lock| SCHED[4x ShedLock Schedulers]
```

---

## 💻 Tech Stack

| Layer | Technology | Purpose |
|---|---|---|
| **Runtime** | Java 21 / Spring Boot 3.4 | LTS stability, Virtual Threads readiness |
| **Database** | PostgreSQL + pgvector | ACID transactions + vector similarity search |
| **Message Broker** | RabbitMQ (CloudAMQP) + DLQ | Async event-driven PDF & email processing, zero message loss |
| **Distributed Lock** | ShedLock + JDBC | Safe multi-instance scheduler coordination |
| **AI Embeddings** | HuggingFace `all-MiniLM-L6-v2` | Free 384-dim semantic text embeddings |
| **Migrations** | Flyway | Deterministic, version-controlled schema evolution |
| **Cache & State** | Upstash Redis | JWT blacklists, OTPs, idempotency locks, read caching |
| **Payments** | Razorpay SDK | Orders, webhooks, instant refunds |
| **Media CDN** | Cloudinary | Scalable image storage & delivery |
| **Render Engine** | iText7 + Thymeleaf | PDF invoices + HTML email templates |
| **Security** | Spring Security + Bucket4j | JWT auth + per-IP rate limiting |
| **Observability** | Micrometer + Prometheus + Grafana Cloud | 200+ live metrics — JVM, DB pool, RabbitMQ, HTTP latency, custom counters |
| **API Docs** | SpringDoc OpenAPI (Swagger) | Interactive documentation |

---

## 🚀 Quick Start Guide

### 1. Prerequisites
- **Java 21+**, **Maven 3.8+**
- **PostgreSQL** (with `pgvector` extension installed — see below)
- **Redis** (Upstash free tier works)
- **HuggingFace** free account for semantic search

### 2. Install pgvector (Required for AI Search)
```sql
-- Run as PostgreSQL superuser (postgres) in pgAdmin:
CREATE EXTENSION IF NOT EXISTS vector;
```
> Download the pgvector binary for your OS from [github.com/pgvector/pgvector/releases](https://github.com/pgvector/pgvector/releases)

### 3. Clone & Configure
```bash
git clone https://github.com/manish5200/CognitoCart.git
cd CognitoCart/smartcart

# Create the database
psql -U postgres -c "CREATE DATABASE cognitocart;"
```

Copy `application-demo.yml` → `application.yml` and fill in your credentials:
- PostgreSQL connection details
- Redis (Upstash) URL
- Gmail SMTP App Password
- Razorpay sandbox keys
- Cloudinary API credentials
- **HuggingFace API token** (free at [huggingface.co/settings/tokens](https://huggingface.co/settings/tokens))

### 4. Run
```bash
./mvnw spring-boot:run
```
> On startup, Flyway runs **20 migrations** — enabling pgvector, seeding the Admin account, building category tree (61 nodes), and initialising all tables.

### 5. Explore
Navigate to **`http://localhost:8080/swagger-ui.html`** — 50+ OpenAPI endpoints across 10 functional domains.

**Quick test the AI search (no login required):**
```bash
curl "http://localhost:8080/api/v1/products/search/semantic?q=wireless earphones for travel&limit=5"
```

---

## 🛣️ Engineering Roadmap

<details>
<summary><b>✅ Completed Phases (1 → 4.1)</b></summary>

- **Phase 1 — Auth Hardening:** Redis JWT Blacklists · Pessimistic Stock Locks · Secure OTP verification
- **Phase 2 — Fulfillment:** Razorpay refunds · iText7 PDF invoices · Logistical state machines
- **Phase 3 — Scale:** Cloudinary CDN · Advanced JPQL analytics · Seller dashboards
- **Phase 3.5 — Operations:** DLQs for failed webhooks · Guest-to-User cart migrations
- **Phase 3.9 — Pre-AI:** Global `@SoftDelete` · Idempotency Locks · BOGO Engine · Wishlist HTML Schedulers
- **Phase 4.1 — Semantic Search ✅:** pgvector + HuggingFace AI embeddings · Cosine similarity search
- **Phase 4.3 — AI Review Summarization ✅:** HuggingFace BART Large CNN · Background `@Scheduled` workers · `ProductInsights` engine

</details>

**Phase 4 (In Progress) — Artificial Intelligence 🤖**
- [x] **Semantic Vector Search** — pgvector + HuggingFace · Cosine similarity endpoint
- [x] **AI Review Summarization** — HuggingFace BART Large CNN · Automated sentiment insights
- [ ] **Collaborative Filtering** — "Customers who bought this also bought..." co-purchase frequency matrix
- [ ] **Visual Reverse Image Search** — CLIP Embeddings + pgvector image similarity

**Phase 5 — Cloud & DevOps ☁️**
- [x] **Distributed Schedulers ✅:** `ShedLock` + PostgreSQL ACID locking across 4 background jobs in multi-instance deployments.
- [x] **Event-Driven Architecture ✅:** RabbitMQ (CloudAMQP) decouples PDF invoice generation & email dispatch from the HTTP thread — response time dropped from ~4s to <50ms. Dead Letter Queue (DLQ) ensures zero message loss on failures.
- [x] **Observability ✅:** Micrometer + Prometheus + Grafana Cloud — 200+ live metrics (JVM heap, GC cycles, HikariCP DB pool, RabbitMQ throughput, HTTP latency, custom order counters) scraped every 15s via Grafana Alloy with live dashboards.
- [ ] **Containerization:** Full Docker + docker-compose setup for one-command local stack.
- [ ] **CI/CD Pipeline:** GitHub Actions — automated build, test, and container image push on every commit.

---

## 📡 API Infrastructure

| Domain | Endpoints | Auth |
|---|---|---|
| Authentication | Register · Login · Refresh · Logout · OTP | Public / JWT |
| Products | CRUD · Slug · Category · **AI Semantic Search** 🤖 | Public GET / Seller POST |
| Search | Keyword + Filter (price, category, name) | Public |
| Orders | Place · Track · Cancel · Invoice PDF | Customer JWT |
| Payments | Razorpay Create · Webhook · Verify | Public Webhook |
| Cart | Guest Cart · Auth Cart · Merge on Login | Mixed |
| Reviews | Submit · List · Approve | Customer JWT |
| Wishlist | Add · Remove · List · Email Digest | Customer JWT |
| Seller | Dashboard · Analytics · KYC | Seller JWT |
| Admin | User Management · Reports · Promotions | Admin JWT |

---

## 👨‍💻 Primary Engineer

**Manish Kumar Singh**
*Backend Systems Engineer · Java · Spring Boot · AI Integration · Distributed Systems*

I engineer resilient software capable of surviving heavy load and dynamic scaling requirements. CognitoCart demonstrates the full spectrum — from raw PostgreSQL schema design and distributed transaction management, to integrating live AI embeddings and vector similarity search into a production REST API.

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/maniish5200/)
[![GitHub](https://img.shields.io/badge/GitHub-Follow-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/manish5200)

<div align="center">
  <sub>⭐ Star this repo if you appreciate rigorous, production-grade software engineering!</sub>
</div>
