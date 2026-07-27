package com.ntn.fziot.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工单优先级变更请求")
public record TicketPriorityRequest(
        @Schema(description = "目标优先级：LOW/NORMAL/HIGH/URGENT") String priority,
        @Schema(description = "优先级变更说明") String reason
) {
}
