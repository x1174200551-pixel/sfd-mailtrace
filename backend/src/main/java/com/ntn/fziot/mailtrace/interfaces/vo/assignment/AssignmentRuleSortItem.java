package com.ntn.fziot.mailtrace.interfaces.vo.assignment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "分配规则排序项")
public record AssignmentRuleSortItem(
        @NotNull(message = "规则ID不能为空")
        @Schema(description = "规则ID") Long id,

        @NotNull(message = "优先级不能为空")
        @Min(value = 1, message = "优先级需大于 0")
        @Max(value = 9999, message = "优先级不能超过 9999")
        @Schema(description = "匹配优先级，数字越小越优先") Integer priorityOrder
) {
}
