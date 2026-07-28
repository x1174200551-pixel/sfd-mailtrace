package com.ntn.fziot.mailtrace.interfaces.vo.sla;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "SLA 策略启停请求")
public class SlaPolicyEnabledRequest {

    @NotNull(message = "请选择启停状态")
    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;
}
