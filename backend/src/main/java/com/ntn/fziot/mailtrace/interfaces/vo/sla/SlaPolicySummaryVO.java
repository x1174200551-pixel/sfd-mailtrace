package com.ntn.fziot.mailtrace.interfaces.vo.sla;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SLA 策略统计摘要")
public record SlaPolicySummaryVO(
        @Schema(description = "总数") long totalCount,
        @Schema(description = "启用数") long enabledCount,
        @Schema(description = "停用数") long disabledCount,
        @Schema(description = "默认策略数") long defaultCount
) {
}
