package com.manish.smartcart.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtUtilService {

    @Value("${application.security.jwt.secret-key}")
    private String SECRET_KEY;

    @Value("${application.security.jwt.expiration}")
    private Long EXPIRATION;



    public String extractUsername(String token) {
         return extractClaim(token,Claims::getSubject);
    }

    public boolean validateToken(String authToken, UserDetails userDetails) {
           return isTokenValid(authToken, userDetails);
    }

    public String generateToken(String email) {
        Map<String, Object> claims = new HashMap<>();
         return generateToken(claims,email);
    }

    public String generateToken(Map<String,Object> claims, String email) {
           return Jwts.builder()
                   .claims(claims)
                   .subject(email)
                   .issuedAt(new Date(System.currentTimeMillis()))
                   .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                   .signWith(getSigningKey())
                   .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(extractUsername(token)) && !isTokenExpired(token));
    }

    // Internal helper methods
    private <T> T extractClaim(String token, Function<Claims,T>claimResolver){
          final Claims claims = extractAllClaims(token);
          return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token){
               return (Claims) Jwts.parser()
                       .verifyWith(getSigningKey())
                       .build()
                       .parse(token)
                       .getPayload();
    }
    private boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        return expiration!=null && expiration.before(new Date());
    }

    private Date extractExpiration(String token) {
          return extractClaim(token, Claims::getExpiration);
    }

    private SecretKey getSigningKey(){
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
