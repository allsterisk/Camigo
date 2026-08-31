package com.camigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PickupCodeGeneratorTest {

    private final PickupCodeGenerator generator = new PickupCodeGenerator();

    @Test
    void shouldKeepLeadingZeros() {
        assertThat(generator.format(0)).isEqualTo("0000");
        assertThat(generator.format(42)).isEqualTo("0042");
        assertThat(generator.format(9999)).isEqualTo("9999");
    }

    @Test
    void shouldRejectOutOfRangeValues() {
        assertThatThrownBy(() -> generator.format(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.format(10_000)).isInstanceOf(IllegalArgumentException.class);
    }
}
