package com.camigo.service;

import com.camigo.dto.EvaluationCreateDTO;
import com.camigo.vo.EvaluationVO;

/**
 * 用户配送评价服务。
 */
public interface EvaluationService {

    EvaluationVO create(Long userId, EvaluationCreateDTO request);
}
