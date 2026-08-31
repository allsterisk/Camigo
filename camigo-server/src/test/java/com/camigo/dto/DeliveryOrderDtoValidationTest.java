package com.camigo.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DeliveryOrderDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidCreateRequest() {
        DeliveryOrderCreateDTO request = new DeliveryOrderCreateDTO(1L, null, "NORMAL", 30, 25L);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void shouldRejectInvalidCreateFields() {
        DeliveryOrderCreateDTO request = new DeliveryOrderCreateDTO(0L, -1L, "IMMEDIATE", 1441, 0L);

        assertThat(validator.validate(request)).hasSize(5);
    }

    @Test
    void shouldAcceptPublicDeliveryStatus() {
        assertThat(validator.validate(new DeliveryOrderStatusDTO("DELIVERED"))).isEmpty();
    }

    @Test
    void shouldRejectInternalDeliveryStatus() {
        assertThat(validator.validate(new DeliveryOrderStatusDTO("IN_TRANSIT")))
                .hasSize(1);
    }
}
