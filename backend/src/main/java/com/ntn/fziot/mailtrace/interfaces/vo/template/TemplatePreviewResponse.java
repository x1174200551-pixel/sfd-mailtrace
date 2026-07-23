package com.ntn.fziot.mailtrace.interfaces.vo.template;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "模板预览响应")
public record TemplatePreviewResponse(
        @Schema(description = "渲染后主题") String subject,
        @Schema(description = "渲染后正文") String content
) {
}
