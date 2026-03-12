package com.manish.smartcart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Manages the JWT deny-list in Redis.
 * Key format:  "jwt:blacklist:<jti>"
 * TTL: Set to the token's remaining lifetime → auto-expires, zero maintenance.
 * Why StringRedisTemplate?
 * We only store a simple "1" as the value — no object serialization needed.
 * StringRedisTemplate is lighter than the generic RedisTemplate.
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "jwt:blacklist:";

    // Spring auto-wires this from your existing Redis/Upstash config
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Adds a token's jti to the deny-list with a TTL matching the token's
     * remaining lifetime. After TTL expires, Redis auto-deletes the key.
     */
    public void blacklist(String jti, long remainingTtlSeconds) {
        if(remainingTtlSeconds <= 0) {
            log.debug("Token already expired — skipping blacklist for jti: {}", jti);
            return;
        }
        stringRedisTemplate.opsForValue()
                .set(PREFIX + jti, "1", Duration.ofSeconds(remainingTtlSeconds));
        log.info("🔒 Token blacklisted | jti: {} | TTL: {}s", jti, remainingTtlSeconds);
    }

    /**
     * Returns true if the token's jti is in the deny-list.
     * Called by JwtFilter on every authenticated request.
     **/
    public boolean isBlacklisted(String jti) {
        return stringRedisTemplate.hasKey(PREFIX + jti);
    }

}
