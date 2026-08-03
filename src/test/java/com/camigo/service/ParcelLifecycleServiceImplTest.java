package com.camigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.camigo.entity.ParcelEntity;
import com.camigo.entity.ParcelStatusLog;
import com.camigo.exception.BusinessException;
import com.camigo.mapper.ParcelMapper;
import com.camigo.mapper.ParcelStatusLogMapper;
import com.camigo.service.impl.ParcelLifecycleServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParcelLifecycleServiceImplTest {

    @Mock
    private ParcelMapper parcelMapper;
    @Mock
    private ParcelStatusLogMapper statusLogMapper;

    private ParcelLifecycleServiceImpl service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "parcel-lifecycle-test");
        TableInfoHelper.initTableInfo(assistant, ParcelEntity.class);
        service = new ParcelLifecycleServiceImpl(parcelMapper, statusLogMapper);
    }

    @Test
    void shouldUpdateStatusAndWriteAuditLog() {
        ParcelEntity parcel = new ParcelEntity();
        parcel.setId(1L);
        parcel.setCurrentStatus("REGISTERED");
        when(parcelMapper.update(isNull(), any())).thenReturn(1);

        service.changeStatus(parcel, "WAITING_ASSIGNMENT", 8L, "USER", "创建配送任务");

        assertThat(parcel.getCurrentStatus()).isEqualTo("WAITING_ASSIGNMENT");
        verify(statusLogMapper).insert(any(ParcelStatusLog.class));
    }

    @Test
    void shouldRejectIllegalTransition() {
        ParcelEntity parcel = new ParcelEntity();
        parcel.setId(1L);
        parcel.setCurrentStatus("REGISTERED");

        assertThatThrownBy(() -> service.changeStatus(parcel, "PICKED_UP", 8L, "USER", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能从");
    }

    @Test
    void shouldAllowPickedParcelToBecomeCompleted() {
        ParcelEntity parcel = new ParcelEntity();
        parcel.setId(1L);
        parcel.setCurrentStatus("PICKED_UP");
        when(parcelMapper.update(isNull(), any())).thenReturn(1);

        service.changeStatus(parcel, "COMPLETED", 8L, "USER", "确认收货");

        assertThat(parcel.getCurrentStatus()).isEqualTo("COMPLETED");
        verify(statusLogMapper).insert(any(ParcelStatusLog.class));
    }
}
