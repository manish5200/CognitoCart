package com.manish.smartcart.config.jwt;

import com.manish.smartcart.config.CustomUserDetails;
import com.manish.smartcart.config.CustomUserDetailsService;
import com.manish.smartcart.service.TokenBlacklistService;
import com.manish.smartcart.util.AppConstants;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Service //@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/auth");
    }


    // ─── CHANGE : In doFilterInternal — add blacklist check ──────────────────
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(AppConstants.AUTH_HEADER); // "Authorization"
        String authToken;
        String username;

        if(header == null || !header.startsWith(AppConstants.TOKEN_PREFIX)){  //"Bearer "
             filterChain.doFilter(request,response);
             return;
        }
        authToken = header.substring(7);

        try{
            username = jwtUtil.extractUsername(authToken); // Uses JwtService
        } catch (Exception e) {
            filterChain.doFilter(request,response);
            return;
        }

        if(username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try{
                // 🔒 NEW: Reject blacklisted tokens (logged-out sessions)
                String jti = jwtUtil.extractJti(authToken);
                if(tokenBlacklistService.isBlacklisted(jti)){
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            "Token has been invalidated. Please log in again.");
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // FORCE LOGOUT CHECK: Was this token issued before the user's last password change?
                // Handles the case where an attacker's session must be killed after a reset.
                LocalDateTime issuedAt = jwtUtil.extractIssuedAt(authToken);
                LocalDateTime passwordChangedAt = ((CustomUserDetails)userDetails).getUser().getPasswordChangedAt();

                if(passwordChangedAt != null && issuedAt.isBefore(passwordChangedAt)){
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            "Session expired due to a password change. Please log in again.");
                    return;
                }

                if(jwtUtil.validateToken(authToken,userDetails)){
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }catch (JwtException e){
                //response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Invalid token");
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request,response);
    }
}
