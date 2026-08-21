package com.ntn.fziot.mailtrace.application.bizservice.ticket;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.storage.FileStorageService;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.CustomerTicketDetailVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketAttachmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEventEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketMessageEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketAttachmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketEventMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMessageMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerTicketAccessServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private TicketEventMapper ticketEventMapper;
    @Mock
    private TicketMessageMapper ticketMessageMapper;
    @Mock
    private TicketAttachmentMapper ticketAttachmentMapper;
    @Mock
    private FileStorageService fileStorageService;

    private CustomerTicketAccessService service;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        initTableInfo(configuration, "CustomerTicketAccessServiceTest.TicketEntity", TicketEntity.class);
        initTableInfo(configuration, "CustomerTicketAccessServiceTest.TicketEventEntity", TicketEventEntity.class);
        initTableInfo(configuration, "CustomerTicketAccessServiceTest.TicketMessageEntity", TicketMessageEntity.class);
        initTableInfo(configuration, "CustomerTicketAccessServiceTest.TicketAttachmentEntity", TicketAttachmentEntity.class);
    }

    @BeforeEach
    void setUp() {
        service = new CustomerTicketAccessService(
                passwordEncoder,
                ticketMapper,
                ticketEventMapper,
                ticketMessageMapper,
                ticketAttachmentMapper,
                fileStorageService,
                "http://localhost:5174/customer/tickets",
                72,
                6);
        org.mockito.Mockito.lenient().when(ticketAttachmentMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void verifyAndGetDetail_shouldReturnOnlyCustomerVisibleTimelineAndMessages() {
        TicketEntity ticket = ticket();
        when(ticketMapper.selectOne(any())).thenReturn(ticket);
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(ticketMessageMapper.selectList(any())).thenReturn(List.of(
                message(TicketBizService.DIRECTION_INBOUND, "客户邮件正文"),
                message(TicketBizService.DIRECTION_INTERNAL, "内部备注内容"),
                message(TicketBizService.DIRECTION_OUTBOUND, "对外回复内容")
        ));
        when(ticketEventMapper.selectList(any())).thenReturn(List.of(
                event(TicketBizService.EVENT_CREATED, "工单已创建"),
                event(TicketBizService.EVENT_INTERNAL_NOTE, "内部备注：不要外显"),
                event(TicketBizService.EVENT_PRIORITY_CHANGED, "优先级变更：高"),
                event(TicketBizService.EVENT_ASSIGNED, "自动分配给张三"),
                event(TicketBizService.EVENT_FIRST_REPLY, "首次对外回复客户"),
                event(TicketBizService.EVENT_AGENT_REPLY, "回复客户：对外回复内容")
        ));

        CustomerTicketDetailVO detail = service.verifyAndGetDetail("TCK-260821101500-482931", "123456", "127.0.0.1");

        assertEquals(2, detail.messages().size());
        assertTrue(detail.timeline().stream().anyMatch(item -> "工单已创建".equals(item.title())));
        assertTrue(detail.timeline().stream().anyMatch(item -> "已进入处理".equals(item.title())));
        assertTrue(detail.timeline().stream().anyMatch(item -> "已首次回复".equals(item.title())));
        assertFalse(detail.timeline().stream().anyMatch(item -> item.content().contains("内部备注")));
        assertFalse(detail.timeline().stream().anyMatch(item -> item.content().contains("张三")));
        assertFalse(detail.timeline().stream().anyMatch(item -> item.content().contains("优先级")));
    }

    @Test
    void verifyAndGetDetail_shouldRenderCustomerInlineImageUrlInHtml() {
        TicketEntity ticket = ticket();
        TicketMessageEntity message = message(TicketBizService.DIRECTION_INBOUND, "客户邮件正文");
        message.setId(200L);
        message.setTicketId(ticket.getId());
        message.setContentHtml("<p>正文</p><img src=\"cid:logo@cid\">");
        TicketAttachmentEntity attachment = inlineAttachment(ticket.getId(), message.getId());
        when(ticketMapper.selectOne(any())).thenReturn(ticket);
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(ticketMessageMapper.selectList(any())).thenReturn(List.of(message));
        when(ticketAttachmentMapper.selectList(any())).thenReturn(List.of(attachment));
        when(ticketEventMapper.selectList(any())).thenReturn(List.of());

        CustomerTicketDetailVO detail = service.verifyAndGetDetail("TCK-260821101500-482931", "123456", "127.0.0.1");

        assertTrue(detail.email().contentHtml().contains("/api/v1/customer-tickets/TCK-260821101500-482931/attachments/301/download"));
        assertTrue(detail.email().contentHtml().contains("token="));
        assertFalse(detail.email().contentHtml().contains("123456"));
        assertFalse(detail.email().contentHtml().contains("cid:logo@cid"));
    }

    @Test
    void verifyAndGetDetail_whenFailedTooManyTimes_shouldLockAccessTemporarily() {
        TicketEntity ticket = ticket();
        when(ticketMapper.selectOne(any())).thenReturn(ticket);
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        for (int i = 0; i < 5; i++) {
            assertThrows(BusinessException.class,
                    () -> service.verifyAndGetDetail("TCK-260821101500-482931", "bad", "127.0.0.1"));
        }

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.verifyAndGetDetail("TCK-260821101500-482931", "123456", "127.0.0.1"));
        assertTrue(exception.getMessage().contains("失败次数过多"));
    }

    private TicketEntity ticket() {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(100L);
        ticket.setTicketNo("TCK-260821101500-482931");
        ticket.setSubject("订单咨询");
        ticket.setStatus(TicketBizService.STATUS_PROCESSING);
        ticket.setCustomerEmail("customer@example.com");
        ticket.setCustomerAccessEnabled(true);
        ticket.setCustomerAccessCodeHash("hash");
        ticket.setCustomerAccessExpiresAt(LocalDateTime.now().plusHours(1));
        ticket.setCreatedAt(LocalDateTime.parse("2026-08-21T10:15:00"));
        ticket.setUpdatedAt(LocalDateTime.parse("2026-08-21T11:32:00"));
        return ticket;
    }

    private TicketMessageEntity message(String direction, String content) {
        TicketMessageEntity message = new TicketMessageEntity();
        message.setDirection(direction);
        message.setFromAddress("customer@example.com");
        message.setToAddress("service@example.com");
        message.setSubject("订单咨询");
        message.setContentText(content);
        message.setSentAt(LocalDateTime.parse("2026-08-21T10:15:00"));
        return message;
    }

    private TicketAttachmentEntity inlineAttachment(Long ticketId, Long messageId) {
        TicketAttachmentEntity attachment = new TicketAttachmentEntity();
        attachment.setId(301L);
        attachment.setTicketId(ticketId);
        attachment.setMessageId(messageId);
        attachment.setFileName("logo.png");
        attachment.setFileSize(12L);
        attachment.setContentType("image/png");
        attachment.setObjectKey("mailtrace/logo.png");
        attachment.setIsInline(true);
        attachment.setContentId("logo@cid");
        attachment.setCreatedAt(LocalDateTime.parse("2026-08-21T10:15:00"));
        return attachment;
    }

    private TicketEventEntity event(String type, String content) {
        TicketEventEntity event = new TicketEventEntity();
        event.setEventType(type);
        event.setEventContent(content);
        event.setEventAt(LocalDateTime.parse("2026-08-21T10:15:00"));
        return event;
    }

    private static void initTableInfo(MybatisConfiguration configuration, String namespace, Class<?> entityClass) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, namespace);
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
