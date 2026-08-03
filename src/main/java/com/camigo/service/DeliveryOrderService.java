package com.camigo.service;

import com.camigo.common.PageResult;
import com.camigo.dto.DeliveryOrderCreateDTO;
import com.camigo.dto.DeliveryOrderStatusDTO;
import com.camigo.vo.DeliveryOrderVO;

/**
 * 配送任务创建、查询、接单与状态流转服务。
 */
public interface DeliveryOrderService {

    DeliveryOrderVO create(Long userId, DeliveryOrderCreateDTO request);

    PageResult<DeliveryOrderVO> listTasks(Long userId, long page, long pageSize, String status);

    DeliveryOrderVO accept(Long userId, Long orderId);

    DeliveryOrderVO updateStatus(Long userId, Long orderId, DeliveryOrderStatusDTO request);
}
