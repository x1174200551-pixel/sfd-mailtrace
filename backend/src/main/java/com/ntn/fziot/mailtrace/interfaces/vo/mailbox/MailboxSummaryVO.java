package com.ntn.fziot.mailtrace.interfaces.vo.mailbox;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "邮箱统计摘要")
public record MailboxSummaryVO(
        @Schema(description = "邮箱总数") long totalMailboxes,
        @Schema(description = "启用邮箱数") long enabledMailboxes,
        @Schema(description = "停用邮箱数") long disabledMailboxes,
        @Schema(description = "连接正常邮箱数") long okMailboxes,
        @Schema(description = "连接异常邮箱数") long errorMailboxes,
        @Schema(description = "连接未知邮箱数") long unknownMailboxes,
        @Schema(description = "今日拉取邮件总数") long todayReceivedMailCount,
        @Schema(description = "今日自动新建工单总数") long todayCreatedTicketCount
) {
}
