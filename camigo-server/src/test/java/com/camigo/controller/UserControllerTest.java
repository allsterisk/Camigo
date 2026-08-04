package com.camigo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camigo.dto.UserRegisterDTO;
import com.camigo.common.PageResult;
import com.camigo.dto.UserUpdateDTO;
import com.camigo.exception.GlobalExceptionHandler;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.UserService;
import com.camigo.vo.UserInfoVO;
import com.camigo.vo.AdminUserVO;
import java.time.LocalDateTime;
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

    @Test
    void shouldListUsersForAdmin() throws Exception {
        AdminUserVO user = new AdminUserVO(7L, "student_001", "张三", "13800138000",
                "20260001", "ACTIVE", "USER", null, LocalDateTime.now(), LocalDateTime.now());
        when(userService.listUsers(1L, 1, 20, "张", "ACTIVE", "USER"))
                .thenReturn(new PageResult<>(List.of(user), 1, 1, 20));

        mockMvc.perform(get("/api/users")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 1L)
                        .param("keyword", "张").param("status", "ACTIVE").param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].roles[0]").value("USER"));
    }

    @Test
    void shouldDeleteUserForAdmin() throws Exception {
        mockMvc.perform(delete("/api/users/7")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 1L))
                .andExpect(status().isOk());
        verify(userService).deleteUser(1L, 7L);
    }

    private UserInfoVO userInfo(Long id, String status) {
        return new UserInfoVO(id, "student_001", "张三", "13800138000",
                "20260001", status, List.of("USER"));
    }
}
