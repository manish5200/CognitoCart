package com.manish.smartcart.config;

import com.manish.smartcart.config.filter.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
                        .oauth2Login(oauth2Login -> oauth2Login.successHandler(oAuth2LoginSuccessHandler)
                        )
                        .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .authorizeHttpRequests(request -> request
                                .requestMatchers(
                                        "/api/v1/auth/**",
                                        "/api/v1/guest-cart/**",
                                        "/api/v1/payments/verify",
                                        "/api/v1/payments/webhook",
                                        "/login/oauth2/**",
                                        "/oauth2/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**",
                                        "/actuator/**")
                                .permitAll()
                                .requestMatchers(HttpMethod.GET,
                                        "/api/v1/products",
                                        "/api/v1/products/search",
                                        "/api/v1/categories",
                                        "/api/v1/products/{slug}",
                                        "/api/v1/products/category/{categoryId}",
                                        "/api/v1/products/search/semantic",
                                        "/api/v1/reviews/**")
                                        .permitAll()
                                        .anyRequest().authenticated())
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
