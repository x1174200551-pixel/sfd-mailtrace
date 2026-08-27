package com.ntn.fziot.mailtrace.interfaces.vo.assignment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "分配规则组详情")
public record AssignmentRuleGroupVO(
        @Schema(description = "规则组ID") Long id,
        @Schema(description = "所属企业ID") Long enterpriseId,
        @Schema(description = "规则组名称") String groupName,
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "备注") String remark,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
