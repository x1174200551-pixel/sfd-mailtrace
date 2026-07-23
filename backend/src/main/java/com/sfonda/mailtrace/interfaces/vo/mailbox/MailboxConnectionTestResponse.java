package com.sfonda.mailtrace.interfaces.vo.mailbox;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "邮箱连接测试响应")
public record MailboxConnectionTestResponse(
        @Schema(description = "整体是否成功") boolean success,
        @Schema(description = "连接状态：OK/ERROR") String connectionStatus,
        @Schema(description = "IMAP是否成功") boolean imapSuccess,
        @Schema(description = "IMAP结果说明") String imapMessage,
        @Schema(description = "SMTP是否成功") boolean smtpSuccess,
        @Schema(description = "SMTP结果说明") String smtpMessage,
        @Schema(description = "测试时间") LocalDateTime testedAt
) {
}
