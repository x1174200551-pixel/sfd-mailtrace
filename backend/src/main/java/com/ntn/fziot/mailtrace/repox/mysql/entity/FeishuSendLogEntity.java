package com.ntn.fziot.mailtrace.repox.mysql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mt_feishu_send_log")
public class FeishuSendLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ticketEventId;
    private Long ticketId;
    private Long mailboxId;
    private Long enterpriseId;
    private Integer enterpriseConfigVersion;
    private Long assigneeUserId;
    private String sendType;
    private Long templateId;
    private String groupBotName;
    private String title;
    private String contentBody;
    private String cardContent;
    private String sendStatus;
    private Integer retryCount;
    private Integer maxRetry;
    private LocalDateTime nextRetryAt;
    private String responseCode;
    private String responseMessage;
    private LocalDateTime sentAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
