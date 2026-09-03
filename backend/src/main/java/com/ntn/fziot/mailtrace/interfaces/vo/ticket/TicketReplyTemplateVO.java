package com.ntn.fziot.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工单可选处理人回复模板")
public record TicketReplyTemplateVO(
        @Schema(description = "模板ID") Long id,
        @Schema(description = "模板名称") String templateName,
        @Schema(description = "是否为当前邮箱默认模板") Boolean defaultTemplate
) {
}
