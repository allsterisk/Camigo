package com.camigo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 新增快递请求。
 */
@Schema(description = "管理员录入快递请求")
public record ParcelCreateDTO(
        @Schema(description = "快递单号", example = "SF202608030001")
        @NotBlank(message = "快递单号不能为空")
        @Size(max = 64, message = "快递单号不能超过64位")
        @Pattern(regexp = "^[A-Za-z0-9-]{6,64}$", message = "快递单号格式不正确")
        String trackingNo,

        @Schema(description = "承运商编码", example = "SF")
        @NotBlank(message = "承运商编码不能为空")
        @Size(max = 32, message = "承运商编码不能超过32位")
        @Pattern(regexp = "^[A-Za-z0-9_-]{2,32}$", message = "承运商编码格式不正确")
        String carrierCode,

        @Schema(description = "寄件人姓名", example = "李四")
        @NotBlank(message = "寄件人姓名不能为空")
        @Size(max = 50, message = "寄件人姓名不能超过50位")
        String sender,

        @Schema(description = "收件人姓名，用于与手机号对应用户二次核验", example = "张三")
        @NotBlank(message = "收件人姓名不能为空")
        @Size(max = 50, message = "收件人姓名不能超过50位")
        String receiver,

        @Schema(description = "收件人手机号", example = "13800138000")
        @NotBlank(message = "收件人手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @Schema(description = "目标快递柜编号", example = "1")
        @NotNull(message = "目标快递柜不能为空")
        @Positive(message = "目标快递柜编号必须为正数")
        Long lockerId,

        @Schema(description = "包裹尺寸", example = "M")
        @NotBlank(message = "包裹尺寸不能为空")
        @Pattern(regexp = "S|M|L|XL", message = "包裹尺寸只能为S、M、L或XL")
        String sizeClass,

        @Schema(description = "包裹重量（千克）", example = "1.25", nullable = true)
        @DecimalMin(value = "0.0", message = "包裹重量不能为负数")
        BigDecimal weightKg) {
}
