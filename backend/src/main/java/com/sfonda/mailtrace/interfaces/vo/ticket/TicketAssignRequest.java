package com.sfonda.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工单分配请求")
public record TicketAssignRequest(
        @Schema(description = "处理人用户ID") Long assigneeId
) {
}
