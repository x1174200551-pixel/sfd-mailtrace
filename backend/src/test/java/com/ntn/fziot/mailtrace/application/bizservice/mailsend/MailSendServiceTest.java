package com.ntn.fziot.mailtrace.application.bizservice.mailsend;

import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailSendServiceTest {

    private final MailSendService service = new MailSendService(
            null, null, null, null, null, null, null, null, null, null);

    @Test
    void buildMessage_shouldWriteFixedThreadHeadersReplyToAndHtmlContentType() throws Exception {
        MailboxEntity mailbox = new MailboxEntity();
        mailbox.setSmtpFromName("客服中心");
        OutboundMailRequest request = new OutboundMailRequest(
                11L, 100L, 200L, 30L, "AGENT_REPLY", "AGENT_REPLY",
                "customer@example.com", "Re: 咨询", "<p>回复内容</p>",
                MailSendService.CONTENT_TYPE_HTML, "fixed@example.com", "parent@example.com",
                "<root@example.com> <parent@example.com>", "support@example.com");

        MimeMessage message = service.buildMessage(mailbox, "support@example.com", request);

        assertEquals("<fixed@example.com>", message.getHeader("Message-ID", null));
        assertEquals("<parent@example.com>", message.getHeader("In-Reply-To", null));
        assertEquals("<root@example.com> <parent@example.com>", message.getHeader("References", null));
        assertEquals("support@example.com", message.getReplyTo()[0].toString());
        assertTrue(message.getContentType().toLowerCase().startsWith("text/html"));
    }

    @Test
    void detectContentType_shouldPreservePlainTextAndHtml() {
        assertEquals(MailSendService.CONTENT_TYPE_TEXT, MailSendService.detectContentType("普通文本"));
        assertEquals(MailSendService.CONTENT_TYPE_HTML, MailSendService.detectContentType("  <p>HTML</p>"));
    }

    @Test
    void buildMessage_shouldIncludeReplyAttachmentsInMixedMultipart() throws Exception {
        MailboxEntity mailbox = new MailboxEntity();
        mailbox.setSmtpFromName("客服中心");
        OutboundMailRequest request = new OutboundMailRequest(
                11L, 100L, 200L, 30L, "AGENT_REPLY", "AGENT_REPLY",
                "customer@example.com", "Re: 咨询", "回复内容",
                MailSendService.CONTENT_TYPE_TEXT, "fixed@example.com", "parent@example.com",
                "<parent@example.com>", "support@example.com");

        MimeMessage message = service.buildMessage(mailbox, "support@example.com", request, List.of(
                new MailSendService.MailAttachmentContent(
                        "测试附件.txt", "text/plain", "attachment-body".getBytes(StandardCharsets.UTF_8))));

        assertTrue(message.getContentType().toLowerCase().startsWith("multipart/mixed"));
        MimeMultipart multipart = (MimeMultipart) message.getContent();
        assertEquals(2, multipart.getCount());
        assertEquals("测试附件.txt", multipart.getBodyPart(1).getFileName());
        assertEquals("attachment-body", new String(
                multipart.getBodyPart(1).getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void buildRetrySnapshot_shouldReuseOriginalThreadHeadersMessageIdAndHtmlType() throws Exception {
        MailboxEntity mailbox = new MailboxEntity();
        mailbox.setId(11L);
        mailbox.setSmtpFromName("客服中心");
        mailbox.setSmtpUsername("support@example.com");
        MailSendLogEntity sendLog = new MailSendLogEntity();
        sendLog.setTicketId(100L);
        sendLog.setTicketMessageId(200L);
        sendLog.setTemplateId(30L);
        sendLog.setTemplateType("AGENT_REPLY");
        sendLog.setSendType("AGENT_REPLY");
        sendLog.setToAddress("customer@example.com");
        sendLog.setSubject("Re: 咨询");
        sendLog.setContentBody("<p>回复内容</p>");
        sendLog.setContentType(MailSendService.CONTENT_TYPE_HTML);
        sendLog.setMessageId("fixed@example.com");
        sendLog.setInReplyTo("parent@example.com");
        sendLog.setMailReferences("<root@example.com> <parent@example.com>");
        sendLog.setReplyToAddress("support@example.com");

        OutboundMailRequest retrySnapshot = service.buildRetrySnapshot(mailbox, sendLog);
        MimeMessage firstAttempt = service.buildMessage(mailbox, "support@example.com", retrySnapshot);
        MimeMessage retryAttempt = service.buildMessage(mailbox, "support@example.com", retrySnapshot);

        assertEquals("fixed@example.com", retrySnapshot.messageId());
        assertEquals(MailSendService.CONTENT_TYPE_HTML, retrySnapshot.contentType());
        assertEquals(firstAttempt.getHeader("Message-ID", null), retryAttempt.getHeader("Message-ID", null));
        assertEquals(firstAttempt.getHeader("In-Reply-To", null), retryAttempt.getHeader("In-Reply-To", null));
        assertEquals(firstAttempt.getHeader("References", null), retryAttempt.getHeader("References", null));
        assertEquals(firstAttempt.getContentType(), retryAttempt.getContentType());
    }
}
