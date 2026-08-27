package com.ntn.fziot.mailtrace.repox.mysql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mt_mail_fetch_log")
public class MailFetchLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long mailboxId;

    private Long enterpriseId;

    private String triggerType;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @TableField("success")
    private Boolean success;

    private Integer fetchedCount;

    private Integer createdTicketCount;

    private Integer linkedCount;

    private String errorMessage;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
