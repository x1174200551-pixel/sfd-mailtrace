package com.ntn.fziot.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工单回复/备注请求")
public record TicketReplyRequest(
        @Schema(description = "回复内容（纯文本）") String content,
        @Schema(description = "是否为内部备注（true=内部备注，false=对外回复客户）") Boolean internal
) {
}
