package com.ntn.fziot.mailtrace.application.bizservice.mailsend;

/**
 * 外发邮件不可变快照。线程化邮件必须在首次发送前确定全部字段，重试不得重新渲染。
 */
public record OutboundMailRequest(
        Long mailboxId,
        Long ticketId,
        Long ticketMessageId,
        Long templateId,
        String templateType,
        String sendType,
        String toAddress,
        String subject,
        String content,
        String contentType,
        String messageId,
        String inReplyTo,
        String references,
        String replyToAddress
) {
}
