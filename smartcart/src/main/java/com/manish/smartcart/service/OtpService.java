package com.manish.smartcart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    // Redis key: "email-otp:<email>" → "123456" (10 min TTL)
    private static final String OTP_PREFIX = "email-otp:";
    private static final long OTP_TTL_MINUTES = 10;
    // Rate limit key: "otp-limit:<email>" → "1" (2 min cooldown)
    private static final String RATE_PREFIX = "otp-limit:";
    private static final long RATE_TTL_MINUTES = 2;
    private final StringRedisTemplate stringRedisTemplate;

    // SecureRandom is cryptographically strong (unlike java.util.Random)
    private final SecureRandom random = new SecureRandom();

    // ─────────────────────────────────────────────────────────────────────────
    // Generate a fresh 6-digit OTP and store it in Redis.
    // Returns the OTP string so the caller can embed it in the email.
    // ─────────────────────────────────────────────────────────────────────────
    public String generateAndStore(String email) {
        // Produces a number 100000–999999 (always 6 digits)
        String otp = String.valueOf(100000 + random.nextInt(900000));

        // Store with 10-min auto-expiry — no cleanup job needed
        stringRedisTemplate.opsForValue()
                .set(OTP_PREFIX + email, otp, Duration.ofMinutes(OTP_TTL_MINUTES));

        log.info("OTP generated for: {}", email);
        return otp;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Checks if OTP is valid (matches AND has not expired).
    // Redis TTL handles expiry automatically — if key is gone, OTP expired.
    // ─────────────────────────────────────────────────────────────────────────
    public boolean verifyOTP(String email, String submittedOTP) {
        String storedOTP = stringRedisTemplate.opsForValue().get(OTP_PREFIX + email);

        // storedOtp == null means it either expired or was never sent
        if (storedOTP == null) {
            return false;
        }
        boolean matched = storedOTP.equals(submittedOTP);

        if (matched) {
            // ONE-TIME USE: delete immediately so the same OTP can't be reused
            stringRedisTemplate.delete(OTP_PREFIX + email);
            log.info("OTP verified successfully for: {}", email);
        }
        return matched;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rate limiter for resend OTP — prevents inbox spam.
    // Returns true if the user must wait (is rate-limited).
    // ─────────────────────────────────────────────────────────────────────────
    public boolean isRateLimited(String email) {
        return stringRedisTemplate.hasKey(RATE_PREFIX + email);
    }

    // Marks this email as "rate limited" for 2 minutes
    public void markRateLimited(String email) {
        stringRedisTemplate.opsForValue()
                .set(RATE_PREFIX + email, "1", Duration.ofMinutes(RATE_TTL_MINUTES));
    }
}
