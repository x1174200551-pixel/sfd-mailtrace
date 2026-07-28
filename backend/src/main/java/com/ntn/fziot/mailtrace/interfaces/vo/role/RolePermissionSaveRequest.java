package com.ntn.fziot.mailtrace.interfaces.vo.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "角色权限与默认数据范围保存请求")
public class RolePermissionSaveRequest {

    @NotEmpty(message = "请选择权限")
    @Schema(description = "权限编码清单")
    private List<String> permissionCodes = new ArrayList<>();

    @Valid
    @NotEmpty(message = "请选择默认数据范围")
    @Schema(description = "默认数据范围")
    private List<RoleDataScopeRequest> dataScopes = new ArrayList<>();
}
