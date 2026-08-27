package com.ntn.fziot.mailtrace.interfaces.vo.log;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "邮件发送日志")
public record MailSendLogVO(
        @Schema(description = "日志ID") Long id,
        @Schema(description = "关联工单ID") Long ticketId,
        @Schema(description = "企业ID") Long enterpriseId,
        @Schema(description = "发件邮箱ID") Long mailboxId,
        @Schema(description = "发送类型") String sendType,
        @Schema(description = "模板ID") Long templateId,
        @Schema(description = "模板类型") String templateType,
        @Schema(description = "收件人") String toAddress,
        @Schema(description = "邮件主题") String subject,
        @Schema(description = "邮件正文") String contentBody,
        @Schema(description = "发送状态：PENDING/SUCCESS/FAILED/RETRYING") String sendStatus,
        @Schema(description = "已重试次数") Integer retryCount,
        @Schema(description = "最大重试次数") Integer maxRetry,
        @Schema(description = "失败原因") String errorMessage,
        @Schema(description = "成功发送时间") LocalDateTime sentAt,
        @Schema(description = "创建时间") LocalDateTime createdAt
) {
}
