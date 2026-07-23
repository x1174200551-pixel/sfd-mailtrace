package com.sfonda.mailtrace.repox.mysql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mt_ticket")
public class TicketEntity {

    // ---------- 主键 ----------
    @TableId(type = IdType.AUTO)
    private Long id;

    private String ticketNo;

    private String subject;

    private String status;

    private String priority;

    private Long mailboxId;

    private Long customerId;

    private String customerEmail;

    private Long assigneeId;

    @TableField("link_suspect")
    private Boolean linkSuspect;

    // ---------- SLA 埋点字段 ----------
    private LocalDateTime firstReplyAt;

    private LocalDateTime closedAt;

    private Long slaPolicyId;

    private LocalDateTime slaResponseDeadline;

    private LocalDateTime slaResolveDeadline;

    @TableField("sla_breached")
    private Boolean slaBreached;

    @TableField("sla_warning_sent")
    private Boolean slaWarningSent;

    @TableField("sla_breach_notified")
    private Boolean slaBreachNotified;

    private LocalDateTime lastCustomerMailAt;

    private LocalDateTime lastAgentReplyAt;

    // ---------- 审计字段 ----------
    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
