<div align="center">
  <h1>🛒 CognitoCart: Enterprise E-Commerce Backend API</h1>
  <i>A scalable, cloud-native E-Commerce engine built with Spring Boot 3, PostgreSQL, RabbitMQ, and Semantic AI.</i>
  <br/><br/>
  
  ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)
  ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)
  ![RabbitMQ](https://img.shields.io/badge/RabbitMQ-Event_Driven-FF6600?style=flat-square&logo=rabbitmq&logoColor=white)
  ![Redis](https://img.shields.io/badge/Redis-Caching-DC382D?style=flat-square&logo=redis&logoColor=white)
  ![OAuth2](https://img.shields.io/badge/OAuth_2.0-Google_Identity-4285F4?style=flat-square&logo=google&logoColor=white)
  ![Prometheus](https://img.shields.io/badge/Prometheus-Monitoring-E6522C?style=flat-square&logo=prometheus&logoColor=white)
</div>

---

## 🌟 Overview
**CognitoCart** is a heavy-duty, production-ready backend API designed to mimic the core infrastructure of world-class platforms like Amazon or Flipkart. Built entirely in Java (Spring Boot), it goes beyond simple CRUD operations by integrating complex business logic, async message queues, memory-safe pagination, distributed caching, and AI-driven semantic text search.

### 🚀 Core Capabilities
* **Dual-Delegated Authentication:** Supports both JWT-based local authentication and Google OAuth 2.0 social logins securely.
* **Role-Based Access Control (RBAC):** Strict isolation between `CUSTOMER`, `SELLER`, and `ADMIN` privileges using Spring Security context mapping.
* **Algorithmic Math Engine:** A robust, automated checkout pipeline that calculates sub-totals, evaluates coupon eligibility, deducts flat discounts/BOGOs, and dynamically applies shipping fees safely.
* **Database Concurrency:** Employs **Pessimistic Write Locks (`SELECT FOR UPDATE`)** on product inventory to completely eliminate double-selling race conditions during high-traffic flash sales.
* **Asynchronous Notifications:** Uses CloudAMQP (RabbitMQ) to offload heavy tasks like rendering Thymeleaf HTML emails, ensuring the main HTTP threads respond instantly.
* **High-Performance Caching:** Integrates Upstash Redis to aggressively cache high-frequency read operations (like Category trees and Product Details).
* **Enterprise Observability:** Fully instrumented with Micrometer metrics and Prometheus, ready to be scraped into Grafana dashboards.
* **AI Semantic Search:** Wraps HuggingFace's `all-MiniLM-L6-v2` embedding model with PostgreSQL `pgvector`, allowing users to search by "meaning" rather than just exact keyword matches.
* **Asset Management:** Direct integration with Cloudinary CDN for product image storage.
* **Flyway Migrations:** All database schema changes are strictly version-controlled to prevent data corruption.

---

## 🏗️ Architecture Stack
| Layer | Technologies Used |
|---|---|
| **Core Framework** | Java 17, Spring Boot 3.2, Spring Web, Spring Security |
| **Database & ORM** | PostgreSQL 16 (pgvector), Spring Data JPA, Hibernate, Flyway |
| **Authentication** | JSON Web Tokens (JWT), Spring OAuth2 Client (Google) |
| **Message Broker** | RabbitMQ (CloudAMQP) |
| **Caching Layer** | Redis (Upstash Serverless) |
| **Media Delivery** | Cloudinary CDN |
| **Payment Gateway** | Razorpay SDK |
| **Observability** | Micrometer, Prometheus Actuator |
| **API Documentation** | OpenAPI 3.0 / Swagger UI |

---

## 💻 Running the Application

### 1. Prerequisites
Ensure you have the following installed locally:
* Java 17+
* Maven
* PostgreSQL (or Docker)

### 2. Environment Variables
You must supply the following credentials to your environment to spin up the application gracefully. 
*(If running locally, these can be templated directly into `application-dev.yml`)*
```env
GOOGLE_CLIENT_ID=your_google_id
GOOGLE_CLIENT_SECRET=your_google_secret
CLOUDINARY_URL=cloudinary://api_key:api_secret@cloud_name
RABBITMQ_URL=amqps://user:pass@host.rmq.cloudamqp.com/vhost
REDIS_URL=rediss://default:password@host.upstash.io:6379
RAZORPAY_KEY_ID=your_razorpay_key
RAZORPAY_KEY_SECRET=your_razorpay_secret
POSTGRES_USER=cognitocart
POSTGRES_PASSWORD=your_db_password
HUGGINGFACE_API_KEY=hf_your_token
```

### 3. Start the Server
Build and run the Spring Boot application. Flyway will automatically execute all migration scripts inside `/db/migration` to map your tables.
```bash
mvn clean install
mvn spring-boot:run
```

### 4. Interactive Documentation
Once the server starts on `http://localhost:8080`, interact directly with the APIs via the automatically generated Swagger UI:
* **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`

---

## 🧪 Advanced Testing Insights
* **Memory-Safe Pagination:** All collection-based endpoints (`/products`, `/orders/history`) are heavily optimized. Append `?page=0&size=20` to the URL. The Spring Data APIs natively use `countQuery` logic to prevent dangerous in-memory `JOIN FETCH` operations that lead to Out-Of-Memory (OOM) crashes.
* **Exception Standardization:** Business rules violating database constraints trigger explicit sub-classes (e.g., `ResourceNotFoundException`, `InsufficientStockException`) rather than generic Runtime Exceptions. The custom `@RestControllerAdvice` maps these securely into appropriate `404`, `409`, or `400` JSON responses.
* **OAuth 2.0 Identity Flow Testing:** To test the Google Sign-in flow natively without a frontend, open any browser window (Incognito recommended) and fire this exact URL:
  ```http
  http://localhost:8080/oauth2/authorization/google
  ```
  After granting consent, the backend will dynamically register the account and directly print your signed `accessToken` and `refreshToken` securely to the browser window for you to copy into Postman or Swagger!

---

<div align="center">
  <i>"Built for Scalability. Engineered for Reliability."</i>
</div>
