package org.service.b.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.service.b.auth.message.Message;
import org.service.b.auth.model.User;
import org.service.b.auth.serviceimpl.UserPrinciple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtProvider {

    private static String jwtSecret;

    private static final Logger logger = LoggerFactory.getLogger(JwtProvider.class);

    @Value("${service.b.org.app.jwtSecret}")
    public void setJwtSecret(String jwtSecret) {
        JwtProvider.jwtSecret = jwtSecret;
    }

    @Value("${service.b.org.app.jwtExpiration}")
    private int jwtExpiration;

    @Value("${service.b.org.app.jwtResetExpiration}")
    private int resetExpiration;

    public String generateJwtToken(Authentication authentication) {
        UserPrinciple userPrincipal = (UserPrinciple) authentication.getPrincipal();
        String authorities = authentication.getAuthorities()
                                           .stream()
                                           .map(GrantedAuthority::getAuthority)
                                           .collect(Collectors.joining(","));
        return Jwts.builder()
                   .subject(userPrincipal.getUsername())
                   .issuedAt(new Date())
                   .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                   .claim("roles", authorities)
                   .signWith(getSigningKey())
                   .compact();
    }

    public String generatePasswordResetToken(User user) {
        return Jwts.builder()
                   .subject(user.getUsername())
                   .issuedAt(new Date())
                   .expiration(new Date(System.currentTimeMillis() + resetExpiration))
                   .signWith(getSigningKey())
                   .compact();
    }

    public String getUsernameFromJwtToken(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build()
                   .parseSignedClaims(token).getPayload().getSubject();
    }

    public Message validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return new Message(true);
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
            return new Message(e.toString(), false);
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            return new Message(e.toString(), false);
        } catch (ExpiredJwtException e) {
            logger.error("Expired JWT token: {}", e.getMessage());
            return new Message(e.toString(), false);
        } catch (UnsupportedJwtException e) {
            logger.error("Unsupported JWT token: {}", e.getMessage());
            return new Message(e.toString(), false);
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
            return new Message(e.toString(), false);
        }
    }

    private static SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

}
