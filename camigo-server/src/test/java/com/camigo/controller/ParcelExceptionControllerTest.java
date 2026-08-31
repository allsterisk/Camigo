package com.camigo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camigo.common.PageResult;
import com.camigo.dto.ParcelExceptionReportDTO;
import com.camigo.dto.ParcelExceptionResolutionDTO;
import com.camigo.exception.GlobalExceptionHandler;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.ParcelExceptionService;
import com.camigo.vo.ParcelExceptionVO;
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
class ParcelExceptionControllerTest {
    @Mock private ParcelExceptionService service;
    private MockMvc courierMvc;
    private MockMvc adminMvc;

    @BeforeEach
    void setUp() {
        courierMvc = MockMvcBuilders.standaloneSetup(new CourierExceptionController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        adminMvc = MockMvcBuilders.standaloneSetup(new AdminExceptionController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void shouldReportCourierException() throws Exception {
        when(service.report(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq(12L),
                any(ParcelExceptionReportDTO.class))).thenReturn(exception());
        courierMvc.perform(post("/api/courier-execution/orders/12/exceptions")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exceptionType\":\"LOCKER_FAILURE\",\"description\":\"柜门无法打开\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void shouldRejectIncompleteReport() throws Exception {
        courierMvc.perform(post("/api/courier-execution/orders/12/exceptions")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 7L)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListAndResolveAsAdmin() throws Exception {
        when(service.listAdmin(1, 10, "PENDING", "SF"))
                .thenReturn(new PageResult<>(List.of(exception()), 1, 1, 10));
        when(service.resolve(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(3L),
                any(ParcelExceptionResolutionDTO.class))).thenReturn(exception());
        adminMvc.perform(get("/api/admin/exceptions?page=1&pageSize=10&status=PENDING&keyword=SF"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        adminMvc.perform(post("/api/admin/exceptions/3/resolution")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionType\":\"RESUME_DELIVERY\",\"resolutionNote\":\"信息已核实\"}"))
                .andExpect(status().isOk());
        verify(service).resolve(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(3L),
                any(ParcelExceptionResolutionDTO.class));
    }

    private ParcelExceptionVO exception() {
        return new ParcelExceptionVO(3L, 4L, "SF001", 12L, "DO001", 2L, "配送员",
                "LOCKER_FAILURE", "柜门无法打开", true, "PENDING", LocalDateTime.now(),
                null, null, null, null, null);
    }
}
