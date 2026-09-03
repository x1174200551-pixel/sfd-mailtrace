package com.ntn.fziot.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "工单生命周期事件")
public record TicketEventVO(
        @Schema(description = "事件ID") Long id,
        @Schema(description = "事件类型：CREATED/AUTO_REPLY/ASSIGNED/FIRST_REPLY/AGENT_REPLY/CUSTOMER_REPLY/CLOSED/CANCELLED/SLA_RESPONSE_WARNING/SLA_RESPONSE_BREACH/SLA_RESPONSE_ESCALATION/SLA_RESOLVE_WARNING/SLA_RESOLVE_BREACH/SLA_RESOLVE_ESCALATION等")
        String eventType,
        @Schema(description = "事件内容摘要") String eventContent,
        @Schema(description = "操作人") String operator,
        @Schema(description = "事件时间") LocalDateTime eventAt
) {
}
