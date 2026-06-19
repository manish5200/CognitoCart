package com.manish.smartcart.repository;

import com.manish.smartcart.dto.event.SellerEmailProjection;
import com.manish.smartcart.enums.Role;
import com.manish.smartcart.model.user.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    /**
     * Paginated Projection Query — fetches ONLY email + fullName, 500 rows at a time.
     *
     * ─── HOW IT WORKS ────────────────────────────────────────────────────────────
     * Spring Data reads this method name and auto-generates SQL at startup:
     *     SELECT u.email, u.full_name FROM users u WHERE u.role = ? LIMIT 500 OFFSET 0
     *
     * The Orchestrator calls this in a loop:
     *     page 0 → processes 500 sellers → those 500 objects become GC-eligible
     *     page 1 → processes next 500 → again GC-eligible
     *     ...and so on until page.hasNext() returns false
     *
     * The JVM NEVER holds more than 500 lightweight projection objects at once.
     * This is how you handle millions of records without OOM.
     */
    Slice<SellerEmailProjection> findByRole(Role role, Pageable pageable);
}
