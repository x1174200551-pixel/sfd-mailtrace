package com.ntn.fziot.mailtrace.interfaces.vo.department;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DepartmentEnabledRequest {

    @NotNull(message = "请选择启停状态")
    @Schema(description = "是否启用")
    private Boolean enabled;
}
