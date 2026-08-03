package com.camigo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 外部化配置。密钥必须通过 JWT_SECRET 环境变量注入。
 */
@ConfigurationProperties(prefix = "camigo.jwt")
public record JwtProperties(String secret, long expirationMinutes) {

    public JwtProperties {
        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException("JWT 过期时间必须大于 0 分钟");
        }
    }
}
