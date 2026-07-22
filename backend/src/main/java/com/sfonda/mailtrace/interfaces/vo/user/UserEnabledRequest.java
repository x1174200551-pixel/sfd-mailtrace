package com.sfonda.mailtrace.interfaces.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "启停用户请求")
public class UserEnabledRequest {

    @NotNull(message = "请选择启用状态")
    @Schema(description = "是否启用", example = "false")
    private Boolean enabled;
}
