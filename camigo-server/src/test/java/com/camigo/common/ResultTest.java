package com.camigo.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void shouldCreateSuccessAndErrorResults() {
        Result<String> success = Result.success("ok");
        Result<Void> error = Result.error(400, "参数错误");

        assertThat(success.code()).isEqualTo(200);
        assertThat(success.data()).isEqualTo("ok");
        assertThat(error.code()).isEqualTo(400);
        assertThat(error.message()).isEqualTo("参数错误");
    }

    @Test
    void shouldValidatePageArguments() {
        PageResult<Long> page = new PageResult<>(List.of(1L, 2L), 2, 1, 20);

        assertThat(page.records()).containsExactly(1L, 2L);
        assertThatThrownBy(() -> new PageResult<>(List.of(), 0, 0, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
