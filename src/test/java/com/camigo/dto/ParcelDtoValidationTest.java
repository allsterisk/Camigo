package com.camigo.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ParcelDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidParcelCreation() {
        ParcelCreateDTO request = new ParcelCreateDTO(
                "SF202608030001", "SF", "李四", "张三", "13800138000",
                1L, "M", new BigDecimal("1.25"));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void shouldRejectInvalidTrackingPhoneLockerAndSize() {
        ParcelCreateDTO request = new ParcelCreateDTO(
                "?", "SF", "李四", "张三", "123", 0L, "XX", null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("trackingNo", "phone", "lockerId", "sizeClass");
    }

    @Test
    void shouldAcceptOnlyPublicParcelStatuses() {
        ParcelStatusUpdateDTO request = new ParcelStatusUpdateDTO("PICKED_UP", null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("status");
    }
}
