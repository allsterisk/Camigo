package com.camigo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camigo.dispatch.IntelligentDispatchService;
import com.camigo.entity.DeliveryOrder;
import com.camigo.exception.GlobalExceptionHandler;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.vo.DispatchOfferVO;
import java.math.BigDecimal;
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
class DispatchControllerTest {

    @Mock
    private IntelligentDispatchService dispatchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DispatchController(dispatchService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnAllCurrentThreeMinuteOffers() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 9, 0);
        when(dispatchService.currentOffers(8L)).thenReturn(List.of(
                new DispatchOfferVO(
                        12L, 5L, "DO001", 25L, "食堂北快递服务站", 26L, "一号学生公寓",
                        120, new BigDecimal("81.2500"), now, now.plusMinutes(3)),
                new DispatchOfferVO(
                        13L, 6L, "DO002", 25L, "食堂北快递服务站", 27L, "二号学生公寓",
                        180, new BigDecimal("78.5000"), now, now.plusMinutes(3))));

        mockMvc.perform(get("/api/delivery-assignments/current")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 8L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].assignmentId").value(12))
                .andExpect(jsonPath("$.data[0].routeIncrementMeters").value(120))
                .andExpect(jsonPath("$.data[1].assignmentId").value(13));
    }

    @Test
    void shouldAcceptOffer() throws Exception {
        DeliveryOrder order = new DeliveryOrder();
        order.setId(5L);
        order.setOrderNo("DO001");
        order.setAssignedCourierId(3L);
        order.setStatus("ACCEPTED");
        order.setAcceptedTime(LocalDateTime.now());
        when(dispatchService.acceptAssignment(8L, 12L)).thenReturn(order);

        mockMvc.perform(put("/api/delivery-assignments/12/accept")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 8L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void shouldRequireDetailForOtherReasonInServiceRequest() throws Exception {
        mockMvc.perform(put("/api/delivery-assignments/12/reject")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 8L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reasonCode\":\"OTHER\",\"reasonDetail\":\"临时有事\"}"))
                .andExpect(status().isOk());

        verify(dispatchService).rejectAssignment(org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.eq(12L), any());
    }

    @Test
    void shouldReturnAdminDetailsByParcel() throws Exception {
        when(dispatchService.adminDetailsByParcel(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/delivery-assignments/parcels/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(dispatchService).adminDetailsByParcel(10L);
    }

    @Test
    void shouldLetAdminStartNextRoundByParcel() throws Exception {
        mockMvc.perform(post("/api/delivery-assignments/parcels/10/start")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 7L))
                .andExpect(status().isOk());

        verify(dispatchService).startDispatchByParcel(7L, 10L);
    }
}
