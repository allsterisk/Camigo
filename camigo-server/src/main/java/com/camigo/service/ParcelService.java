package com.camigo.service;

import com.camigo.common.PageResult;
import com.camigo.dto.ParcelCreateDTO;
import com.camigo.dto.ParcelStatusUpdateDTO;
import com.camigo.vo.ParcelVO;

/**
 * 快递新增、查询、状态修改和删除服务。
 */
public interface ParcelService {

    ParcelVO create(Long userId, ParcelCreateDTO request);

    PageResult<ParcelVO> listAll(Long userId, long page, long pageSize,
                                 String status, String trackingNo);

    PageResult<ParcelVO> listMy(Long userId, long page, long pageSize,
                                String status, String trackingNo);

    ParcelVO getById(Long userId, Long parcelId);

    ParcelVO updateStatus(Long userId, Long parcelId, ParcelStatusUpdateDTO request);

    void delete(Long userId, Long parcelId);
}
