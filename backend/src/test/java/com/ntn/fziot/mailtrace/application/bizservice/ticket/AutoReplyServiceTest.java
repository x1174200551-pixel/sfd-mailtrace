package com.ntn.fziot.mailtrace.application.bizservice.ticket;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.OutboundMailRequest;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.NotificationTemplateEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketMessageEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMessageMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class AutoReplyServiceTest {

    @Mock
    private NotificationTemplateMapper templateMapper;
    @Mock
    private MailboxMapper mailboxMapper;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private TicketMessageMapper ticketMessageMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MailSendService mailSendService;
    @Mock
    private CustomerTicketAccessService customerTicketAccessService;

    @InjectMocks
    private AutoReplyService autoReplyService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "AutoReplyServiceTest.TicketEntity");
        TableInfoHelper.initTableInfo(assistant, TicketEntity.class);
    }

    @Test
    void sendAutoReply_whenMailboxHasNoBoundTemplate_shouldSkipWithoutGlobalFallback() {
        when(ticketMapper.selectById(100L)).thenReturn(ticket());
        when(mailboxMapper.selectById(11L)).thenReturn(mailbox(null));

        AutoReplyService.AutoReplyResult result = autoReplyService.sendAutoReply(100L, 11L, "123456");

        assertFalse(result.success());
        assertTrue(result.message().contains("未绑定"));
        verify(templateMapper, never()).selectById(any());
        verify(mailSendService, never()).sendThreadedMail(any());
    }

    @Test
    void sendAutoReply_whenBoundTemplateValid_shouldSnapshotAndForwardSendMetadata() {
        TicketEntity ticket = ticket();
        when(ticketMapper.selectById(100L)).thenReturn(ticket);
        when(mailboxMapper.selectById(11L)).thenReturn(mailbox(30L));
        when(templateMapper.selectById(30L)).thenReturn(template(30L, 1L, "AUTO_REPLY", true));
        when(customerTicketAccessService.buildTicketUrl(ticket)).thenReturn("/customer/tickets/TCK-100");
        when(customerTicketAccessService.formatExpiresAt(ticket.getCustomerAccessExpiresAt())).thenReturn("2026-08-29 10:00");
        TicketMessageEntity parent = new TicketMessageEntity();
        parent.setId(200L);
        parent.setTicketId(100L);
        parent.setDirection(TicketBizService.DIRECTION_INBOUND);
        parent.setMessageId("customer-mail@example.com");
        parent.setMailReferences("<root@example.com>");
        parent.setSubject("咨询");
        when(ticketMessageMapper.selectById(200L)).thenReturn(parent);
        when(ticketMessageMapper.insert(any(TicketMessageEntity.class))).thenAnswer(invocation -> {
            TicketMessageEntity message = invocation.getArgument(0);
            message.setId(201L);
            return 1;
        });
        when(mailSendService.sendThreadedMail(any()))
                .thenReturn(MailSendService.SendResult.ok("发送成功", "reply@example.com"));

        AutoReplyService.AutoReplyResult result = autoReplyService.sendAutoReply(100L, 11L, "123456", 200L);

        assertTrue(result.success());
        assertEquals(30L, ticket.getAutoReplyTemplateId());
        verify(ticketMapper).update(eq(null), any());
        ArgumentCaptor<OutboundMailRequest> requestCaptor = ArgumentCaptor.forClass(OutboundMailRequest.class);
        verify(mailSendService).sendThreadedMail(requestCaptor.capture());
        assertEquals(201L, requestCaptor.getValue().ticketMessageId());
        assertEquals("customer-mail@example.com", requestCaptor.getValue().inReplyTo());
        assertEquals("<root@example.com> <customer-mail@example.com>", requestCaptor.getValue().references());
        assertEquals("Re: 咨询", requestCaptor.getValue().subject());
        assertEquals("support@example.com", requestCaptor.getValue().replyToAddress());
    }

    @Test
    void sendAutoReply_whenTemplateTypeDoesNotMatch_shouldSkip() {
        when(ticketMapper.selectById(100L)).thenReturn(ticket());
        when(mailboxMapper.selectById(11L)).thenReturn(mailbox(30L));
        when(templateMapper.selectById(30L)).thenReturn(template(30L, 2L, "SLA_WARNING", true));

        AutoReplyService.AutoReplyResult result = autoReplyService.sendAutoReply(100L, 11L, "123456");

        assertFalse(result.success());
        verify(ticketMapper, never()).update(eq(null), any());
        verify(mailSendService, never()).sendThreadedMail(any());
    }

    private TicketEntity ticket() {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(100L);
        ticket.setEnterpriseId(1L);
        ticket.setMailboxId(11L);
        ticket.setTicketNo("TCK-100");
        ticket.setSubject("咨询");
        ticket.setCustomerEmail("customer@example.com");
        ticket.setCustomerAccessExpiresAt(LocalDateTime.parse("2026-08-29T10:00:00"));
        return ticket;
    }

    private MailboxEntity mailbox(Long templateId) {
        MailboxEntity mailbox = new MailboxEntity();
        mailbox.setId(11L);
        mailbox.setEnterpriseId(1L);
        mailbox.setEmailAddress("support@example.com");
        mailbox.setSmtpUsername("support@example.com");
        mailbox.setAutoReplyEnabled(true);
        mailbox.setAutoReplyTemplateId(templateId);
        return mailbox;
    }

    private NotificationTemplateEntity template(Long id, Long enterpriseId, String type, boolean enabled) {
        NotificationTemplateEntity template = new NotificationTemplateEntity();
        template.setId(id);
        template.setTemplateType(type);
        template.setEnabled(enabled);
        template.setSubjectTpl("工单 {ticket_no} 已创建");
        template.setContentTpl("主题：{subject}，查询码：{customer_ticket_code}");
        return template;
    }
}
