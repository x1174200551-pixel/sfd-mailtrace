package com.ntn.fziot.mailtrace.repox.mysql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mt_mail_send_log")
public class MailSendLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ticketId;

    private Long mailboxId;

    private String sendType;

    private String toAddress;

    private String subject;

    private String sendStatus;

    private Integer retryCount;

    private Integer maxRetry;

    private LocalDateTime nextRetryAt;

    private String errorMessage;

    private LocalDateTime sentAt;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
