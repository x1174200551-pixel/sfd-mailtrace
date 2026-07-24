package com.ntn.fziot.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "工单详情")
public record TicketVO(
        @Schema(description = "工单ID") Long id,
        @Schema(description = "工单号") String ticketNo,
        @Schema(description = "主题") String subject,
        @Schema(description = "状态：PENDING_ASSIGN/PROCESSING/WAITING_CUSTOMER/CLOSED/CANCELLED") String status,
        @Schema(description = "优先级：LOW/NORMAL/HIGH/URGENT") String priority,
        @Schema(description = "来源邮箱ID") Long mailboxId,
        @Schema(description = "来源邮箱名称") String mailboxName,
        @Schema(description = "客户ID") Long customerId,
        @Schema(description = "客户邮箱") String customerEmail,
        @Schema(description = "处理人ID") Long assigneeId,
        @Schema(description = "处理人名称") String assigneeName,
        @Schema(description = "是否疑似断链") Boolean linkSuspect,
        @Schema(description = "首次响应时间") LocalDateTime firstReplyAt,
        @Schema(description = "关闭时间") LocalDateTime closedAt,
        @Schema(description = "首次响应SLA截止时间") LocalDateTime slaResponseDeadline,
        @Schema(description = "解决SLA截止时间") LocalDateTime slaResolveDeadline,
        @Schema(description = "是否已SLA超时") Boolean slaBreached,
        @Schema(description = "客户最近来信时间") LocalDateTime lastCustomerMailAt,
        @Schema(description = "处理人最近回复时间") LocalDateTime lastAgentReplyAt,
        @Schema(description = "备注") String remark,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt,
        @Schema(description = "邮件消息列表") List<TicketMessageVO> messages,
        @Schema(description = "生命周期事件列表") List<TicketEventVO> events
) {
}
