package com.ntn.fziot.mailtrace.interfaces.vo.assignment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "分配规则详情")
public record AssignmentRuleVO(
        @Schema(description = "规则ID") Long id,
        @Schema(description = "规则名称") String ruleName,
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "匹配优先级") Integer priorityOrder,
        @Schema(description = "是否默认规则") Boolean defaultRule,
        @Schema(description = "匹配类型") String matchType,
        @Schema(description = "匹配值") String matchValue,
        @Schema(description = "分配目标处理人ID") Long assigneeId,
        @Schema(description = "分配目标处理人名称") String assigneeName,
        @Schema(description = "是否通知处理人") Boolean notifyEnabled,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
