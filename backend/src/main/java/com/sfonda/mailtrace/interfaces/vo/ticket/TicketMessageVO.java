package com.sfonda.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "工单邮件消息")
public record TicketMessageVO(
        @Schema(description = "消息ID") Long id,
        @Schema(description = "方向：INBOUND客户来信/OUTBOUND外发/INTERNAL内部备注") String direction,
        @Schema(description = "发件人地址") String fromAddress,
        @Schema(description = "收件人地址") String toAddress,
        @Schema(description = "主题") String subject,
        @Schema(description = "纯文本正文") String contentText,
        @Schema(description = "邮件原始发送时间") LocalDateTime sentAt,
        @Schema(description = "操作人名称") String operatorName,
        @Schema(description = "创建时间") LocalDateTime createdAt
) {
}
