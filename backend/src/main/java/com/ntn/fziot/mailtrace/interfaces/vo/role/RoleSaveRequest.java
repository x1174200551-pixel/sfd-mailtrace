package com.ntn.fziot.mailtrace.interfaces.vo.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "角色保存请求")
public class RoleSaveRequest {

    @Pattern(regexp = "^$|^[A-Za-z][A-Za-z0-9_]{1,63}$", message = "角色编码仅支持 2-64 位字母、数字或下划线")
    @Schema(description = "角色编码；为空时系统自动生成", example = "QUALITY_CHECKER")
    private String roleCode;

    @Size(max = 64, message = "角色名称不能超过 64 个字符")
    @Schema(description = "角色名称", example = "工单质检")
    private String roleName;

    @Size(max = 512, message = "角色说明不能超过 512 个字符")
    @Schema(description = "角色说明", example = "查看工单、客户和附件，用于服务质量抽查")
    private String roleDesc;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled = true;
}
