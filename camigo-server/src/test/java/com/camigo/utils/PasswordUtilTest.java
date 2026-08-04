package com.camigo.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordUtilTest {

    @Test
    void shouldEncodeAndMatchPassword() {
        PasswordUtil passwordUtil = new PasswordUtil(new BCryptPasswordEncoder());

        String encoded = passwordUtil.encode("Camigo@123");

        assertThat(encoded).isNotEqualTo("Camigo@123");
        assertThat(passwordUtil.matches("Camigo@123", encoded)).isTrue();
        assertThat(passwordUtil.matches("wrong", encoded)).isFalse();
    }
}
