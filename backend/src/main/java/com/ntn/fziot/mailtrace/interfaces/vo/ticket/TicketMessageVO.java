package com.ntn.fziot.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "工单邮件消息")
public record TicketMessageVO(
        @Schema(description = "消息ID") Long id,
        @Schema(description = "方向：INBOUND客户来信/OUTBOUND外发/INTERNAL内部备注") String direction,
        @Schema(description = "发件人地址") String fromAddress,
        @Schema(description = "收件人地址") String toAddress,
        @Schema(description = "原始收件人列表") String toAddresses,
        @Schema(description = "原始抄送列表") String ccAddresses,
        @Schema(description = "原始密送列表") String bccAddresses,
        @Schema(description = "主题") String subject,
        @Schema(description = "纯文本正文") String contentText,
        @Schema(description = "HTML正文") String contentHtml,
        @Schema(description = "原始邮件头") String rawHeaders,
        @Schema(description = "原始EML对象键") String rawEmlObjectKey,
        @Schema(description = "原始EML大小") Long rawEmlSize,
        @Schema(description = "邮件原始发送时间") LocalDateTime sentAt,
        @Schema(description = "操作人名称") String operatorName,
        @Schema(description = "创建时间") LocalDateTime createdAt
) {
}
