package com.camigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.camigo.exception.BusinessException;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.LockerMapper;
import com.camigo.service.impl.AdminManagementServiceImpl;
import com.camigo.vo.AdminCourierVO;
import com.camigo.vo.AdminLockerVO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminManagementServiceImplTest {
    @Mock CourierMapper courierMapper;
    @Mock LockerMapper lockerMapper;

    @Test
    void shouldReturnCourierManagementPage() {
        AdminCourierVO courier = new AdminCourierVO(1L, 2L, "C001", "配送员", "13800138000",
                "ACTIVE", "AVAILABLE", 5, new BigDecimal("4.80"), 10L, 9L, 1L, new BigDecimal("900"));
        Page<AdminCourierVO> page = new Page<>(1, 10);
        page.setRecords(List.of(courier)); page.setTotal(1);
        when(courierMapper.selectAdminPage(any(), eq("配送"), eq("ACTIVE"))).thenReturn(page);

        var result = new AdminManagementServiceImpl(courierMapper, lockerMapper)
                .listCouriers(1, 10, " 配送 ", "active");

        assertThat(result.records()).containsExactly(courier);
        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    void shouldRejectUnsupportedLockerStatus() {
        AdminManagementServiceImpl service = new AdminManagementServiceImpl(courierMapper, lockerMapper);
        assertThatThrownBy(() -> service.listLockers(1, 10, null, "BROKEN"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("快递柜状态筛选条件不合法");
    }
}
