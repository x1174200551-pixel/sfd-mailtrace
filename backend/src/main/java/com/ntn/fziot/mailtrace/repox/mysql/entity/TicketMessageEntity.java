package com.ntn.fziot.mailtrace.repox.mysql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mt_ticket_message")
public class TicketMessageEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ticketId;

    private String direction;

    private String sendStatus;

    private LocalDateTime deliveryCompletedAt;

    private String messageId;

    private String inReplyTo;

    private String mailReferences;

    private String fromAddress;

    private String toAddress;

    private String toAddresses;

    private String ccAddresses;

    private String bccAddresses;

    private String subject;

    private String contentText;

    private String contentHtml;

    private String rawHeaders;

    private String rawEmlObjectKey;

    private Long rawEmlSize;

    private LocalDateTime sentAt;

    private Long operatorId;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
