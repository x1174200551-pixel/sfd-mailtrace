package com.ntn.fziot.mailtrace.interfaces.vo.sla;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "SLA 策略列表响应")
public record SlaPolicyListResponse(
        @Schema(description = "策略列表") List<SlaPolicyVO> records,
        @Schema(description = "统计摘要") SlaPolicySummaryVO summary
) {
}
