package com.manish.smartcart.user.repository;

import com.manish.smartcart.infrastructure.messaging.SellerEmailProjection;
import com.manish.smartcart.shared.enums.Role;
import com.manish.smartcart.user.model.Users;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User Domain Operations.
 *
 * Required Indexes:
 * - UNIQUE INDEX on (email)
 * - UNIQUE INDEX on (phone)
 * - UNIQUE INDEX on (public_id)
 * - UNIQUE INDEX on (account_number)
 * - COMPOSITE INDEX on (role, id) -> Optimizes paginated lookups
 */
public interface UsersRepository extends JpaRepository<Users, Long> {

    // Primary authentication lookup point. Backed by unique index.
    Optional<Users> findByEmail(String email);

    // Guard query for onboarding flows. Executes an index-only scan (no entity hydration).
    boolean existsByEmail(String email);

    // Guard query for duplicate registration tracking.
    boolean existsByPhone(String phone);

    /**
     * Memory-safe processing channel for bulk role sweeps (e.g., email campaigns).
     *-
     * NOTE: Employs 'Slice' instead of 'Page' to entirely bypass the expensive COUNT(*)
     * queries that degrade performance on high-volume tables. Fetches data as closed
     * projections to reduce JVM heap allocations.
     */
    Slice<SellerEmailProjection> findByRole(Role role, Pageable pageable);

    /**
     * Secures public REST APIs against IDOR/enumeration attacks.
     * Exposed externally instead of internal database auto-incrementing IDs.
     */
    Optional<Users> findByPublicId(UUID publicId);

    /**
     * Operations and support tracker lookup.
     * Expected format: USR-YYYYMMDD-[RANDOM_STRING] (e.g., USR-20260710-K7P2MQ).
     * Suffix matching or column values must be case-insensitive to ensure index hits.
     */
    Optional<Users> findByAccountNumber(String accountNumber);
}