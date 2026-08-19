package com.ntn.fziot.mailtrace.application.bizservice.ticket;

import com.ntn.fziot.mailtrace.infrastructure.mail.ParsedMail;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketMessageEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageThreadServiceTest {

    @Mock
    private TicketMessageMapper ticketMessageMapper;
    @Mock
    private TicketMapper ticketMapper;

    private MessageThreadService threadService;

    @BeforeEach
    void setUp() {
        threadService = new MessageThreadService(ticketMessageMapper, ticketMapper);
    }

    private ParsedMail createMail(String messageId, String inReplyTo, String references, String subject) {
        return new ParsedMail(messageId, inReplyTo, references,
                "from@test.com", "发件人", List.of("to@test.com"),
                List.of(), List.of(), subject, "正文", null, null, null,
                null, null, List.of(), 0);
    }

    // ========== 1. In-Reply-To 匹配 ==========

    @Test
    void normalizeMessageId_shouldStripAngleBracketsAndWhitespace() {
        assertEquals("reply@example.com", MessageThreadService.normalizeMessageId(" <reply@example.com> "));
        assertEquals("reply@example.com", MessageThreadService.normalizeMessageId("reply@example.com"));
        assertNull(MessageThreadService.normalizeMessageId(" "));
    }

    @Test
    void resolveByInReplyTo_shouldReturnTicketId() {
        ParsedMail mail = createMail("<new@test.com>", "<parent@test.com>", null, "回复：测试");
        TicketMessageEntity msg = new TicketMessageEntity();
        msg.setTicketId(100L);
        msg.setMessageId("parent@test.com");

        when(ticketMessageMapper.selectOne(any())).thenReturn(msg);

        assertEquals(100L, threadService.resolveTicketId(mail));
    }

    @Test
    void resolveByInReplyTo_withBrackets_shouldStripAndMatch() {
        ParsedMail mail = createMail("<new@test.com>", "<parent@test.com>", null, "回复");
        TicketMessageEntity msg = new TicketMessageEntity();
        msg.setTicketId(200L);
        msg.setMessageId("parent@test.com");

        when(ticketMessageMapper.selectOne(any())).thenReturn(msg);

        assertEquals(200L, threadService.resolveTicketId(mail));
    }

    // ========== 2. References 匹配 ==========

    @Test
    void resolveByReferences_shouldUseLastId() {
        ParsedMail mail = createMail("<new@test.com>", null,
                "<a@test.com> <b@test.com> <c@test.com>", "回复");

        TicketMessageEntity msg = new TicketMessageEntity();
        msg.setTicketId(300L);
        msg.setMessageId("c@test.com");

        when(ticketMessageMapper.selectOne(any())).thenReturn(msg);

        assertEquals(300L, threadService.resolveTicketId(mail));
    }

    // ========== 3. 主题工单号匹配 ==========

    @Test
    void resolveByTicketNoInSubject_shouldReturnTicketId() {
        ParsedMail mail = createMail("<new@test.com>", null, null,
                "回复：TCK-20260724-0001 工单问题");

        TicketEntity ticket = new TicketEntity();
        ticket.setId(400L);
        ticket.setTicketNo("TCK-20260724-0001");

        lenient().when(ticketMessageMapper.selectOne(any())).thenReturn(null);
        when(ticketMapper.selectOne(any())).thenReturn(ticket);

        assertEquals(400L, threadService.resolveTicketId(mail));
    }

    // ========== 4. 无匹配 ==========

    @Test
    void resolveNoMatch_shouldReturnNull() {
        ParsedMail mail = createMail("<new@test.com>", null, null, "全新主题");
        // 主题不含工单号，任何 mapper 都不会被调用
        assertNull(threadService.resolveTicketId(mail));
    }

    // ========== 5. InReplyTo null + References null → 回退到工单号 ==========

    @Test
    void resolveWithNullInReplyToAndReferences_shouldFallbackToTicketNo() {
        ParsedMail mail = createMail("<new@test.com>", null, null,
                "Re: TCK-20260724-0002 订单查询");

        TicketEntity ticket = new TicketEntity();
        ticket.setId(500L);
        ticket.setTicketNo("TCK-20260724-0002");

        lenient().when(ticketMessageMapper.selectOne(any())).thenReturn(null);
        when(ticketMapper.selectOne(any())).thenReturn(ticket);

        assertEquals(500L, threadService.resolveTicketId(mail));
    }

    @Test
    void resolveWithEmptyReferences_shouldReturnNull() {
        ParsedMail mail = createMail("<new@test.com>", null, "", "全新邮件");

        lenient().when(ticketMessageMapper.selectOne(any())).thenReturn(null);
        lenient().when(ticketMapper.selectOne(any())).thenReturn(null);

        assertNull(threadService.resolveTicketId(mail));
    }

    // ========== 6. In-Reply-To 优先于工单号 ==========

    @Test
    void inReplyToShouldTakePriorityOverTicketNo() {
        ParsedMail mail = createMail("<new@test.com>", "<existing@test.com>", null,
                "回复：TCK-20260724-0003 问题");

        TicketMessageEntity msg = new TicketMessageEntity();
        msg.setTicketId(600L);
        msg.setMessageId("existing@test.com");

        // 即使主题有工单号，也应该优先匹配 In-Reply-To
        when(ticketMessageMapper.selectOne(any())).thenReturn(msg);

        assertEquals(600L, threadService.resolveTicketId(mail));
    }
}
