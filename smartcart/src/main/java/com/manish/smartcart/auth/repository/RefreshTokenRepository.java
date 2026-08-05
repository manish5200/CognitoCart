package com.manish.smartcart.auth.repository;

import com.manish.smartcart.auth.model.RefreshToken;
import com.manish.smartcart.user.model.Users;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Transactional
    void deleteByUser(Users user);

    // Security: Find by user to check for session limits
    Optional<RefreshToken> findByUser(Users user);

    // PUBLIC ID LOOKUP
    Optional<RefreshToken> findByPublicId(UUID publicId);
}
