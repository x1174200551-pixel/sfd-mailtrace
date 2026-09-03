package com.ntn.fziot.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工单回复邮件预览请求")
public record TicketReplyPreviewRequest(
        @Schema(description = "本次回复选用的处理人回复模板ID；为空时使用邮箱默认模板") Long replyTemplateId,
        @Schema(description = "回复内容（纯文本）") String content,
        @Schema(description = "回复内容（HTML 富文本）") String htmlContent
) {
}
