package com.camigo.utils;

import com.camigo.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * JWT 创建、验签与用户编号解析工具。
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtUtil(JwtProperties properties) {
        String secret = properties.secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET 必须至少包含 32 字节");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(properties.expirationMinutes());
    }

    public String createToken(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户编号必须为正数");
        }
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            long userId = Long.parseLong(parseClaims(token).getSubject());
            return userId > 0;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Long getUserId(String token) {
        try {
            long userId = Long.parseLong(parseClaims(token).getSubject());
            if (userId <= 0) {
                throw new IllegalArgumentException("JWT 用户编号必须为正数");
            }
            return userId;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("JWT 用户编号格式不正确", ex);
        }
    }

    private Claims parseClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT 不能为空");
        }
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
