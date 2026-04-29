package com.project.backend.Security;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.project.backend.Dto.JwtAuthDto;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@Component
public class JwtService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey signingKey;

    @PostConstruct
    void initSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public JwtAuthDto generateToken(String email) {
        JwtAuthDto dto = new JwtAuthDto();
        dto.setToken(generateJwtToken(email));
        dto.setRefreshToken(generateRefreshToken(email));
        return dto;
    }

    public JwtAuthDto refreshBaseToken(String email, String refreshToken) {
        JwtAuthDto dto = new JwtAuthDto();
        dto.setToken(generateJwtToken(email));
        dto.setRefreshToken(generateRefreshToken(email));
        return dto;
    }

    public boolean validateToken(String token) {
        return parseClaimsSafely(token).isPresent();
    }

    public Optional<String> getEmailFromValidToken(String token) {
        return parseClaimsSafely(token)
                .map(Claims::getSubject);
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    private Optional<Claims> parseClaimsSafely(String token) {
        try {
            return Optional.of(parseClaims(token));
        } catch (ExpiredJwtException e) {
            LOGGER.error("Token expired", e);
        } catch (UnsupportedJwtException e) {
            LOGGER.error("Unsupported token", e);
        } catch (MalformedJwtException e) {
            LOGGER.error("Malformed token", e);
        } catch (SignatureException e) {
            LOGGER.error("Signature validation failed", e);
        } catch (Exception e) {
            LOGGER.error("Invalid token", e);
        }

        return Optional.empty();
    }

    private String generateJwtToken(String email) {
        java.util.Date expiration = Date.from(LocalDateTime.now().plusMinutes(10).atZone(ZoneId.systemDefault()).toInstant());
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new java.util.Date())
                .setExpiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    String generateRefreshToken(String email) {
        java.util.Date expiration = Date.from(LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant());
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new java.util.Date())
                .setExpiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }
}

