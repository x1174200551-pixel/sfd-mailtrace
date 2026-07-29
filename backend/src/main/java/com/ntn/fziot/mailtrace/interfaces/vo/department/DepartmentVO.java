package com.ntn.fziot.mailtrace.interfaces.vo.department;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record DepartmentVO(
        @Schema(description = "部门ID") Long id,
        @Schema(description = "父部门ID") Long parentId,
        @Schema(description = "部门编码") String deptCode,
        @Schema(description = "部门名称") String deptName,
        @Schema(description = "部门说明") String deptDesc,
        @Schema(description = "负责人用户ID") Long leaderUserId,
        @Schema(description = "负责人名称") String leaderDisplayName,
        @Schema(description = "部门路径") String deptPath,
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "排序值") Integer sortOrder,
        @Schema(description = "直属成员数量") long memberCount,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt,
        @Schema(description = "子部门") List<DepartmentVO> children
) {
}
