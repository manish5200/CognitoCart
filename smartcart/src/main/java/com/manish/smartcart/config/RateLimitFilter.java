package com.manish.smartcart.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

/**
 * IP-based rate limiting using the Token Bucket algorithm (Bucket4j + Redis).
 *
 * Rules:
 * - POST /api/v1/auth/login → 5 attempts per 15 minutes per IP
 * - POST /api/v1/auth/register → 3 attempts per hour per IP
 *
 * Uses ProxyManager backed by Redis for multi-instance readiness.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<byte[]> proxyManager;

    public RateLimitFilter(ProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
    }

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String REGISTER_PATH = "/api/v1/auth/register";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only rate-limit POST requests on auth endpoints
        if (!"POST".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);

        if (path.equals(LOGIN_PATH)) {
            String redisKey = "rate_limit:login:" + clientIp;
            Bucket bucket = proxyManager.builder().build(redisKey.getBytes(), loginBucketConfig());
            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                log.warn("🚫 RATE LIMIT hit: LOGIN — IP={} exceeded 5 attempts/15min", clientIp);
                rejectRequest(response, "Too many login attempts. Please try again after 15 minutes.");
            }
            return;
        }

        if (path.equals(REGISTER_PATH)) {
            String redisKey = "rate_limit:register:" + clientIp;
            Bucket bucket = proxyManager.builder().build(redisKey.getBytes(), registerBucketConfig());
            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                log.warn("🚫 RATE LIMIT hit: REGISTER — IP={} exceeded 3 attempts/hour", clientIp);
                rejectRequest(response, "Too many registration attempts. Please try again after 1 hour.");
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Login: 5 tokens, refills 5 tokens every 15 minutes (greedy refill).
     */
    private Supplier<BucketConfiguration> loginBucketConfig() {
        return () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillGreedy(5, Duration.ofMinutes(15))
                        .build())
                .build();
    }

    /**
     * Register: 3 tokens, refills 3 tokens every 1 hour (greedy refill).
     */
    private Supplier<BucketConfiguration> registerBucketConfig() {
        return () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(3)
                        .refillGreedy(3, Duration.ofHours(1))
                        .build())
                .build();
    }

    /**
     * Extracts the real client IP, respecting reverse proxy headers.
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim(); // First IP in chain = real client
        }
        return request.getRemoteAddr();
    }

    /**
     * Writes a standardised 429 JSON response identical to our
     * GlobalExceptionHandler format.
     */
    private void rejectRequest(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = Map.of(
                "status", 429,
                "errorCode", "RATE_LIMIT_EXCEEDED",
                "message", message,
                "timestamp", java.time.LocalDateTime.now().toString());
        new ObjectMapper().writeValue(response.getWriter(), body);
    }
}
