package com.ntn.fziot.mailtrace.interfaces.vo.role;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "权限树节点")
public record PermissionTreeNodeVO(
        @Schema(description = "权限ID") Long id,
        @Schema(description = "权限编码") String permissionCode,
        @Schema(description = "权限名称") String permissionName,
        @Schema(description = "权限类型") String permissionType,
        @Schema(description = "模块编码") String moduleCode,
        @Schema(description = "父权限ID") Long parentId,
        @Schema(description = "子权限") List<PermissionTreeNodeVO> children
) {
}
