package com.camigo.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UserDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidRegistration() {
        UserRegisterDTO request = new UserRegisterDTO(
                "student_001", "Camigo@123", "13800138000", "USER", "张三", "20260001");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void shouldRejectUnknownRoleAndInvalidPhone() {
        UserRegisterDTO request = new UserRegisterDTO(
                "student_001", "Camigo@123", "123", "ROOT", "张三", null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("phone", "role");
    }

    @Test
    void shouldRejectInvalidAccountStatus() {
        UserUpdateDTO request = new UserUpdateDTO(null, null, null, "UNKNOWN");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("status");
    }
}
