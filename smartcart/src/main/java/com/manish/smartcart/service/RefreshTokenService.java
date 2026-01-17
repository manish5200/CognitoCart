package com.manish.smartcart.service;

import com.manish.smartcart.dto.auth.TokenRefreshResponse;
import com.manish.smartcart.model.RefreshToken;
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.RefreshTokenRepository;
import com.manish.smartcart.repository.UsersRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UsersRepository usersRepository;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    public RefreshToken createRefreshToken(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Step 1: Clean up any old tokens (One session per user logic)
        refreshTokenRepository.deleteByUser(user);

        // Step 2: Build the new token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpiration))
                .build();

        return refreshTokenRepository.save(refreshToken);

    }

    public RefreshToken verifyExpiration(RefreshToken refreshToken) {
        if(refreshToken.getExpiryDate().compareTo(Instant.now()) < 0){
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired. Please sign in again.");
        }
        return refreshToken;
    }

    public Optional<RefreshToken> findByToken(@NotBlank(message = "Refresh token is missing") String refreshToken) {
         return refreshTokenRepository.findByToken(refreshToken);
    }
}
