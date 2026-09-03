package com.ntn.fziot.mailtrace.application.bizservice.mailsend;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailThreadHeadersTest {

    @Test
    void forReply_shouldNormalizeDeduplicateAndAppendParent() {
        MailThreadHeaders headers = MailThreadHeaders.forReply(
                "<parent@example.com>",
                "<root@example.com> <parent@example.com> <root@example.com>",
                "support@example.com");

        assertEquals("parent@example.com", headers.inReplyTo());
        assertEquals("<root@example.com> <parent@example.com>", headers.references());
        assertEquals("support@example.com", headers.replyToAddress());
    }

    @Test
    void buildReferences_shouldTrimOldestAndAlwaysKeepDirectParent() {
        String longReferences = IntStream.range(0, 80)
                .mapToObj(index -> "<message-" + index + "-with-long-id@example.com>")
                .reduce((left, right) -> left + " " + right)
                .orElseThrow();

        String references = MailThreadHeaders.buildReferences(longReferences, "parent@example.com");

        assertTrue(references.length() <= MailThreadHeaders.MAX_REFERENCES_LENGTH);
        assertTrue(references.endsWith("<parent@example.com>"));
        assertFalse(references.contains("<message-0-with-long-id@example.com>"));
    }

    @Test
    void normalizeMessageId_shouldRejectHeaderInjection() {
        assertNull(MailThreadHeaders.normalizeMessageId("safe@example.com\r\nBcc: attacker@example.com"));
        assertTrue(MailThreadHeaders.parseMessageIds("<safe@example.com>\nBcc: attacker@example.com").isEmpty());
    }

    @Test
    void buildReplySubject_shouldAvoidRepeatedPrefixes() {
        assertEquals("Re: 订单咨询", MailThreadHeaders.buildReplySubject("Re: RE： 回复: 订单咨询"));
        assertEquals("Re: (无主题)", MailThreadHeaders.buildReplySubject("  "));
    }

    @Test
    void generateMessageId_shouldUseSenderDomain() {
        String messageId = MailThreadHeaders.generateMessageId("support@example.com");
        assertTrue(messageId.endsWith("@example.com"));
        assertEquals("<" + messageId + ">", MailThreadHeaders.toHeaderValue(messageId));
    }
}
