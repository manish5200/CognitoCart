package com.manish.smartcart.service;

import com.manish.smartcart.repository.UsersRepository;
import com.manish.smartcart.service.email.EmailTemplateBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    // Key format: "pwd-reset:<UUID-token>" → value: "user@email.com"
    private static final String PREFIX = "pwd-reset:";

    // Token is valid for 15 minutes — after that Redis auto-deletes it
    private static final long TTL_MINUTES = 15;

    // ─── RATE LIMIT GUARD ────────────────────────────────────────────────────
    // Prevents email bombing (spam requesting resets for someone else's inbox)
    // One reset email per email address per 5 minutes — stored in Redis
    private static final String RATE_LIMIT_PREFIX = "pwd-reset-limit:";
    private static final long RATE_LIMIT_MINUTES = 2;

    private final StringRedisTemplate stringRedisTemplate;
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailTemplateBuilder emailTemplateBuilder;

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 1: User clicks "Forgot Password" → generate token → store in Redis →
    // send email
    // ─────────────────────────────────────────────────────────────────────────
    public void initiatePasswordReset(String email) {
        // Check if we already sent a reset email for this address recently
        Boolean alreadySent = stringRedisTemplate.hasKey(RATE_LIMIT_PREFIX + email);
        if(alreadySent) {
            // Don't reveal if email exists — just say "check your inbox or try later"
            // This prevents both enumeration AND email bombing
            log.info("Rate limited reset attempt for: {}", email);
            return; // Silently ignore — same 200 response to caller
        }

        // Always return the same response — don't leak if the email is registered(prevents user enumeration attacks)
        usersRepository.findByEmail(email).ifPresent(user -> {
            // UUID is 128-bit random — cryptographically impossible to guess
            String token = UUID.randomUUID().toString();

            // Store: "pwd-reset:<token>" = "user@email.com" with 15 min auto-expiry
            // Key uses token (not email) so attackers can't trigger resets by email guess
            stringRedisTemplate.opsForValue()
                    .set(PREFIX + token, email, Duration.ofMinutes(TTL_MINUTES));

            //Set the rate limit key (2-min cooldown per email)
            stringRedisTemplate.opsForValue()
                            .set(RATE_LIMIT_PREFIX + email, "1", Duration.ofMinutes(RATE_LIMIT_MINUTES));

            log.info("Password reset initiated for: {}", email);

            // Send the email with the reset link
            // The full URL would be: https://cognitocart.com/reset-password?token=<token>
            // For now we send the token directly — frontend appends to its URL

            try {
                String body = emailTemplateBuilder.buildPasswordResetEmail(user, token);
                emailService.sendMail(user.getEmail(),
                        "Reset Your CognitoCart Password 🔑",
                        body, "CognitoCart Security Team");
            } catch (Exception e) {
                log.warn("Failed to send reset email to {}", email, e);
            }

        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2: User submits new password with the token from the email link
    // ─────────────────────────────────────────────────────────────────────────
    public void resetPassword(String token, String newPassword) {

        // Look up the email stored against this token
        String email = stringRedisTemplate.opsForValue().get(PREFIX + token);

        // Token not found = expired or invalid (Redis TTL already removed it)
        if (email == null) {
            throw new RuntimeException("Reset link has expired or is invalid. Please request a new one.");
        }

        // Find the user and update their password
        usersRepository.findByEmail(email).ifPresent(user -> {
            // bcrypt the new password before saving — never store plain text
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordChangedAt(LocalDateTime.now());
            usersRepository.save(user);
            log.info("Password updated successfully for: {}", email);

            // SECURITY NOTIFICATION: Tell the user their password just changed.
            // Real-world pattern (Google, GitHub, Amazon all do this):
            // → If it WAS them → reassurance + "log in with new password"
            // → If it WASN'T → red alert + "contact support" CTA
            // This gives users a chance to react immediately if their account was hijacked.
            try {
                String body = emailTemplateBuilder.buildPasswordChangedEmail(user);
                emailService.sendMail(
                        user.getEmail(),
                        "Your CognitoCart Password Was Changed 🔒",
                        body,
                        "CognitoCart Security Team");
            } catch (Exception e) {
                log.warn("Failed to send password-changed notification to {}", email, e);
            }
        });

        // ONE-TIME USE: delete the token immediately after use
        // Even if the same link is clicked again → "invalid token" response
        stringRedisTemplate.delete(PREFIX + token);
    }
}
