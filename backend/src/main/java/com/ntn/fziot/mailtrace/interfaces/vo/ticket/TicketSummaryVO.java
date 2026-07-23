package com.ntn.fziot.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "工单列表项")
public record TicketSummaryVO(
        @Schema(description = "工单ID") Long id,
        @Schema(description = "工单号") String ticketNo,
        @Schema(description = "主题") String subject,
        @Schema(description = "状态") String status,
        @Schema(description = "优先级") String priority,
        @Schema(description = "客户邮箱") String customerEmail,
        @Schema(description = "处理人ID") Long assigneeId,
        @Schema(description = "处理人名称") String assigneeName,
        @Schema(description = "来源邮箱ID") Long mailboxId,
        @Schema(description = "来源邮箱名称") String mailboxName,
        @Schema(description = "是否疑似断链") Boolean linkSuspect,
        @Schema(description = "是否已首次响应") Boolean hasReplied,
        @Schema(description = "首次响应SLA截止时间") LocalDateTime slaResponseDeadline,
        @Schema(description = "是否已SLA超时") Boolean slaBreached,
        @Schema(description = "创建时间") LocalDateTime createdAt
) {
}
