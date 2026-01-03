# 🛒 CognitoCart API: Enterprise E-Commerce Backend

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]() [![Stack](https://img.shields.io/badge/stack-Spring%20Boot%20%7C%20Java%2017-blue)]() [![Database](https://img.shields.io/badge/database-PostgreSQL-blue)]() [![License](https://img.shields.io/badge/license-MIT-green)]()

> **An enterprise-grade e-commerce backend built with Java & Spring Boot, focusing on performance optimization, transactional integrity, and secure design patterns.**

---

## 📖 Project Overview

**CognitoCart** is a scalable, real-world e-commerce platform simulation. It handles the complete shopping lifecycle—from product discovery and social proof to secure checkout and inventory management—while adhering to industry best practices.

The core philosophy is **"Performance by Design."** We don't just query the database; we optimize how data flows to ensure the system scales with millions of users.

---

## 🏗️ Key Architectural Concepts

### 1. 🚀 O(1) Performance for Social Proof (Review Math)
Standard `SELECT AVG(rating)` queries become a bottleneck as datasets grow.
**The CognitoCart Solution:** We implement an **Incremental Moving Average** algorithm. By denormalizing the `Product` entity to store aggregate stats, we update ratings in **constant time O(1)** using math, bypassing expensive database scans.



### 2. 🛡️ Hardened Security (JWT & RBAC)
* **JWT (JSON Web Token):** Stateless authentication using a secure, **256-bit BASE64URL encoded secret key**.
* **RBAC:** Role-Based Access Control distinguishing `ADMIN` and `CUSTOMER` privileges.
* **Security Whitelisting:** Strategic permit-all access for Swagger documentation and public discovery endpoints.

### 3. 🔄 Transactional Integrity & Inventory Management
We use Spring's `@Transactional` to ensure **Atomicity** in business-critical flows:
* **Wishlist to Cart:** Items move between tables as a single atomic unit.
* **Place Order:** Real-time stock deduction during checkout. If inventory is insufficient, the transaction rolls back, preventing data corruption.



### 4. 🛠️ Enterprise Design Patterns
* **Global Exception Handling:** Centralized `@RestControllerAdvice` for standardized JSON error responses.
* **AppConstants:** Single source of truth for all "magic strings" and default values.
* **DTO Mapping:** Clean separation between Database Entities and API Response objects.

---

## 💻 Technology Stack

* **Core:** Java 17, Spring Boot 3.x
* **Database:** PostgreSQL (Production), H2 (Testing)
* **Security:** Spring Security, JJWT (Java JWT)
* **Documentation:** SpringDoc OpenAPI 3 (Swagger UI)
* **Persistence:** Spring Data JPA / Hibernate

---

## 🔌 API Documentation (Swagger)

The API is fully documented and interactive. Once the application is running, access the UI at:
👉 **`http://localhost:8080/swagger-ui/index.html`**



---

## 🚀 Getting Started

### Prerequisites
* Java JDK 17+
* Maven 3.8+
* PostgreSQL

### Installation

1. **Clone & Configure**
   ```bash
   git clone [https://github.com/yourusername/cognitocart.git](https://github.com/yourusername/cognitocart.git)

```

2. **Update application.properties**
Set your PostgreSQL credentials and a secure 256-bit `jwt.secret`.
3. **Build and Run**
```bash
mvn clean install
mvn spring-boot:run

```



---

## 👨‍💻 Author

**Manish Kumar Singh**

* [LinkedIn](https://www.google.com/search?q=YOUR_LINKEDIN_URL)
* [Portfolio](https://www.google.com/search?q=YOUR_PORTFOLIO_URL)

Built with ☕ and code during the 100 Days of Code challenge.

```
