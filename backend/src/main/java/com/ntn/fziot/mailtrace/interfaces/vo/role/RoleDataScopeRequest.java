package com.ntn.fziot.mailtrace.interfaces.vo.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "角色数据范围保存项")
public class RoleDataScopeRequest {

    @NotBlank(message = "请选择资源类型")
    @Schema(description = "资源类型：TICKET/CUSTOMER/DASHBOARD", example = "TICKET")
    private String resourceType;

    @NotBlank(message = "请选择数据范围")
    @Schema(description = "范围编码：ALL/SELF/DEPT/DEPT_AND_CHILDREN", example = "SELF")
    private String scopeCode;

    @Schema(description = "范围说明")
    private String scopeDesc;
}
