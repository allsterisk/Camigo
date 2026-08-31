package com.camigo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminParcelGenerateDTO(
        @NotNull @Positive Long recipientUserId,
        @NotNull @Positive Long pickupLocationId,
        @NotNull @Positive Long destinationLocationId,
        @NotBlank @Pattern(regexp = "S|M|L|XL", message = "包裹尺寸只能为S、M、L或XL") String sizeClass,
        @NotNull @DecimalMin(value = "0.001", message = "包裹重量必须大于0")
        @DecimalMax(value = "99999.999", message = "包裹重量超出字段范围") BigDecimal weightKg,
        @PastOrPresent(message = "到达时间不能晚于当前时间") LocalDateTime arriveTime,
        @Min(value = 1, message = "至少生成1个包裹") @Max(value = 10, message = "单次最多生成10个包裹") int count) {
}
