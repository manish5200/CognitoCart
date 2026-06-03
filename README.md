<div align="center">

# 🛒 CognitoCart (Enterprise E-Commerce API)
### **Distributed Systems · Event-Driven Architecture · AI Semantic Search · Observability**

[![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL_pgvector-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis_Upstash-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://upstash.com/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ_CloudAMQP-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![OAuth2](https://img.shields.io/badge/OAuth_2.0-Google_Identity-4285F4?style=for-the-badge&logo=google&logoColor=white)](https://developers.google.com/identity/protocols/oauth2)
[![Grafana](https://img.shields.io/badge/Grafana_Alloy-F46800?style=for-the-badge&logo=grafana&logoColor=white)](https://grafana.com/)
[![HuggingFace](https://img.shields.io/badge/HuggingFace_AI-FFD21E?style=for-the-badge&logo=huggingface&logoColor=black)](https://huggingface.co/)

> **Why This Project Stands Out:** Most portfolio projects stop at basic CRUD. CognitoCart tackles the brutal edge cases that define **real production systems** — surviving double-charge payment failures with **Redis idempotency locks**, decoupling heavy workloads via **RabbitMQ event streams**, preventing race conditions under high concurrency using **Pessimistic DB Locks**, and pioneering **Mathematical AI Search** using 384-dimensional vectors.

[Core Architecture](#️-the-engineering) · [AI Features](#-ai--data-intelligence) · [System Capabilities](#-system-capabilities) · [Quick Start](#-quick-start-guide)

</div>

---

## 🏗️ The Engineering (How it solves real problems)

CognitoCart rejects simplified logic in favor of robust, distributed architecture. Here is exactly what it handles gracefully:

### 🛡️ Transactional Integrity & Concurrency (Flash Sales)
> **The Threat:** Two users click 'Checkout' on the last unit of a product simultaneously.
> **The Solution:** **Pessimistic Locking (`SELECT FOR UPDATE`)** in PostgreSQL. The checkout flow mathematically locks the row, evaluates promotions, deducts stock, and generates the invoice within a single immutable `@Transactional` boundary, preventing race conditions entirely.

### 💳 Webhook Idempotency & Resiliency
> **The Threat:** A user's internet drops after paying, or Razorpay's webhook fires twice.
> **The Solution:** Dual lifecycle modeling (`orderStatus` & `paymentStatus`) + a strictly **idempotent Redis `SETNX` lock layer**. An async DLQ processes HMAC-SHA256 verified webhooks — if a node fails mid-transaction, it retries safely without ever double-charging the customer.

### 🚄 Zero-Memory Streaming (Seller Analytics)
> **The Threat:** A seller requests a CSV export of 500,000 orders, crashing the JVM via Out-Of-Memory (OOM).
> **The Solution:** Implemented **`StreamingResponseBody`** combined with a JPA cursor (Fetch Size: 500) and manual `EntityManager.detach()`. Data streams directly to the network socket, keeping JVM memory footprint perfectly flat regardless of file size.

### 🔐 OAuth 2.0 Identity Server & Defenses
> **The Threat:** Malicious actors attempting to bypass password restrictions via social login vector endpoints.
> **The Solution:** A locked-down Spring Security filter chain tightly coupling **Google OAuth 2.0** profiles with internal accounts. It autonomously denies cross-hijacking attempts and issues dual-token JWTs securely.

---

## 🤖 AI & Data Intelligence

### Semantic Vector Search
Traditional `LIKE '%keyword%'` search fails when users think in sentences. CognitoCart converts product descriptions into **384-dimensional mathematical vectors** via HuggingFace AI, storing them in PostgreSQL (`pgvector`). 

**Real Proof — Zero Keyword Overlap:**

| User Searches For | Top Result Returned | Why It Works |
|---|---|---|
| `"earphones for blocking noise while studying"` | Noise Cancelling Headphones | AI maps "earphones" → "headphones", "blocking noise" → "noise cancellation" |
| `"something to brew hot drinks at the office"` | Espresso Coffee Machine | AI maps "brew hot drinks" → "espresso/coffee", "office" → "home-office" |
| `"comfortable footwear for morning fitness routine"` | Running Shoes | AI maps "footwear" → "shoes", "fitness routine" → "marathon/jogging" |

### AI Review Summarization
Uses the **HuggingFace BART (Large CNN)** model to aggregate raw product review clusters into a single, high-impact sentiment summary (e.g. "Loved the bass, but the ear-tips are stiff"). Summaries are pre-computed by a **ShedLock-coordinated distributed background worker** for sub-millisecond retrieval.

### Deep Financial Intelligence
Admin and Seller dashboards powered by raw **JPQL Aggregate DTO Projections**. Calculates dynamic Customer Lifetime Value (CLV), Customer Churn Risk signals, and generates algorithmic "Product Quality Scores" based on return funnel metrics — providing actionable business intelligence natively.

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

- **OOM-Protected APIs:** Aggressive Spring Data `Pageable` enforcement across product catalogs and heavy order repositories.
- **Sub-Millisecond Caching:** `@Cacheable` directives tied to **Upstash Redis**, dramatically offloading product and category DB reads with native eviction triggers.
- **DDoS Mitigation:** Per-IP Token Bucket rate limiting via **Bucket4j** built into the Spring Security filter chain.
- **Cloud Content Delivery:** Direct binary integration with the **Cloudinary CDN** — zero local disk dependency.

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
    RL --> JWT[🔒 JWT / OAuth2 Auth]
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
| **AI Embeddings** | HuggingFace `all-MiniLM-L6-v2` | Semantic text embeddings |
| **Migrations** | Flyway | Deterministic, version-controlled schema evolution |
| **Cache & State** | Upstash Redis | JWT blacklists, OTPs, idempotency locks, read caching |
| **Payments** | Razorpay SDK | Orders, webhooks, instant refunds |
| **Observability** | Micrometer + Prometheus + Grafana | 200+ live metrics — JVM, DB pool, RabbitMQ, HTTP latency |

---

## 🚀 Quick Start Guide

### 1. Prerequisites
- **Java 21+**, **Maven 3.8+**
- **PostgreSQL** (with `pgvector` extension installed)
- **Redis** (Upstash free tier works)
- **HuggingFace** free account for semantic search
- **Google Cloud Console** (OAuth2 Web Client Credentials)

### 2. Install pgvector (Required for AI Search)
```sql
-- Run as PostgreSQL superuser (postgres) in pgAdmin:
CREATE EXTENSION IF NOT EXISTS vector;
```

### 3. Clone & Configure
```bash
git clone https://github.com/manish5200/CognitoCart.git
cd CognitoCart/smartcart

# Create the database
psql -U postgres -c "CREATE DATABASE cognitocart;"
```

Copy `application-demo.yml` → `application.yml` and explicitly define your credentials:
- PostgreSQL Connection Details
- Redis (Upstash) URL
- Gmail SMTP App Password
- HuggingFace API Token
- **Google Client ID & Secret** for OAuth2 Authorization

### 4. Run & Explore
```bash
./mvnw spring-boot:run
```
> Flyway will forcefully execute **20+ migrations** to map relations, inject pgvector structures, build the Category tree, and establish your default Admin account prior to Tomcat startup.

Navigate directly to **`http://localhost:8080/swagger-ui.html`** to test your endpoints. Or, test the new Google OAuth2 implementation natively through your browser by visiting: `http://localhost:8080/oauth2/authorization/google`

---

## 🛣️ Engineering Roadmap

<details>
<summary><b>✅ Completed Phases (1 → 4.3)</b></summary>

- **Phase 1 — Auth Hardening:** Redis JWT Blacklists · Pessimistic Stock Locks · Secure OTP verification
- **Phase 2 — Fulfillment:** Razorpay refunds · iText7 PDF invoices · Logistical state machines
- **Phase 3 — Scale:** Cloudinary CDN · Advanced JPQL analytics · Seller dashboards
- **Phase 3.5 — Operations:** DLQs for failed webhooks · Guest-to-User cart migrations
- **Phase 3.9 — Pre-AI:** Global `@SoftDelete` · Idempotency Locks · BOGO Engine · Wishlist HTML Schedulers
- **Phase 4.1 — Semantic Search ✅:** pgvector + HuggingFace AI embeddings · Cosine similarity search
- **Phase 4.3 — AI Review Summarization ✅:** HuggingFace BART Large CNN · Background `@Scheduled` workers · `ProductInsights` engine

</details>

**Phase 5 — Cloud & DevOps ☁️**
- [x] **Distributed Schedulers ✅:** `ShedLock` + PostgreSQL ACID locking across 4 background jobs in multi-instance deployments.
- [x] **Event-Driven Architecture ✅:** RabbitMQ (CloudAMQP) decouples PDF invoice generation & email dispatch — response time dropped from ~4s to <50ms. Dead Letter Queue (DLQ) ensures zero message loss.
- [x] **Observability ✅:** Micrometer + Prometheus + Grafana Cloud — 200+ live metrics scraped every 15s via Grafana Alloy with live dashboards.
- [x] **Enterprise Exception Hierarchy ✅:** `@RestControllerAdvice` standardization across logic boundaries (explicitly throwing ResourceNotFound, InsufficientStock algorithms).

**Phase 7 — Advanced Analytics & Infrastructure Hardening 📊**
- [x] **High-Performance Streaming ✅:** `StreamingResponseBody` + OpenCSV + `TransactionTemplate` for zero-memory background data exports.
- [x] **Async Security Propagation ✅:** `RequestAttributeSecurityContextRepository` integration for safe user identity inheritance in Tomcat async dispatches.
- [ ] **Logistics Webhooks:** Carrier status sync (In-Progress).

---

## 👨‍💻 Primary Architect

**Manish Kumar Singh**  
*Backend Systems Engineer · Java · Spring Boot · AI Integration · Distributed Systems*

I engineer resilient software capable of surviving heavy load and dynamic scaling requirements. CognitoCart demonstrates the full spectrum — from raw PostgreSQL schema design and distributed transaction management, to integrating live AI embeddings and asynchronous event-driven architectures.

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/maniish5200/)
[![GitHub](https://img.shields.io/badge/GitHub-Follow-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/manish5200)

<div align="center">
  <sub>⭐ Star this repo if you appreciate rigorous, production-grade software engineering!</sub>
</div>
