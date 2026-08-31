package com.camigo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 配送员拒绝派单邀请的原因。 */
@Schema(description = "拒绝派单请求")
public record DispatchRejectDTO(
        @NotBlank(message = "拒绝原因不能为空")
        @Pattern(regexp = "TOO_FAR|TEMPORARILY_UNAVAILABLE|VEHICLE_ISSUE|OTHER",
                message = "拒绝原因编码不受支持")
        String reasonCode,

        @Size(max = 255, message = "拒绝原因详情不能超过255个字符")
        String reasonDetail) {
}
