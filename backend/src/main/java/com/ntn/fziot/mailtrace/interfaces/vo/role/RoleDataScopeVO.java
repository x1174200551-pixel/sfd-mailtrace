package com.ntn.fziot.mailtrace.interfaces.vo.role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "角色默认数据范围")
public record RoleDataScopeVO(
        @Schema(description = "资源类型") String resourceType,
        @Schema(description = "范围编码") String scopeCode,
        @Schema(description = "范围说明") String scopeDesc
) {
}
