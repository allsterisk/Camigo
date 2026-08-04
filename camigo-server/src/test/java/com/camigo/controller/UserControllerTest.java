package com.camigo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camigo.dto.UserRegisterDTO;
import com.camigo.dto.UserUpdateDTO;
import com.camigo.exception.GlobalExceptionHandler;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.UserService;
import com.camigo.vo.UserInfoVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldRegisterThroughPluralUsersPath() throws Exception {
        UserInfoVO user = userInfo(7L, "ACTIVE");
        when(userService.register(any(UserRegisterDTO.class))).thenReturn(user);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"student_001",
                                  "password":"Camigo@123",
                                  "phone":"13800138000",
                                  "role":"USER",
                                  "realName":"张三",
                                  "studentNo":"20260001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"));
    }

    @Test
    void shouldRejectInvalidRegistrationRoleBeforeService() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"student_001",
                                  "password":"Camigo@123",
                                  "phone":"13800138000",
                                  "role":"ROOT",
                                  "realName":"张三"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldUpdateCurrentUserThroughResourcePath() throws Exception {
        when(userService.updateUser(eq(7L), eq(7L), any(UserUpdateDTO.class)))
                .thenReturn(userInfo(7L, "ACTIVE"));

        mockMvc.perform(put("/api/users/7")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"realName\":\"新姓名\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userService).updateUser(eq(7L), eq(7L), any(UserUpdateDTO.class));
    }

    private UserInfoVO userInfo(Long id, String status) {
        return new UserInfoVO(id, "student_001", "张三", "13800138000",
                "20260001", status, List.of("USER"));
    }
}
