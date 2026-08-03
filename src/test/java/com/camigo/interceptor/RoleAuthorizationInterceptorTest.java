package com.camigo.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.camigo.annotation.RequireRoles;
import com.camigo.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

@ExtendWith(MockitoExtension.class)
class RoleAuthorizationInterceptorTest {

    @Mock
    private UserMapper userMapper;

    @Test
    void shouldAllowRequiredRole() throws Exception {
        RoleAuthorizationInterceptor interceptor = interceptor();
        MockHttpServletRequest request = requestForUser(1L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(userMapper.selectRoleCodes(1L)).thenReturn(List.of("ADMIN"));

        assertThat(interceptor.preHandle(request, response, adminHandler())).isTrue();
        assertThat(request.getAttribute(RoleAuthorizationInterceptor.CURRENT_USER_ROLES))
                .isEqualTo(List.of("ADMIN"));
    }

    @Test
    void shouldRejectMissingRoleWithForbiddenResponse() throws Exception {
        RoleAuthorizationInterceptor interceptor = interceptor();
        MockHttpServletRequest request = requestForUser(7L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(userMapper.selectRoleCodes(7L)).thenReturn(List.of("USER"));

        assertThat(interceptor.preHandle(request, response, adminHandler())).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":403");
    }

    private RoleAuthorizationInterceptor interceptor() {
        return new RoleAuthorizationInterceptor(userMapper, new ObjectMapper());
    }

    private MockHttpServletRequest requestForUser(Long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(LoginInterceptor.CURRENT_USER_ID, userId);
        return request;
    }

    private HandlerMethod adminHandler() throws NoSuchMethodException {
        SecuredController controller = new SecuredController();
        Method method = SecuredController.class.getDeclaredMethod("adminOnly");
        return new HandlerMethod(controller, method);
    }

    private static class SecuredController {
        @RequireRoles("ADMIN")
        void adminOnly() {
        }
    }
}
