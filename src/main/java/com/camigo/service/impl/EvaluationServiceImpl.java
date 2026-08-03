package com.camigo.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.camigo.common.ErrorCodes;
import com.camigo.dto.EvaluationCreateDTO;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.Evaluation;
import com.camigo.entity.ParcelEntity;
import com.camigo.exception.BusinessException;
import com.camigo.exception.ResourceNotFoundException;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.EvaluationMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.service.EvaluationService;
import com.camigo.vo.EvaluationVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 评价服务实现。
 */
@Service
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationMapper evaluationMapper;
    private final DeliveryOrderMapper orderMapper;
    private final ParcelMapper parcelMapper;
    private final CourierMapper courierMapper;

    public EvaluationServiceImpl(EvaluationMapper evaluationMapper,
                                 DeliveryOrderMapper orderMapper,
                                 ParcelMapper parcelMapper,
                                 CourierMapper courierMapper) {
        this.evaluationMapper = evaluationMapper;
        this.orderMapper = orderMapper;
        this.parcelMapper = parcelMapper;
        this.courierMapper = courierMapper;
    }

    @Override
    @Transactional
    public EvaluationVO create(Long userId, EvaluationCreateDTO request) {
        DeliveryOrder order = orderMapper.selectById(request.deliveryOrderId());
        if (order == null) {
            throw new ResourceNotFoundException("配送订单不存在");
        }
        ParcelEntity parcel = parcelMapper.selectById(order.getParcelId());
        if (parcel == null || !userId.equals(parcel.getRecipientUserId())) {
            throw new ResourceNotFoundException("配送订单不存在");
        }
        if (!"COMPLETED".equals(order.getStatus()) || order.getAssignedCourierId() == null) {
            throw new BusinessException(ErrorCodes.EVALUATION_NOT_ALLOWED, "配送完成后才能评价");
        }

        Evaluation evaluation = new Evaluation();
        evaluation.setDeliveryOrderId(order.getId());
        evaluation.setUserId(userId);
        evaluation.setCourierId(order.getAssignedCourierId());
        evaluation.setDeliveryScore(request.deliveryScore());
        evaluation.setServiceScore(request.serviceScore());
        evaluation.setContent(StringUtils.hasText(request.content()) ? request.content().trim() : null);
        evaluation.setStatus("VISIBLE");
        evaluation.setEvaluatedTime(LocalDateTime.now());
        try {
            evaluationMapper.insert(evaluation);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCodes.EVALUATION_CONFLICT, "该配送订单已经评价");
        }

        BigDecimal average = evaluationMapper.selectCourierAverageRating(order.getAssignedCourierId());
        courierMapper.update(null, new LambdaUpdateWrapper<Courier>()
                .eq(Courier::getId, order.getAssignedCourierId())
                .set(Courier::getRatingAvg, average));
        return toVo(evaluation);
    }

    private EvaluationVO toVo(Evaluation evaluation) {
        return new EvaluationVO(evaluation.getId(), evaluation.getDeliveryOrderId(),
                evaluation.getUserId(), evaluation.getCourierId(), evaluation.getDeliveryScore(),
                evaluation.getServiceScore(), evaluation.getContent(), evaluation.getStatus(),
                evaluation.getEvaluatedTime());
    }
}
