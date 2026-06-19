package com.manish.smartcart.dto.event;

/**
 * ─── WHAT IS THIS? ───────────────────────────────────────────────────────────
 * This is a Spring Data JPA "Projection" — an interface, NOT a class.
 *
 * ─── LAYMAN EXPLANATION ──────────────────────────────────────────────────────
 * Normally when you call findByRole(SELLER), JPA runs:
 *     SELECT * FROM users WHERE role = 'SELLER'
 * That loads the FULL Users object — password, address, profile, everything.
 * With 50,000 sellers, that's loading ~50,000 full objects into RAM. OOM crash.
 *
 * A Projection tells JPA to run instead:
 *     SELECT u.email, u.full_name FROM users u WHERE u.role = 'SELLER'
 * Only 2 columns. Zero object graph. Zero lazy loading. Safe at any scale.
 *
 * Spring auto-generates the implementation of this interface at runtime.
 * You never write a class body — just declarAe what columns you want.
 */

//By Gemini
/**
 * Lightweight JPA projection used to optimize memory consumption during mass data retrieval.
 * Restricts the Hibernate SQL query to fetch only the essential scalar values required
 * for asynchronous email fan-out operations, preventing OutOfMemory (OOM) degradation
 * associated with loading full entity graphs.
 */

public interface SellerEmailProjection {

    /**
     * @return The seller's registered contact email address.
     */
    String getEmail();

    /**
     * @return The seller's fully formatted name for use in marketing templates.
     */
    String getFullName();
}
