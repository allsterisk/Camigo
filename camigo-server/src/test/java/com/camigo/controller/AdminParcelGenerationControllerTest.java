package com.camigo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camigo.exception.GlobalExceptionHandler;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.AdminParcelGenerationService;
import com.camigo.vo.AdminGeneratedParcelVO;
import com.camigo.vo.AdminParcelGenerationOptionsVO;
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
class AdminParcelGenerationControllerTest {
    @Mock private AdminParcelGenerationService generationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminParcelGenerationController(generationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnGenerationOptions() throws Exception {
        when(generationService.options(1L)).thenReturn(
                new AdminParcelGenerationOptionsVO(List.of(), List.of(), List.of(),
                        List.of("S", "M", "L", "XL"), 10));

        mockMvc.perform(get("/api/admin/parcel-generation/options")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sizeClasses.length()").value(4))
                .andExpect(jsonPath("$.data.maxBatchSize").value(10));
    }

    @Test
    void shouldGenerateBatchThroughService() throws Exception {
        when(generationService.generate(org.mockito.ArgumentMatchers.eq(1L), any()))
                .thenReturn(List.of(new AdminGeneratedParcelVO(
                        101L, "SF001", "SF", 201L, "DO001",
                        "WAITING_ASSIGNMENT", "ASSIGNING", 1, 3L)));

        mockMvc.perform(post("/api/admin/parcel-generation")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientUserId":2,"pickupLocationId":1,
                                 "destinationLocationId":2,"sizeClass":"M",
                                 "weightKg":1.25,"count":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].parcelStatus").value("WAITING_ASSIGNMENT"))
                .andExpect(jsonPath("$.data[0].orderStatus").value("ASSIGNING"));

        verify(generationService).generate(org.mockito.ArgumentMatchers.eq(1L), any());
    }

    @Test
    void shouldRejectInvalidBatchBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/admin/parcel-generation")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientUserId":2,"pickupLocationId":1,
                                 "destinationLocationId":2,"sizeClass":"M",
                                 "weightKg":0,"count":11}
                                """))
                .andExpect(status().isBadRequest());
    }
}
