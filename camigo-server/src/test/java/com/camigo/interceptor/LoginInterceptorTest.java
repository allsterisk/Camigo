package com.camigo.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import com.camigo.config.JwtProperties;
import com.camigo.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoginInterceptorTest {

    private static final String TEST_SECRET = "camigo-test-secret-must-be-at-least-32-bytes-long";

    @Test
    void shouldAcceptValidBearerToken() throws Exception {
        JwtUtil jwtUtil = new JwtUtil(new JwtProperties(TEST_SECRET, 30));
        LoginInterceptor interceptor = new LoginInterceptor(jwtUtil, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwtUtil.createToken(7L));

        boolean accepted = interceptor.preHandle(request, response, new Object());

        assertThat(accepted).isTrue();
        assertThat(request.getAttribute(LoginInterceptor.CURRENT_USER_ID)).isEqualTo(7L);
    }

    @Test
    void shouldReturnUnifiedUnauthorizedResponseWhenTokenMissing() throws Exception {
        JwtUtil jwtUtil = new JwtUtil(new JwtProperties(TEST_SECRET, 30));
        LoginInterceptor interceptor = new LoginInterceptor(jwtUtil, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean accepted = interceptor.preHandle(request, response, new Object());

        assertThat(accepted).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":401");
    }

    @Test
    void shouldAllowCorsPreflightWithoutBearerToken() throws Exception {
        JwtUtil jwtUtil = new JwtUtil(new JwtProperties(TEST_SECRET, 30));
        LoginInterceptor interceptor = new LoginInterceptor(jwtUtil, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/parcels/my");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(HttpHeaders.ORIGIN, "http://127.0.0.1:5173");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

        boolean accepted = interceptor.preHandle(request, response, new Object());

        assertThat(accepted).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
