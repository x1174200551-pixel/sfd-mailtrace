package com.ntn.fziot.mailtrace.interfaces.vo.sla;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "SLA 默认策略请求")
public class SlaPolicyDefaultRequest {

    @NotNull(message = "请选择默认状态")
    @Schema(description = "是否设为默认策略", example = "true")
    private Boolean defaultPolicy;
}
