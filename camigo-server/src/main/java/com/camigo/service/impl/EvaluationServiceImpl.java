package com.camigo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.camigo.common.ErrorCodes;
import com.camigo.dto.EvaluationCreateDTO;
import com.camigo.entity.Courier;
import com.camigo.entity.DeliveryOrder;
import com.camigo.entity.Evaluation;
import com.camigo.entity.ParcelEntity;
import com.camigo.exception.BusinessException;
import com.camigo.exception.ForbiddenException;
import com.camigo.exception.ResourceNotFoundException;
import com.camigo.mapper.CourierMapper;
import com.camigo.mapper.DeliveryOrderMapper;
import com.camigo.mapper.EvaluationMapper;
import com.camigo.mapper.ParcelMapper;
import com.camigo.service.EvaluationService;
import com.camigo.vo.EvaluationVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 评价服务实现。
 */
@Service
public class EvaluationServiceImpl implements EvaluationService {

    private static final String ROLE_ADMIN = "ADMIN";

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

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationVO> listByParcel(Long userId, Collection<String> roles, Long parcelId) {
        ParcelEntity parcel = parcelMapper.selectById(parcelId);
        if (parcel == null) {
            throw new ResourceNotFoundException("快递不存在");
        }
        if (!isAdmin(roles) && !userId.equals(parcel.getRecipientUserId())) {
            throw new ForbiddenException("只能查看本人快递的评价");
        }
        return evaluationMapper.selectVisibleByParcelId(parcelId).stream()
                .map(this::toVo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationVO getByOrder(Long userId, Collection<String> roles, Long orderId) {
        DeliveryOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("配送订单不存在");
        }
        if (!isAdmin(roles)) {
            Courier courier = courierMapper.selectOne(new LambdaQueryWrapper<Courier>()
                    .eq(Courier::getUserId, userId));
            if (courier == null || order.getAssignedCourierId() == null
                    || !courier.getId().equals(order.getAssignedCourierId())) {
                throw new ForbiddenException("只能查看本人配送订单的评价");
            }
        }
        Evaluation evaluation = evaluationMapper.selectVisibleByOrderId(orderId);
        return evaluation == null ? null : toVo(evaluation);
    }

    private boolean isAdmin(Collection<String> roles) {
        return roles != null && roles.contains(ROLE_ADMIN);
    }

    private EvaluationVO toVo(Evaluation evaluation) {
        return new EvaluationVO(evaluation.getId(), evaluation.getDeliveryOrderId(),
                evaluation.getUserId(), evaluation.getCourierId(), evaluation.getDeliveryScore(),
                evaluation.getServiceScore(), evaluation.getContent(), evaluation.getStatus(),
                evaluation.getEvaluatedTime());
    }
}
