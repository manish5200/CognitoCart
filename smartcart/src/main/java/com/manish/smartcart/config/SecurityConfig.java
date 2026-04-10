package com.manish.smartcart.config;

import com.manish.smartcart.config.filter.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtFilter jwtFilter;
        private final RateLimitFilter rateLimitFilter;
        private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                        .csrf(csrf -> csrf.disable())
                        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                        // ── STATELESS SESSION ─────────────────────────────────────────────────────
                        // Our primary auth mechanism is JWT — no server-side HTTP sessions.
                        // STATELESS means Spring will never create or use an HttpSession.
                        .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                        // ── SECURITY CONTEXT REPOSITORY ───────────────────────────────────────────
                        // Stores the security context in request attributes (not in HTTP session).
                        // Required for async streaming endpoints (CSV export) to propagate identity.
                        .securityContext(context -> context
                                .securityContextRepository(new RequestAttributeSecurityContextRepository()))

                        .oauth2Login(oauth2Login ->
                                oauth2Login.successHandler(oAuth2LoginSuccessHandler)

                                        // The authorization endpoint — the URL that triggers the Google redirect.
                                        // Frontend "Login with Google" button links to this URL explicitly.
                                        .authorizationEndpoint(endpoint -> endpoint
                                                .baseUri("/oauth2/authorization"))
                                        // Where Google returns the user after successful login
                                        .redirectionEndpoint(endpoint -> endpoint
                                                .baseUri("/login/oauth2/code/*"))
                        )

                        .exceptionHandling(ex -> ex
                                .authenticationEntryPoint(
                                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                                )
                        )

                        .formLogin(form -> form.disable())

                        .authorizeHttpRequests(request -> request
                                // ── Fully Public Endpoints (no token or login required) ──────────────
                                .requestMatchers(
                                        "/api/v1/auth/**",           // Registration, login, OTP, password reset
                                        "/api/v1/guest-cart/**",     // Shopping before login
                                        "/api/v1/payments/verify",   // Razorpay frontend callback
                                        "/api/v1/payments/webhook",  // Razorpay backend webhook
                                        "/oauth2/**",                // OAuth2 authorization flow initiation
                                        "/login/oauth2/**",          // OAuth2 callback from Google
                                        "/swagger-ui/**",            // API documentation UI
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**",           // OpenAPI spec JSON
                                        "/actuator/**"               // Health, metrics, Prometheus scrape
                                )
                                .permitAll()

                                // ── Public Read-Only Product/Category/Review Endpoints ───────────────
                                // Browse the store without logging in — same as Amazon guest browsing.
                                .requestMatchers(HttpMethod.GET,
                                        "/api/v1/products",
                                        "/api/v1/products/search",
                                        "/api/v1/categories",
                                        "/api/v1/products/{slug}",
                                        "/api/v1/products/category/{categoryId}",
                                        "/api/v1/products/search/semantic",
                                        "/api/v1/reviews/**" // View reviews and rating distribution
                                )
                                .permitAll()
                                .anyRequest().authenticated())

                        // Register our custom filters in order:
                        // 1. RateLimitFilter → checks Bucket4j token before any processing
                        // 2. JwtFilter → validates Bearer token and populates SecurityContext
                        // Both run BEFORE Spring's built-in UsernamePasswordAuthenticationFilter
                        .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        /**
         * CORS Policy:
         * - Dev: allow all origins (change to your frontend URL in production)
         * - Exposes Authorization header so the frontend can read the JWT
         * - Caches pre-flight response for 1 hour (3600s)
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                // Dev: allow all. Production: replace with ["https://yourclientdomain.com"]
                config.setAllowedOriginPatterns(List.of("*"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setExposedHeaders(List.of("Authorization"));
                config.setAllowCredentials(true);
                config.setMaxAge(3600L); // Cache pre-flight for 1 hour

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        // Password Encoder Bean
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
