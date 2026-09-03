package com.ntn.fziot.mailtrace.application.bizservice.ticket;

import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.repox.mysql.entity.CustomerEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.NotificationTemplateEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.CustomerMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentReplyTemplateServiceTest {

    @Mock
    private NotificationTemplateMapper notificationTemplateMapper;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CustomerTicketAccessService customerTicketAccessService;

    @InjectMocks
    private AgentReplyTemplateService service;

    @Test
    void listAvailableTemplates_shouldMarkMailboxDefault() {
        MailboxEntity mailbox = mailbox();
        when(notificationTemplateMapper.selectList(any())).thenReturn(List.of(
                template(501L, "默认回复", "您好：{reply_content}"),
                template(502L, "售后回复", "售后处理结果：{reply_content}")));

        var templates = service.listAvailableTemplates(mailbox);

        assertEquals(2, templates.size());
        assertTrue(templates.get(0).defaultTemplate());
        assertEquals(502L, templates.get(1).id());
    }

    @Test
    void render_whenTemplateSelected_shouldOverrideMailboxDefaultAndRenderOnce() {
        TicketEntity ticket = ticket();
        MailboxEntity mailbox = mailbox();
        when(notificationTemplateMapper.selectById(502L))
                .thenReturn(template(502L, "售后回复", "工单 {ticket_no}\n{reply_content}"));
        when(customerMapper.selectById(31L)).thenReturn(customer());
        when(userMapper.selectById(41L)).thenReturn(assignee());
        when(customerTicketAccessService.buildTicketUrl(ticket)).thenReturn("https://example.test/TCK-1");
        when(customerTicketAccessService.formatExpiresAt(ticket.getCustomerAccessExpiresAt()))
                .thenReturn("2026-09-05 10:00");

        AgentReplyTemplateService.RenderedReply rendered = service.render(
                ticket, mailbox, 502L,
                "已完成，原文 {ticket_no} 不应再次替换",
                "<p>已完成，原文 {ticket_no} 不应再次替换</p>",
                "Re: 原始主题");

        assertEquals(502L, rendered.templateId());
        assertEquals("SELECTED", rendered.templateSource());
        assertEquals("Re: 原始主题", rendered.subject());
        assertTrue(rendered.contentText().contains("TCK-1"));
        assertTrue(rendered.contentText().contains("原文 {ticket_no} 不应再次替换"));
        assertTrue(rendered.contentHtml().contains("<p style=\"margin:0\">已完成"));
        assertEquals(MailSendService.CONTENT_TYPE_HTML, rendered.contentType());
    }

    @Test
    void render_whenTemplateAndEditorBothProvideSpacing_shouldKeepOnlyTemplateBlankLine() {
        when(notificationTemplateMapper.selectById(502L))
                .thenReturn(template(502L, "标准回复", "上文\n\n{reply_content}\n\n下文"));

        AgentReplyTemplateService.RenderedReply rendered = service.render(
                ticket(), mailbox(), 502L, "正文", "<p>正文</p>", "Re: 原始主题");

        assertEquals(
                "上文<br/><br/><p style=\"margin:0\">正文</p><br/>下文",
                rendered.contentHtml());
    }

    @Test
    void render_whenTemplateHasSingleLineBreakAfterBlockReply_shouldNotAddBlankLine() {
        when(notificationTemplateMapper.selectById(502L))
                .thenReturn(template(502L, "标准回复", "上文\n{reply_content}\n下文"));

        AgentReplyTemplateService.RenderedReply rendered = service.render(
                ticket(), mailbox(), 502L, "正文", "<p>正文</p>", "Re: 原始主题");

        assertEquals(
                "上文<br/><p style=\"margin:0\">正文</p>下文",
                rendered.contentHtml());
    }

    @Test
    void render_whenSelectedTemplateMissesReplyContent_shouldRejectWithoutFallback() {
        when(notificationTemplateMapper.selectById(502L))
                .thenReturn(template(502L, "错误模板", "固定内容"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.render(ticket(), mailbox(), 502L, "客服回复", null, "Re: 主题"));

        assertTrue(exception.getMessage().contains("未包含回复内容变量"));
    }

    private MailboxEntity mailbox() {
        MailboxEntity mailbox = new MailboxEntity();
        mailbox.setId(11L);
        mailbox.setAgentReplyTemplateId(501L);
        mailbox.setEmailAddress("service@example.com");
        mailbox.setSmtpUsername("smtp@example.com");
        return mailbox;
    }

    private TicketEntity ticket() {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(21L);
        ticket.setTicketNo("TCK-1");
        ticket.setSubject("原始主题");
        ticket.setCustomerId(31L);
        ticket.setCustomerEmail("customer@example.com");
        ticket.setAssigneeId(41L);
        ticket.setCustomerAccessExpiresAt(LocalDateTime.of(2026, 9, 5, 10, 0));
        return ticket;
    }

    private CustomerEntity customer() {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(31L);
        customer.setDisplayName("客户甲");
        return customer;
    }

    private UserEntity assignee() {
        UserEntity user = new UserEntity();
        user.setId(41L);
        user.setDisplayName("客服甲");
        return user;
    }

    private NotificationTemplateEntity template(Long id, String name, String content) {
        NotificationTemplateEntity template = new NotificationTemplateEntity();
        template.setId(id);
        template.setTemplateName(name);
        template.setTemplateType("AGENT_REPLY");
        template.setSubjectTpl("Re: {subject}");
        template.setContentTpl(content);
        template.setEnabled(true);
        return template;
    }
}
