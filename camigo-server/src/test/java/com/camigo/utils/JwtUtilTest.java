package com.camigo.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.camigo.config.JwtProperties;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private static final String TEST_SECRET = "camigo-test-secret-must-be-at-least-32-bytes-long";

    @Test
    void shouldCreateValidateAndParseToken() {
        JwtUtil jwtUtil = new JwtUtil(new JwtProperties(TEST_SECRET, 30));

        String token = jwtUtil.createToken(42L);

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUserId(token)).isEqualTo(42L);
        assertThat(jwtUtil.validateToken(token + "tampered")).isFalse();
    }

    @Test
    void shouldRejectWeakSecret() {
        assertThatThrownBy(() -> new JwtUtil(new JwtProperties("too-short", 30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32");
    }
}
