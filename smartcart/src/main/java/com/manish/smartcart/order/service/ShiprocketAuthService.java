package com.manish.smartcart.order.service;

import com.manish.smartcart.shared.exception.BusinessLogicException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages Shiprocket authentication.
 * <p>
 * CONCEPT — Cache-Aside Pattern:
 * Shiprocket uses a JWT token valid for 24 hours.
 * We store it in Redis for 23h so we never call the login API on every shipment.
 * If the cached token is stale (edge case), forceRefreshToken() clears and re-fetches.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShiprocketAuthService {

    @Qualifier("shiprocketRestClient")
    private final RestClient restClient;

    private final StringRedisTemplate redisTemplate;

    @Value("${shiprocket.email}")
    private String email;

    @Value("${shiprocket.password}")
    private String password;

    // Redis key for the cached token
    private static final String TOKEN_CACHE_KEY = "shiprocket:auth:token";

    /**
     * Returns a valid Shiprocket JWT token.
     * Cache hit → no network call. Cache miss → login and cache for 23h.
     */
    public String getToken() {
        String cached = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        log.info("Shiprocket token not in cache. Fetching fresh token...");
        return fetchAndCacheToken();
    }

    /**
     * Forcefully evicts the stale token from Redis and fetches a fresh one.
     * Called when Shiprocket returns 401 (token expired mid-request).
     * Redis SET is atomic — safe under concurrent requests.
     */
    public String forceRefreshToken() {
        log.warn("Force-refreshing Shiprocket token due to 401 response.");
        redisTemplate.delete(TOKEN_CACHE_KEY);
        return fetchAndCacheToken();
    }

    /**
     * Calls POST /auth/login, extracts the token, and caches it in Redis for 23h.
     */
    private String fetchAndCacheToken() {
        try {
            // Login payload
            Map<String, String> loginBody = new HashMap<>();
            loginBody.put("email", email);
            loginBody.put("password", password);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/auth/login")
                    .body(loginBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("token")) {
                throw new BusinessLogicException("Shiprocket login failed: no token in response. Check credentials.");
            }

            String token = (String) response.get("token");

            // Cache for 23h (token valid 24h — 1h buffer prevents edge-case expiry)
            redisTemplate.opsForValue().set(TOKEN_CACHE_KEY, token, Duration.ofHours(23));
            log.info("Shiprocket token cached successfully for 23 hours.");

            return token;

        } catch (BusinessLogicException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to authenticate with Shiprocket API", e);
            throw new BusinessLogicException(
                    "Shiprocket authentication failed. Check email/password in application-dev.yml.");
        }
    }
}
