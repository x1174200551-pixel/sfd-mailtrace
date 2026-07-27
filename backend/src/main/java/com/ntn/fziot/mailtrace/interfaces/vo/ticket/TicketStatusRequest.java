package com.ntn.fziot.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工单状态变更请求")
public record TicketStatusRequest(
        @Schema(description = "目标状态：PROCESSING/WAITING_CUSTOMER/CLOSED/CANCELLED") String status,
        @Schema(description = "状态变更说明") String reason
) {
}
