package com.ntn.fziot.mailtrace.interfaces.vo.department;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "组织架构统计")
public record DepartmentStatsVO(
        @Schema(description = "部门总数") long totalDepartments,
        @Schema(description = "启用部门数") long enabledDepartments,
        @Schema(description = "停用部门数") long disabledDepartments,
        @Schema(description = "负责人数量") long leaderCount,
        @Schema(description = "成员总数") long memberCount,
        @Schema(description = "未分配用户数") long unassignedUserCount
) {
}
