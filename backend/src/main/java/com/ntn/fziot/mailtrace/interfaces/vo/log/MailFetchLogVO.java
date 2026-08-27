package com.ntn.fziot.mailtrace.interfaces.vo.log;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "邮件拉取日志")
public record MailFetchLogVO(
        @Schema(description = "日志ID") Long id,
        @Schema(description = "企业ID") Long enterpriseId,
        @Schema(description = "邮箱ID") Long mailboxId,
        @Schema(description = "邮箱名称") String mailboxName,
        @Schema(description = "邮箱地址") String emailAddress,
        @Schema(description = "触发方式：SCHEDULED/MANUAL") String triggerType,
        @Schema(description = "开始时间") LocalDateTime startedAt,
        @Schema(description = "结束时间") LocalDateTime finishedAt,
        @Schema(description = "是否成功") Boolean success,
        @Schema(description = "拉取邮件数") Integer fetchedCount,
        @Schema(description = "新建工单数") Integer createdTicketCount,
        @Schema(description = "关联工单数") Integer linkedCount,
        @Schema(description = "错误信息") String errorMessage,
        @Schema(description = "记录时间") LocalDateTime createdAt
) {
}
