package com.manish.smartcart.user.model;

import com.manish.smartcart.shared.enums.AuthProvider;
import com.manish.smartcart.shared.enums.Gender;
import com.manish.smartcart.shared.enums.Role;
import com.manish.smartcart.shared.model.BaseEntity;
import com.manish.smartcart.shared.util.HumanIdGenerator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Core identity and authentication model for all users on the platform.
 * <p>
 * ARCHITECTURE RULE: Delegation Pattern
 * This class strictly holds authentication, logistics, and universal identity data.
 * Role-specific business data is delegated to CustomerProfile or SellerProfile via One-to-One mapping.
 */
@Entity
@Table(name = "users")
@SequenceGenerator(name = "entity_seq", sequenceName = "user_seq", allocationSize = 50)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@SuperBuilder
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted = false")
public class Users extends BaseEntity {

    /**
     * A support-friendly account number readable over the phone (e.g. USR-20260703-K7P2MQ).
     * Generated once on first persist via PrePersist. Never updated. Never exposed in JWT.
     */
    @Column(name = "account_number", unique = true, updatable = false, length = 30)
    private String accountNumber;

    @Column(nullable = false, unique = true, length = 180)
    @Email
    @NotBlank
    private String email;

    @Size(min = 4)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    // --- PHASE 1: HOISTED IDENTITY FIELDS ---
    @NotBlank
    private String fullName;

    @Column(length = 20, unique = true)
    private String phone;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Required for SQLDelete to work. Ensures the column exists in the schema.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    // --- PHASE 1: LOGISTICS SHORTCUT ---
    /**
     * Quick reference to the user's default shipping/billing address.
     * EAGER fetched as it is almost always required during checkout and profile loads.
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "primary_address_id")
    private Address primaryAddress;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private CustomerProfile customerProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private SellerProfile sellerProfile;

    /**
     * Timestamp of last password change.
     * SECURITY RULE: Any JWT issued before this moment is considered stale and rejected.
     * Updated on password reset AND on manual password change.
     */
    private LocalDateTime passwordChangedAt;

    /**
     * Whether this user has confirmed ownership of their email address.
     * Set to FALSE on registration -> TRUE after OTP verification.
     * Checkout is blocked while this is FALSE.
     */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    public Users(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.authProvider = AuthProvider.LOCAL;
    }

    // ─── LIFECYCLE HOOKS ─────────────────────────────────────────────────────

    /**
     * Generates the human-readable account number before the very first DB insert.
     * Guard clause prevents accidental overwrite if entity is somehow persisted twice.
     */
    @PrePersist
    private void generateHumanId() {
        if (this.accountNumber == null) {
            this.accountNumber = HumanIdGenerator.generate("USR");
        }
    }
}