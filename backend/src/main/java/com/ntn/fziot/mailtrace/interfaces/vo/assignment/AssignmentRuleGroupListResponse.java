package com.ntn.fziot.mailtrace.interfaces.vo.assignment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分配规则组列表响应")
public record AssignmentRuleGroupListResponse(
        @Schema(description = "规则组列表") List<AssignmentRuleGroupVO> records,
        @Schema(description = "规则组总数") long totalCount,
        @Schema(description = "启用规则组数") long enabledCount,
        @Schema(description = "停用规则组数") long disabledCount
) {
}
