package com.ntn.fziot.mailtrace.interfaces.vo.template;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "模板变量")
public record TemplateVariableVO(
        @Schema(description = "变量键") String key,
        @Schema(description = "变量名称") String label,
        @Schema(description = "示例值") String sampleValue
) {
}
