package com.ntn.fziot.mailtrace.interfaces.vo.assignment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "分配规则测试匹配结果")
public record AssignmentRuleMatchResponse(
        @Schema(description = "是否命中") Boolean matched,
        @Schema(description = "规则ID") Long ruleId,
        @Schema(description = "规则名称") String ruleName,
        @Schema(description = "匹配类型") String matchType,
        @Schema(description = "匹配值") String matchValue,
        @Schema(description = "分配目标处理人ID") Long assigneeId,
        @Schema(description = "分配目标处理人名称") String assigneeName,
        @Schema(description = "是否通知处理人") Boolean notifyEnabled
) {
    public static AssignmentRuleMatchResponse notMatched() {
        return new AssignmentRuleMatchResponse(false, null, null, null, null, null, null, null);
    }
}
