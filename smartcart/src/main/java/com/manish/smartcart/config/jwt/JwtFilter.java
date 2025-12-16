package com.manish.smartcart.config.jwt;

import com.manish.smartcart.config.UsersUserDetails;
import com.manish.smartcart.config.UsersUserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Service //@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtilService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        String authToken = null;
        String username = null;
        if(header == null || !header.startsWith("Bearer ")){
             filterChain.doFilter(request,response);
             return;
        }
        authToken = header.substring(7);
        try{
            username = jwtService.extractUsername(authToken); // Uses JwtService
        } catch (Exception e) {
            filterChain.doFilter(request,response);
            return;
        }

        if(username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                  try{
                      UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                      if(jwtService.validateToken(authToken,userDetails)){
                          UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                  userDetails,
                                  null,
                                  userDetails.getAuthorities());
                          auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                          SecurityContextHolder.getContext().setAuthentication(auth);
                      }

                  }catch (JwtException e){
                      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Invalid token");
                  }
        }
         filterChain.doFilter(request,response);
    }
}
