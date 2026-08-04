package com.camigo.service;

import com.camigo.common.PageResult;
import com.camigo.vo.AdminCourierVO;
import com.camigo.vo.AdminLockerVO;

/** 管理后台只读业务查询。 */
public interface AdminManagementService {
    PageResult<AdminCourierVO> listCouriers(long page, long pageSize, String keyword,
                                            String employmentStatus);
    PageResult<AdminLockerVO> listLockers(long page, long pageSize, String keyword,
                                          String operationStatus);
}
