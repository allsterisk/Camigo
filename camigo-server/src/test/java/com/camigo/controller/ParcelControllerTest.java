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
import com.camigo.dto.ParcelCreateDTO;
import com.camigo.dto.ParcelStatusUpdateDTO;
import com.camigo.exception.GlobalExceptionHandler;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.ParcelService;
import com.camigo.service.LockerService;
import com.camigo.vo.ParcelPickupAccessVO;
import com.camigo.vo.ParcelVO;
import java.math.BigDecimal;
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
class ParcelControllerTest {

    @Mock
    private ParcelService parcelService;
    @Mock
    private LockerService lockerService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ParcelController(parcelService, lockerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateParcelWithPhaseFiveRequestContract() throws Exception {
        when(parcelService.create(eq(1L), any(ParcelCreateDTO.class))).thenReturn(parcel());

        mockMvc.perform(post("/api/parcels")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "trackingNo":"SF202608030001",
                                  "carrierCode":"SF",
                                  "sender":"李四",
                                  "receiver":"张三",
                                  "phone":"13800138000",
                                  "lockerId":3,
                                  "sizeClass":"M",
                                  "weightKg":1.25
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.targetLockerId").value(3))
                .andExpect(jsonPath("$.data.recipientName").value("张三"))
                .andExpect(jsonPath("$.data.targetLockerName").value("1号楼智能柜"));
    }

    @Test
    void shouldQueryCurrentUsersParcelsFromMyPath() throws Exception {
        when(parcelService.listMy(7L, 1L, 20L, null, null))
                .thenReturn(new PageResult<>(List.of(parcel()), 1, 1, 20));

        mockMvc.perform(get("/api/parcels/my")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].currentStatus").value("ARRIVED"));
    }

    @Test
    void shouldUpdateStatusThroughResourcePath() throws Exception {
        when(parcelService.updateStatus(eq(7L), eq(10L), any(ParcelStatusUpdateDTO.class)))
                .thenReturn(parcel());

        mockMvc.perform(put("/api/parcels/10/status")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\",\"remark\":\"确认收货\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(parcelService).updateStatus(eq(7L), eq(10L), any(ParcelStatusUpdateDTO.class));
    }

    @Test
    void shouldExposePickupCodeOnlyThroughOwnedParcelDetailPath() throws Exception {
        when(lockerService.getPickupAccess(7L, 10L)).thenReturn(new ParcelPickupAccessVO(
                10L, 51L, 3L, "1号楼快递柜", 21L, "M-01", "0042", null));

        mockMvc.perform(get("/api/parcels/10/pickup-access")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pickupCode").value("0042"));
    }

    private ParcelVO parcel() {
        return new ParcelVO(10L, "SF202608030001", "SF", 7L, "张三", "李四", null,
                9L, 3L, "1号楼智能柜", "M", new BigDecimal("1.25"), "ARRIVED",
                false, null, null, null);
    }
}
