package com.ntn.fziot.mailtrace.interfaces.vo.role;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "角色配置详情")
public record RoleVO(
        @Schema(description = "角色ID") Long id,
        @Schema(description = "角色编码") String roleCode,
        @Schema(description = "角色名称") String roleName,
        @Schema(description = "角色说明") String roleDesc,
        @Schema(description = "是否系统内置角色") Boolean systemRole,
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "排序值") Integer sortOrder,
        @Schema(description = "权限编码清单") List<String> permissionCodes,
        @Schema(description = "默认数据范围") List<RoleDataScopeVO> dataScopes,
        @Schema(description = "关联用户数") Long userCount,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
