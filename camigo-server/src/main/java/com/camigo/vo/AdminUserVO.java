package com.camigo.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** 管理员用户列表视图。 */
public record AdminUserVO(
        Long id, String username, String realName, String phone, String studentNo,
        String status, @JsonIgnore String roleCodes, LocalDateTime lastLoginTime,
        LocalDateTime createTime, LocalDateTime updateTime) {

    @JsonProperty("roles")
    public List<String> roles() {
        if (roleCodes == null || roleCodes.isBlank()) {
            return List.of();
        }
        return Arrays.stream(roleCodes.split(",")).distinct().sorted().toList();
    }
}
