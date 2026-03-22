package com.manish.smartcart.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE+5)// Runs very early, just after basic security
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String REDIS_PREFIX = "idempotency:";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);

        // 1. We only apply this to POST or PUT requests that explicitly send the Header.
        // Standard GET requests for products bypass this instantly with zero performance hit.
        if(idempotencyKey == null ||(!request.getMethod().equalsIgnoreCase("POST") &&
                !request.getMethod().equalsIgnoreCase("PUT"))){
            filterChain.doFilter(request, response);
            return;
        }

        String redisKey = REDIS_PREFIX + idempotencyKey;

        // 2. Has this Idempotency-Key been processed recently?
        String cachedResponse = redisTemplate.opsForValue().get(redisKey);

        if(cachedResponse != null){
            if("IN_PROGRESS".equals(cachedResponse)){
                // The user double-clicked the button rapidly!
                // The first request is currently hitting the Razorpay API. Reject this 2nd request.
                log.warn("🚨 Idempotency: Blocked concurrent duplicate request! Key: {}", idempotencyKey);
                response.setStatus(409);//409 conflict
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Request currently processing. Please wait.\", \"status\": 409}");
                return;
            }
            // We ALREADY processed this successfully beforehand! Return the cached JSON directly.
            // This entirely skips the Controller and Database!
            log.info("✅ Idempotency Cache Hit! Returning saved JSON response for Key: {}", idempotencyKey);
            response.setStatus(200);
            response.setContentType("application/json");
            response.getWriter().write(cachedResponse);
            return;
        }

        // 3. This is a brand-new, unique request!
        // Lock it in Redis with a short 60-second TTL using "setIfAbsent" to guarantee thread safety.
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(redisKey, "IN_PROGRESS",60, TimeUnit.SECONDS);

        if(Boolean.FALSE.equals(locked)){
            response.setStatus(409);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Request currently processing. Please wait.\", \"status\": 409}");
            return;
        }

        // 4. Wrap the response dynamically so we can intercept the final JSON body
        // AFTER the Controller finishes saving the Order!
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try{
            // Let the request pass down the chain into the actual Controller...
            filterChain.doFilter(request, responseWrapper);
            // 5. The Controller has finished! Extract the generated JSON response
            byte[] responseArray = responseWrapper.getContentAsByteArray();
            String responseBody = new String(responseArray, StandardCharsets.UTF_8);

            // 6. If the Controller successfully created the order (HTTP 200/201),
            // cache the JSON output in Redis for 24 HOURS!

            if(responseWrapper.getStatus() >= 200 && responseWrapper.getStatus() <= 299){
                redisTemplate.opsForValue().set(redisKey, responseBody, 24, TimeUnit.HOURS);
                log.info("🔒 Idempotency: Successfully cached completed response for Key: {}", idempotencyKey);
            }else {
                // If the user made a mistake (e.g., 400 Bad Request / Invalid Credit Card),
                // DELETE the lock so they can fix their typo and try again immediately!
                redisTemplate.delete(redisKey);
            }
        } catch (Exception e) {
            // If the Java code crashes completely, unlock Redis so it doesn't get stuck permanently.
            redisTemplate.delete(redisKey);
            throw e;
        }finally {
            // CRITICAL: We intercepted the JSON body, so the real HttpServletResponse is currently EMPTY.
            // We MUST copy the array back into the real response, or the Frontend gets a blank white screen!
            responseWrapper.copyBodyToResponse();

        }
    }
}
