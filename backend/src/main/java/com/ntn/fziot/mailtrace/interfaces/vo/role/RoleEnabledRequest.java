package com.ntn.fziot.mailtrace.interfaces.vo.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "角色启停请求")
public class RoleEnabledRequest {

    @NotNull(message = "请选择启用状态")
    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;
}
