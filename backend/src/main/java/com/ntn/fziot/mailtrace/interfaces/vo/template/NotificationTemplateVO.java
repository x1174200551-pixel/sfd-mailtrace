package com.ntn.fziot.mailtrace.interfaces.vo.template;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "通知模板")
public record NotificationTemplateVO(
        @Schema(description = "模板ID") Long id,
        @Schema(description = "模板编码") String templateCode,
        @Schema(description = "模板类型") String templateType,
        @Schema(description = "模板名称") String templateName,
        @Schema(description = "主题模板") String subjectTpl,
        @Schema(description = "正文模板") String contentTpl,
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
