package com.ntn.fziot.mailtrace.repox.mysql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mt_mailbox")
public class MailboxEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long enterpriseId;

    private String mailboxName;

    private String emailAddress;

    @TableField("is_enabled")
    private Boolean enabled;

    private Long defaultAssigneeId;

    private String imapHost;

    private Integer imapPort;

    private Boolean imapSslEnabled;

    private String imapUsername;

    private String imapPasswordEnc;

    private String imapFolder;

    private Integer fetchIntervalSec;

    private String smtpHost;

    private Integer smtpPort;

    private Boolean smtpSslEnabled;

    private String smtpUsername;

    private String smtpPasswordEnc;

    private String smtpFromName;

    private Boolean autoReplyEnabled;

    private Long autoReplyTemplateId;

    private Long assignmentNotifyTemplateId;

    private Long agentReplyTemplateId;

    private Long slaWarningTemplateId;

    private Long slaBreachTemplateId;

    private Long slaPolicyId;

    private Long assignmentRuleGroupId;

    private String assignmentFallbackType;

    private LocalDateTime lastFetchAt;

    private String connectionStatus;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
