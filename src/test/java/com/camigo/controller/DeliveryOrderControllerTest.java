package com.camigo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camigo.common.PageResult;
import com.camigo.dto.DeliveryOrderCreateDTO;
import com.camigo.dto.DeliveryOrderStatusDTO;
import com.camigo.exception.GlobalExceptionHandler;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.DeliveryOrderService;
import com.camigo.vo.DeliveryOrderVO;
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
class DeliveryOrderControllerTest {

    @Mock
    private DeliveryOrderService deliveryOrderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DeliveryOrderController(deliveryOrderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateOrderAtPhaseFivePath() throws Exception {
        when(deliveryOrderService.create(eq(7L), any(DeliveryOrderCreateDTO.class)))
                .thenReturn(order("WAITING"));

        mockMvc.perform(post("/api/delivery-orders")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parcelId":10,
                                  "priority":"NORMAL",
                                  "predictedMinutes":30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("WAITING"));
    }

    @Test
    void shouldListCourierTasks() throws Exception {
        when(deliveryOrderService.listTasks(8L, 1L, 20L, "WAITING"))
                .thenReturn(new PageResult<>(List.of(order("WAITING")), 1, 1, 20));

        mockMvc.perform(get("/api/delivery-orders/tasks")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 8L)
                        .param("status", "WAITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].status").value("WAITING"));
    }

    @Test
    void shouldAcceptOrderThroughResourcePath() throws Exception {
        when(deliveryOrderService.accept(8L, 5L)).thenReturn(order("PROCESSING"));

        mockMvc.perform(put("/api/delivery-orders/5/accept")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 8L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }

    @Test
    void shouldUpdateOrderStatusThroughResourcePath() throws Exception {
        when(deliveryOrderService.updateStatus(eq(8L), eq(5L), any(DeliveryOrderStatusDTO.class)))
                .thenReturn(order("DELIVERED"));

        mockMvc.perform(put("/api/delivery-orders/5/status")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 8L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        verify(deliveryOrderService).updateStatus(
                eq(8L), eq(5L), any(DeliveryOrderStatusDTO.class));
    }

    @Test
    void shouldRejectInternalStatusInRequest() throws Exception {
        mockMvc.perform(put("/api/delivery-orders/5/status")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 8L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_TRANSIT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    private DeliveryOrderVO order(String status) {
        return new DeliveryOrderVO(5L, "DO202608030001", 10L, 9L, 3L,
                "NORMAL", status, 30, null, null, null, null);
    }
}
