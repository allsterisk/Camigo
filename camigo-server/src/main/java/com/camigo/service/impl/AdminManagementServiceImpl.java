package com.camigo.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.camigo.common.PageResult;
import com.camigo.exception.BusinessException;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.LockerMapper;
import com.camigo.service.AdminManagementService;
import com.camigo.vo.AdminCourierVO;
import com.camigo.vo.AdminLockerVO;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 管理后台查询实现。 */
@Service
public class AdminManagementServiceImpl implements AdminManagementService {

    private static final Set<String> EMPLOYMENT_STATUSES = Set.of("ACTIVE", "SUSPENDED", "LEFT");
    private static final Set<String> LOCKER_STATUSES = Set.of("ONLINE", "OFFLINE", "MAINTENANCE", "DISABLED");
    private final CourierMapper courierMapper;
    private final LockerMapper lockerMapper;

    public AdminManagementServiceImpl(CourierMapper courierMapper, LockerMapper lockerMapper) {
        this.courierMapper = courierMapper;
        this.lockerMapper = lockerMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminCourierVO> listCouriers(long page, long pageSize, String keyword,
                                                   String employmentStatus) {
        String status = normalizeStatus(employmentStatus, EMPLOYMENT_STATUSES, "配送员状态");
        Page<AdminCourierVO> result = courierMapper.selectAdminPage(
                new Page<>(page, pageSize), trimToNull(keyword), status);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminLockerVO> listLockers(long page, long pageSize, String keyword,
                                                 String operationStatus) {
        String status = normalizeStatus(operationStatus, LOCKER_STATUSES, "快递柜状态");
        Page<AdminLockerVO> result = lockerMapper.selectAdminPage(
                new Page<>(page, pageSize), trimToNull(keyword), status);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    private String normalizeStatus(String value, Set<String> allowed, String label) {
        if (!StringUtils.hasText(value)) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BusinessException(400, label + "筛选条件不合法", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
