package com.ntn.fziot.mailtrace.interfaces.vo.assignment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "分配规则启停请求")
public class AssignmentRuleEnabledRequest {

    @NotNull(message = "请选择启用状态")
    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;
}
