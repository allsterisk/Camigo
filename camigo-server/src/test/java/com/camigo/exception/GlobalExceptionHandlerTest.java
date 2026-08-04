package com.camigo.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.camigo.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleResourceNotFound() {
        ResponseEntity<Result<Void>> response = handler.handleBusiness(
                new ResourceNotFoundException("用户不存在"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(404);
    }

    @Test
    void shouldHandleForbiddenRequest() {
        ResponseEntity<Result<Void>> response = handler.handleBusiness(
                new ForbiddenException("没有操作权限"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(403);
    }

    @Test
    void shouldReturnNotFoundWhenStaticResourceDoesNotExist() {
        ResponseEntity<Result<Void>> response = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "favicon.ico"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("请求资源不存在");
    }
}
