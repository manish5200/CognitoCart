package com.manish.smartcart.model;

import com.manish.smartcart.model.base.BaseEntity;
import com.manish.smartcart.model.user.Users;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * RefreshToken Entity: Provides a stateful way to manage long-lived sessions.
 * Extends BaseEntity to inherit unified ID, Versioning, and Audit fields.
 **/

@Entity
@Table(name = "refresh_tokens")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@SuperBuilder// Required to work with BaseEntity's SuperBuilder
public class RefreshToken extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

    /**
     * Real-World Concept: One-to-One vs Many-to-One
     * In many apps, a user can have multiple sessions (Laptop, Phone).
     * Using @OneToOne limits a user to ONE active session globally.
     * If you want multiple devices, change this to @ManyToOne.
     */

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Users user;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false; // Used for "Blacklisting" tokens manually

}
