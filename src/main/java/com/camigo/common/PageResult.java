package com.camigo.common;

import java.util.List;

/**
 * 统一分页数据结构，避免向接口层暴露 MyBatis-Plus 分页对象。
 */
public record PageResult<T>(List<T> records, long total, long page, long pageSize) {

    public PageResult {
        records = records == null ? List.of() : List.copyOf(records);
        if (total < 0 || page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("分页参数不合法");
        }
    }
}
