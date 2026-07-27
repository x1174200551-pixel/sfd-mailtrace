package com.ntn.fziot.mailtrace.interfaces.vo.assignment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "分配规则统计摘要")
public record AssignmentRuleSummaryVO(
        @Schema(description = "规则总数") long totalCount,
        @Schema(description = "启用规则数") long enabledCount,
        @Schema(description = "停用规则数") long disabledCount,
        @Schema(description = "默认规则数") long defaultCount
) {
}
