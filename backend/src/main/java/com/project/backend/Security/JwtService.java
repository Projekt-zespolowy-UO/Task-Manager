package com.project.backend.Security;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.project.backend.Dto.JwtAuthDto;

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
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
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

        return false;
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
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

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

