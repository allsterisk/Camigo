package com.camigo.service;

import com.camigo.dto.DeliveryOrderCreateDTO;
import com.camigo.dto.DeliveryOrderStatusDTO;
import com.camigo.vo.DeliveryOrderVO;

/**
 * 配送任务创建、接单与状态流转服务。
 */
public interface DeliveryOrderService {

    DeliveryOrderVO create(Long userId, DeliveryOrderCreateDTO request);

    DeliveryOrderVO accept(Long userId, Long orderId);

    DeliveryOrderVO updateStatus(Long userId, DeliveryOrderStatusDTO request);
}
