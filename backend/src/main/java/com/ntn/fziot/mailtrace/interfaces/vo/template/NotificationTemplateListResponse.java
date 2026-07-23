package com.ntn.fziot.mailtrace.interfaces.vo.template;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "通知模板列表响应")
public record NotificationTemplateListResponse(
        @Schema(description = "模板列表") List<NotificationTemplateVO> records,
        @Schema(description = "统计") NotificationTemplateSummaryVO summary,
        @Schema(description = "可用变量") List<TemplateVariableVO> variables
) {
}
