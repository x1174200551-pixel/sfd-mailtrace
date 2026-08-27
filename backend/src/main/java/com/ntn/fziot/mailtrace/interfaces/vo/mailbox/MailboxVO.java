package com.ntn.fziot.mailtrace.interfaces.vo.mailbox;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "邮箱配置详情")
public record MailboxVO(
        @Schema(description = "邮箱ID") Long id,
        @Schema(description = "所属企业ID") Long enterpriseId,
        @Schema(description = "所属企业名称") String enterpriseName,
        @Schema(description = "邮箱名称") String mailboxName,
        @Schema(description = "邮箱地址") String emailAddress,
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "默认处理人ID") Long defaultAssigneeId,
        @Schema(description = "默认处理人名称") String defaultAssigneeName,
        @Schema(description = "IMAP主机") String imapHost,
        @Schema(description = "IMAP端口") Integer imapPort,
        @Schema(description = "IMAP是否启用SSL") Boolean imapSslEnabled,
        @Schema(description = "IMAP账号") String imapUsername,
        @Schema(description = "收件文件夹") String imapFolder,
        @Schema(description = "拉取间隔秒数") Integer fetchIntervalSec,
        @Schema(description = "SMTP主机") String smtpHost,
        @Schema(description = "SMTP端口") Integer smtpPort,
        @Schema(description = "SMTP是否启用SSL/TLS") Boolean smtpSslEnabled,
        @Schema(description = "SMTP账号") String smtpUsername,
        @Schema(description = "发件人显示名") String smtpFromName,
        @Schema(description = "是否启用自动回执") Boolean autoReplyEnabled,
        @Schema(description = "自动回执模板ID") Long autoReplyTemplateId,
        @Schema(description = "分配通知模板ID") Long assignmentNotifyTemplateId,
        @Schema(description = "处理人回复模板ID") Long agentReplyTemplateId,
        @Schema(description = "SLA 预警模板ID") Long slaWarningTemplateId,
        @Schema(description = "SLA 超时模板ID") Long slaBreachTemplateId,
        @Schema(description = "绑定的 SLA 策略ID") Long slaPolicyId,
        @Schema(description = "绑定的分配规则组ID") Long assignmentRuleGroupId,
        @Schema(description = "规则未命中处理方式：NONE/DEFAULT_ASSIGNEE") String assignmentFallbackType,
        @Schema(description = "最近成功拉取时间") LocalDateTime lastFetchAt,
        @Schema(description = "连接状态：UNKNOWN/OK/ERROR") String connectionStatus,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
