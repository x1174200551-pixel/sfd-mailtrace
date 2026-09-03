package com.ntn.fziot.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工单回复邮件最终预览")
public record TicketReplyPreviewVO(
        @Schema(description = "实际使用的模板ID") Long templateId,
        @Schema(description = "实际使用的模板名称") String templateName,
        @Schema(description = "模板来源：SELECTED/MAILBOX_DEFAULT") String templateSource,
        @Schema(description = "发件人") String fromAddress,
        @Schema(description = "收件人") String toAddress,
        @Schema(description = "最终邮件主题") String subject,
        @Schema(description = "最终纯文本正文") String contentText,
        @Schema(description = "最终HTML正文；纯文本邮件为空") String contentHtml,
        @Schema(description = "最终内容类型") String contentType
) {
}
