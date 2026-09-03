package com.camigo.service;

import com.camigo.dto.EvaluationCreateDTO;
import com.camigo.vo.EvaluationVO;
import java.util.Collection;
import java.util.List;

/**
 * 用户配送评价服务。
 */
public interface EvaluationService {

    EvaluationVO create(Long userId, EvaluationCreateDTO request);

    /**
     * 查询某个快递关联配送订单的全部可见评价（按评价时间倒序，最新一条在前）。
     * 用户仅限本人快递，管理员可查询任意快递。
     *
     * @param userId    当前登录用户编号
     * @param roles     当前登录用户拥有的角色编码（由授权拦截器写入）
     * @param parcelId  快递编号
     */
    List<EvaluationVO> listByParcel(Long userId, Collection<String> roles, Long parcelId);

    /**
     * 查询某条配送订单的可见评价，未评价时返回 null。
     * 配送员仅限本人配送的订单，管理员可查询任意订单。
     *
     * @param userId   当前登录用户编号
     * @param roles    当前登录用户拥有的角色编码（由授权拦截器写入）
     * @param orderId  配送订单编号
     */
    EvaluationVO getByOrder(Long userId, Collection<String> roles, Long orderId);
}
