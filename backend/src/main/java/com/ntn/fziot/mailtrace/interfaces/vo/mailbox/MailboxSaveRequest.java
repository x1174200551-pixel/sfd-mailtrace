package com.ntn.fziot.mailtrace.interfaces.vo.mailbox;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "邮箱保存请求")
public class MailboxSaveRequest {

    @NotNull(message = "请选择所属企业")
    @Min(value = 1, message = "企业ID需大于 0")
    @Schema(description = "所属企业ID")
    private Long enterpriseId;

    @NotBlank(message = "请输入邮箱名称")
    @Size(max = 64, message = "邮箱名称不能超过 64 个字符")
    @Schema(description = "邮箱名称", example = "客服支持邮箱")
    private String mailboxName;

    @NotBlank(message = "请输入邮箱地址")
    @Email(message = "邮箱地址格式不正确")
    @Size(max = 128, message = "邮箱地址不能超过 128 个字符")
    @Schema(description = "邮箱地址", example = "support@example.com")
    private String emailAddress;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled = true;

    @Schema(description = "默认处理人ID")
    private Long defaultAssigneeId;

    @NotBlank(message = "请输入 IMAP 服务器")
    @Size(max = 128, message = "IMAP 服务器不能超过 128 个字符")
    @Schema(description = "IMAP服务器", example = "imap.example.com")
    private String imapHost;

    @Min(value = 1, message = "IMAP 端口需大于 0")
    @Max(value = 65535, message = "IMAP 端口不能超过 65535")
    @Schema(description = "IMAP端口", example = "993")
    private Integer imapPort = 993;

    @Schema(description = "IMAP是否启用SSL", example = "true")
    private Boolean imapSslEnabled = true;

    @NotBlank(message = "请输入 IMAP 账号")
    @Size(max = 128, message = "IMAP 账号不能超过 128 个字符")
    @Schema(description = "IMAP账号", example = "support@example.com")
    private String imapUsername;

    @Size(max = 128, message = "IMAP 密码或授权码不能超过 128 个字符")
    @Schema(description = "IMAP密码或授权码；编辑为空表示不修改")
    private String imapPassword;

    @NotBlank(message = "请输入收件文件夹")
    @Size(max = 64, message = "收件文件夹不能超过 64 个字符")
    @Schema(description = "收件文件夹", example = "INBOX")
    private String imapFolder = "INBOX";

    @Min(value = 60, message = "拉取频率不能低于 60 秒")
    @Max(value = 1800, message = "拉取频率不能超过 1800 秒")
    @Schema(description = "拉取间隔秒数", example = "120")
    private Integer fetchIntervalSec = 120;

    @NotBlank(message = "请输入 SMTP 服务器")
    @Size(max = 128, message = "SMTP 服务器不能超过 128 个字符")
    @Schema(description = "SMTP服务器", example = "smtp.example.com")
    private String smtpHost;

    @Min(value = 1, message = "SMTP 端口需大于 0")
    @Max(value = 65535, message = "SMTP 端口不能超过 65535")
    @Schema(description = "SMTP端口", example = "587")
    private Integer smtpPort = 587;

    @Schema(description = "SMTP是否启用SSL/TLS", example = "true")
    private Boolean smtpSslEnabled = true;

    @NotBlank(message = "请输入 SMTP 账号")
    @Size(max = 128, message = "SMTP 账号不能超过 128 个字符")
    @Schema(description = "SMTP账号", example = "support@example.com")
    private String smtpUsername;

    @Size(max = 128, message = "SMTP 密码或授权码不能超过 128 个字符")
    @Schema(description = "SMTP密码或授权码；编辑为空表示不修改")
    private String smtpPassword;

    @Size(max = 64, message = "发件人显示名不能超过 64 个字符")
    @Schema(description = "发件人显示名", example = "客服支持中心")
    private String smtpFromName;

    @Schema(description = "是否启用自动回执", example = "true")
    private Boolean autoReplyEnabled = false;

    @Schema(description = "自动回执模板ID")
    private Long autoReplyTemplateId;

    @Schema(description = "分配通知模板ID")
    private Long assignmentNotifyTemplateId;

    @Schema(description = "处理人回复模板ID")
    private Long agentReplyTemplateId;

    @Schema(description = "SLA 预警模板ID")
    private Long slaWarningTemplateId;

    @Schema(description = "SLA 超时模板ID")
    private Long slaBreachTemplateId;

    @Schema(description = "绑定的 SLA 策略ID")
    private Long slaPolicyId;

    @Schema(description = "绑定的分配规则组ID")
    private Long assignmentRuleGroupId;

    @Pattern(regexp = "NONE|DEFAULT_ASSIGNEE", message = "规则未命中处理方式仅支持 NONE 或 DEFAULT_ASSIGNEE")
    @Schema(description = "规则未命中处理方式：NONE/DEFAULT_ASSIGNEE", example = "NONE")
    private String assignmentFallbackType = "NONE";
}
