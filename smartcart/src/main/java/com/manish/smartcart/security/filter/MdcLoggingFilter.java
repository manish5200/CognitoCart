package com.manish.smartcart.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC (Mapped Diagnostic Context) Request Tracing Filter.
 * <p>
 * ─── THE PRODUCTION PROBLEM ────────────────────────────────────────────────
 * When 100 concurrent requests hit the server, every thread logs to the same
 * file. Without a unique identifier per request, debugging is impossible:
 * <p>
 *   INFO  OrderService : Order placed for user 45   ← which HTTP request?
 *   INFO  OrderService : Order placed for user 12   ← impossible to correlate
 * <p>
 * ─── THE SOLUTION ──────────────────────────────────────────────────────────
 * MDC is a per-THREAD key-value store built into SLF4J. Since each HTTP
 * request runs on its own Tomcat thread, any key put into MDC appears
 * automatically on EVERY log line in that thread — no param passing needed.
 * <p>
 * After this filter, logs look like:
 *   INFO [traceId=3f7a1b2c] [userId=45] OrderService : Order placed
 *   INFO [traceId=3f7a1b2c] [userId=45] PaymentService: Payment verified
 *   INFO [traceId=9d2e4a1f] [userId=12] OrderService : Order placed
 * <p>
 * ─── X-TRACE-ID HEADER ─────────────────────────────────────────────────────
 * If an upstream API Gateway (AWS ALB, Nginx, GCP LB) sends an X-Trace-Id
 * header, we reuse it. This enables end-to-end distributed tracing across
 * multiple services sharing the same traceId in Grafana/Loki/Kibana.
 * <p>
 * ─── THREAD SAFETY — THE #1 MDC IMPLEMENTATION BUG ────────────────────────
 * Tomcat REUSES threads from a pool. Without MDC.clear() in finally{},
 * the next request on this thread INHERITS the previous request's MDC data,
 * producing phantom traceIds and incorrect userIds in logs.
 * The finally block is therefore MANDATORY and non-negotiable.
 * <p>
 * ─── FILTER ORDER ──────────────────────────────────────────────────────────
 * HIGHEST_PRECEDENCE + 1 → runs before JwtFilter and RateLimitFilter.
 * This ensures traceId is set even when authentication fails, so we can
 * trace auth failures back to their originating request.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MdcLoggingFilter extends OncePerRequestFilter {

    // Standard header used by AWS ALB, GCP Load Balancer, and most API Gateways
    // to propagate a request trace across distributed service hops.
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    // MDC key constants — used in logback-spring.xml pattern as %X{traceId}.
    // Using constants prevents silent typo bugs between filter and logback config.
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_USER_ID  = "userId";
    private static final String MDC_METHOD   = "method";
    private static final String MDC_URI      = "uri";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try{
            // 1. Resolve traceId — prefer upstream, fall back to local UUID.
            //    16-hex chars has 2^64 uniqueness — more than sufficient per node.
            String incoming = request.getHeader(TRACE_ID_HEADER);
            String traceId = (incoming != null && !incoming.isBlank())
                    ? incoming
                    : UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            // 2. Populate MDC — visible on every log line for this thread.
            MDC.put(MDC_TRACE_ID, traceId);
            MDC.put(MDC_METHOD, request.getMethod());
            MDC.put(MDC_URI, request.getRequestURI());

            // 3. Inject userId if authenticated.
            //    JwtFilter runs AFTER this filter, so on first pass the Security
            //    Context may be empty. Defensive null-check is required.
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if(auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                MDC.put(MDC_USER_ID, auth.getName());
            }

            // 4. Echo traceId to client — frontend can log it alongside errors
            //    and reference it in support tickets: "My X-Trace-Id is 3f7a1b2c"
            response.setHeader(TRACE_ID_HEADER, traceId);

            // 5. Proceed — all downstream log statements now carry traceId
            filterChain.doFilter(request, response);

        }finally {
            MDC.clear();
            // 6. MANDATORY CLEANUP — prevents MDC data leaking to the next
            //    request reusing this Tomcat thread from the thread pool.
        }
    }
}
