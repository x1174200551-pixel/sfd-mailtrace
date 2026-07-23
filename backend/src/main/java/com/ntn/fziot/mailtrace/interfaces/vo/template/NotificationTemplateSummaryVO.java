package com.ntn.fziot.mailtrace.interfaces.vo.template;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "通知模板统计")
public record NotificationTemplateSummaryVO(
        @Schema(description = "模板总数") long totalTemplates,
        @Schema(description = "启用模板数") long enabledTemplates,
        @Schema(description = "停用模板数") long disabledTemplates,
        @Schema(description = "可用变量数") int availableVariables
) {
}
