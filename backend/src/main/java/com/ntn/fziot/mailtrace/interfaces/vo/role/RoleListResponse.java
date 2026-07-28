package com.ntn.fziot.mailtrace.interfaces.vo.role;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "角色列表响应")
public record RoleListResponse(
        @Schema(description = "角色列表") List<RoleVO> records,
        @Schema(description = "角色总数") Long total,
        @Schema(description = "启用角色数") Long enabledCount,
        @Schema(description = "内置角色数") Long systemCount,
        @Schema(description = "自定义角色数") Long customCount,
        @Schema(description = "权限项总数") Long permissionTotal,
        @Schema(description = "关联用户总数") Long userTotal
) {
}
