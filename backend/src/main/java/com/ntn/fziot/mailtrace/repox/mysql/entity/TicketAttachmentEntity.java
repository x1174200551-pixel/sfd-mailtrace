package com.ntn.fziot.mailtrace.repox.mysql.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mt_ticket_attachment")
public class TicketAttachmentEntity {
    private Long id;
    private Long ticketId;
    private Long messageId;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String objectKey;
    private Boolean isInline;
    private String contentId;
    private String uploadedBy;
    private LocalDateTime createdAt;
}
