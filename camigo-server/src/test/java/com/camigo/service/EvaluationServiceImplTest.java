package com.camigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.Evaluation;
import com.camigo.entity.ParcelEntity;
import com.camigo.exception.ForbiddenException;
import com.camigo.exception.ResourceNotFoundException;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.EvaluationMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.service.impl.EvaluationServiceImpl;
import com.camigo.vo.EvaluationVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    @Mock
    private EvaluationMapper evaluationMapper;
    @Mock
    private DeliveryOrderMapper orderMapper;
    @Mock
    private ParcelMapper parcelMapper;
    @Mock
    private CourierMapper courierMapper;

    private EvaluationServiceImpl service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "evaluation-service-test");
        TableInfoHelper.initTableInfo(assistant, Courier.class);
        TableInfoHelper.initTableInfo(assistant, DeliveryOrder.class);
        TableInfoHelper.initTableInfo(assistant, ParcelEntity.class);
        TableInfoHelper.initTableInfo(assistant, Evaluation.class);
        service = new EvaluationServiceImpl(evaluationMapper, orderMapper, parcelMapper, courierMapper);
    }

    private ParcelEntity parcel(long id, long recipientUserId) {
        ParcelEntity parcel = new ParcelEntity();
        parcel.setId(id);
        parcel.setRecipientUserId(recipientUserId);
        return parcel;
    }

    private Evaluation evaluation(long id, long orderId, int deliveryScore, int serviceScore) {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(id);
        evaluation.setDeliveryOrderId(orderId);
        evaluation.setUserId(100L);
        evaluation.setCourierId(20L);
        evaluation.setDeliveryScore(deliveryScore);
        evaluation.setServiceScore(serviceScore);
        evaluation.setContent("配送很快");
        evaluation.setStatus("VISIBLE");
        evaluation.setEvaluatedTime(LocalDateTime.of(2026, 9, 1, 10, 0));
        return evaluation;
    }

    @Test
    void listByParcelReturnsEvaluationsForOwner() {
        when(parcelMapper.selectById(1L)).thenReturn(parcel(1L, 100L));
        when(evaluationMapper.selectVisibleByParcelId(1L)).thenReturn(List.of(
                evaluation(2L, 10L, 5, 4), evaluation(1L, 9L, 3, 4)));

        List<EvaluationVO> result = service.listByParcel(100L, List.of("USER"), 1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).deliveryScore()).isEqualTo(5);
        assertThat(result.get(1).deliveryOrderId()).isEqualTo(9L);
    }

    @Test
    void listByParcelRejectsNonOwner() {
        when(parcelMapper.selectById(1L)).thenReturn(parcel(1L, 100L));

        assertThatThrownBy(() -> service.listByParcel(999L, List.of("USER"), 1L))
                .isInstanceOf(ForbiddenException.class);
        verify(evaluationMapper, never()).selectVisibleByParcelId(any());
    }

    @Test
    void listByParcelAllowsAdminRegardlessOfOwner() {
        when(parcelMapper.selectById(1L)).thenReturn(parcel(1L, 100L));
        when(evaluationMapper.selectVisibleByParcelId(1L)).thenReturn(List.of(evaluation(1L, 10L, 5, 5)));

        List<EvaluationVO> result = service.listByParcel(1L, List.of("ADMIN"), 1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void listByParcelThrowsWhenParcelMissing() {
        when(parcelMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.listByParcel(100L, List.of("USER"), 404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByOrderReturnsEvaluationForAssignedCourier() {
        DeliveryOrder order = new DeliveryOrder();
        order.setId(10L);
        order.setAssignedCourierId(20L);
        when(orderMapper.selectById(10L)).thenReturn(order);
        Courier courier = new Courier();
        courier.setId(20L);
        courier.setUserId(200L);
        when(courierMapper.selectOne(any())).thenReturn(courier);
        when(evaluationMapper.selectVisibleByOrderId(10L)).thenReturn(evaluation(1L, 10L, 5, 5));

        EvaluationVO result = service.getByOrder(200L, List.of("COURIER"), 10L);

        assertThat(result).isNotNull();
        assertThat(result.deliveryOrderId()).isEqualTo(10L);
        assertThat(result.content()).isEqualTo("配送很快");
    }

    @Test
    void getByOrderReturnsNullWhenNotEvaluated() {
        DeliveryOrder order = new DeliveryOrder();
        order.setId(10L);
        order.setAssignedCourierId(20L);
        when(orderMapper.selectById(10L)).thenReturn(order);
        Courier courier = new Courier();
        courier.setId(20L);
        courier.setUserId(200L);
        when(courierMapper.selectOne(any())).thenReturn(courier);
        when(evaluationMapper.selectVisibleByOrderId(10L)).thenReturn(null);

        assertThat(service.getByOrder(200L, List.of("COURIER"), 10L)).isNull();
    }

    @Test
    void getByOrderRejectsCourierNotAssigned() {
        DeliveryOrder order = new DeliveryOrder();
        order.setId(10L);
        order.setAssignedCourierId(20L);
        when(orderMapper.selectById(10L)).thenReturn(order);
        Courier courier = new Courier();
        courier.setId(21L);
        courier.setUserId(201L);
        when(courierMapper.selectOne(any())).thenReturn(courier);

        assertThatThrownBy(() -> service.getByOrder(201L, List.of("COURIER"), 10L))
                .isInstanceOf(ForbiddenException.class);
        verify(evaluationMapper, never()).selectVisibleByOrderId(any());
    }

    @Test
    void getByOrderAllowsAdminWithoutCourierLookup() {
        DeliveryOrder order = new DeliveryOrder();
        order.setId(10L);
        order.setAssignedCourierId(20L);
        when(orderMapper.selectById(10L)).thenReturn(order);
        when(evaluationMapper.selectVisibleByOrderId(10L)).thenReturn(evaluation(1L, 10L, 4, 4));

        EvaluationVO result = service.getByOrder(1L, List.of("ADMIN"), 10L);

        assertThat(result).isNotNull();
        verify(courierMapper, never()).selectOne(any());
    }

    @Test
    void getByOrderThrowsWhenOrderMissing() {
        when(orderMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.getByOrder(200L, List.of("COURIER"), 404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
