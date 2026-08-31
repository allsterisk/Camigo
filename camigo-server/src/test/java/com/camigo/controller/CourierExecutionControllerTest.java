package com.camigo.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camigo.exception.GlobalExceptionHandler;
import com.camigo.interceptor.LoginInterceptor;
import com.camigo.service.CourierExecutionService;
import com.camigo.service.LockerService;
import com.camigo.vo.CampusLocationOptionVO;
import com.camigo.vo.CourierExecutionVO;
import com.camigo.vo.CourierLocationStateVO;
import com.camigo.vo.CourierRouteVO;
import com.camigo.vo.CourierStorageVO;
import com.camigo.vo.LockerSlotOptionVO;
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
class CourierExecutionControllerTest {

    @Mock private CourierExecutionService executionService;
    @Mock private LockerService lockerService;
    private MockMvc mockMvc;
    private MockMvc locationMockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new CourierExecutionController(executionService, lockerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        locationMockMvc = MockMvcBuilders.standaloneSetup(
                        new CourierLocationController(executionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldListLocationsAndConfirmCurrentLocation() throws Exception {
        when(executionService.activeLocations()).thenReturn(
                List.of(new CampusLocationOptionVO(25L, "CAMIGO-GATE-E", "食堂北快递服务站")));
        when(executionService.confirmLocation(8L, 25L)).thenReturn(new CourierLocationStateVO(
                3L, 25L, "食堂北快递服务站", LocalDateTime.now(),
                "AVAILABLE", 2, true));

        locationMockMvc.perform(get("/api/couriers/locations")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 8L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].locationName").value("食堂北快递服务站"));

        locationMockMvc.perform(put("/api/couriers/me/location")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 8L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locationId\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationConfirmed").value(true));
    }

    @Test
    void shouldExposeRouteAndExecutionActions() throws Exception {
        CourierRouteVO route = new CourierRouteVO(3L, 25L, "食堂北快递服务站",
                4, 260, 307, "campus-dijkstra-insertion-v1", List.of());
        when(executionService.currentRoute(8L)).thenReturn(route);
        when(executionService.confirmPickup(8L, 5L)).thenReturn(new CourierExecutionVO(
                5L, "DO001", "IN_TRANSIT", "DELIVERING", 25L,
                "食堂北快递服务站", route));
        when(executionService.confirmArrival(8L, 5L)).thenReturn(new CourierExecutionVO(
                5L, "DO001", "ARRIVED", "DELIVERING", 31L, "1号楼", route));

        mockMvc.perform(get("/api/courier-execution/route")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 8L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDistanceMeters").value(260));
        mockMvc.perform(post("/api/courier-execution/orders/5/pickup")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 8L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("IN_TRANSIT"));
        mockMvc.perform(post("/api/courier-execution/orders/5/arrival")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 8L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("ARRIVED"));

        verify(executionService).confirmPickup(8L, 5L);
        verify(executionService).confirmArrival(8L, 5L);
    }

    @Test
    void shouldListCompatibleSlotsAndStoreWithoutReturningPickupCode() throws Exception {
        CourierRouteVO route = new CourierRouteVO(3L, 31L, "1号楼",
                5, 0, 0, "campus-dijkstra-insertion-v1", List.of());
        when(lockerService.listCompatibleSlots(8L, 5L)).thenReturn(List.of(
                new LockerSlotOptionVO(21L, 3L, "1号楼快递柜", "M-01", "M")));
        when(lockerService.storeParcel(8L, 5L, 21L)).thenReturn(new CourierStorageVO(
                61L, 5L, "COMPLETED", "READY_PICKUP", 3L, 21L,
                "M-01", 31L, route));

        mockMvc.perform(get("/api/courier-execution/orders/5/locker-slots")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 8L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sizeClass").value("M"));
        mockMvc.perform(post("/api/courier-execution/orders/5/storage")
                        .requestAttr(LoginInterceptor.CURRENT_USER_ID, 8L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lockerSlotId\":21}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.pickupCode").doesNotExist());
    }
}
