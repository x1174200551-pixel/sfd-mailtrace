package com.ntn.fziot.mailtrace.interfaces.vo.assignment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分配规则列表响应")
public record AssignmentRuleListResponse(
        @Schema(description = "规则列表") List<AssignmentRuleVO> records,
        @Schema(description = "统计摘要") AssignmentRuleSummaryVO summary
) {
}
