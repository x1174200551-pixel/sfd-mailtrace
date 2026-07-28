package com.ntn.fziot.mailtrace.application.bizservice.sla;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.repox.mysql.entity.SlaPolicyEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEventEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.SlaPolicyMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketEventMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlaCheckServiceTest {

    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private TicketEventMapper ticketEventMapper;
    @Mock
    private SlaPolicyMapper slaPolicyMapper;
    @Mock
    private NotificationTemplateMapper notificationTemplateMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MailSendService mailSendService;

    @InjectMocks
    private SlaCheckService slaCheckService;

    private final LocalDateTime now = LocalDateTime.parse("2026-07-27T10:00:00");

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        initTableInfo(configuration, "SlaCheckServiceTest.TicketEntity", TicketEntity.class);
        initTableInfo(configuration, "SlaCheckServiceTest.TicketEventEntity", TicketEventEntity.class);
        initTableInfo(configuration, "SlaCheckServiceTest.SlaPolicyEntity", SlaPolicyEntity.class);
        initTableInfo(configuration, "SlaCheckServiceTest.UserEntity", UserEntity.class);
    }

    @BeforeEach
    void setUp() {
        lenient().when(slaPolicyMapper.selectById(20L)).thenReturn(policy(20L, 1));
        lenient().when(ticketMapper.update(eq(null), any())).thenReturn(1);
        lenient().when(userMapper.selectById(2L)).thenReturn(agent());
        lenient().when(mailSendService.sendRawMail(any(), any(), any(), any(), any()))
                .thenReturn(MailSendService.SendResult.ok("发送成功"));
    }

    @Test
    void checkDueTickets_whenResponseDeadlineNear_shouldMarkWarningOnce() {
        TicketEntity ticket = ticket(100L);
        ticket.setSlaPolicyId(20L);
        ticket.setSlaResponseDeadline(now.plusMinutes(30));
        when(ticketMapper.selectList(any())).thenReturn(List.of(ticket));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.scannedCount());
        assertEquals(1, result.warningCount());
        assertEquals(0, result.breachCount());

        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals(SlaCheckService.EVENT_SLA_WARNING, eventCaptor.getValue().getEventType());
        assertTrue(eventCaptor.getValue().getEventContent().contains("首次响应 SLA 即将超时"));
        verify(mailSendService).sendRawMail(eq(11L), eq("agent@example.com"),
                any(), any(), eq(SlaCheckService.EVENT_SLA_WARNING));
    }

    @Test
    void checkDueTickets_whenResponseDeadlinePassed_shouldMarkBreachAndWarningSent() {
        TicketEntity ticket = ticket(101L);
        ticket.setSlaPolicyId(20L);
        ticket.setSlaResponseDeadline(now.minusMinutes(1));
        when(ticketMapper.selectList(any())).thenReturn(List.of(ticket));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.scannedCount());
        assertEquals(0, result.warningCount());
        assertEquals(1, result.breachCount());

        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals(SlaCheckService.EVENT_SLA_BREACH, eventCaptor.getValue().getEventType());
        assertTrue(eventCaptor.getValue().getEventContent().contains("首次响应 SLA 已超时"));
        verify(mailSendService).sendRawMail(eq(11L), eq("agent@example.com"),
                any(), any(), eq(SlaCheckService.EVENT_SLA_BREACH));
    }

    @Test
    void checkDueTickets_whenWarningAlreadySent_shouldNotRepeatWarning() {
        TicketEntity ticket = ticket(102L);
        ticket.setSlaPolicyId(20L);
        ticket.setSlaWarningSent(true);
        ticket.setSlaResponseDeadline(now.plusMinutes(30));
        when(ticketMapper.selectList(any())).thenReturn(List.of(ticket));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.scannedCount());
        assertEquals(0, result.warningCount());
        assertEquals(0, result.breachCount());
        verify(ticketMapper, never()).update(eq(null), any());
        verify(ticketEventMapper, never()).insert(any());
        verify(mailSendService, never()).sendRawMail(any(), any(), any(), any(), any());
    }

    @Test
    void checkDueTickets_whenBreachAlreadyNotified_shouldNotRepeatBreach() {
        TicketEntity ticket = ticket(103L);
        ticket.setSlaPolicyId(20L);
        ticket.setSlaResponseDeadline(now.minusMinutes(1));
        ticket.setSlaBreachNotified(true);
        when(ticketMapper.selectList(any())).thenReturn(List.of(ticket));
        when(ticketMapper.update(eq(null), any())).thenReturn(0);

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.scannedCount());
        assertEquals(0, result.warningCount());
        assertEquals(0, result.breachCount());
        verify(ticketEventMapper, never()).insert(any());
        verify(mailSendService, never()).sendRawMail(any(), any(), any(), any(), any());
    }

    @Test
    void checkDueTickets_whenFirstReplyDone_shouldCheckResolveDeadline() {
        TicketEntity ticket = ticket(104L);
        ticket.setSlaPolicyId(20L);
        ticket.setFirstReplyAt(now.minusHours(2));
        ticket.setSlaResponseDeadline(now.minusHours(1));
        ticket.setSlaResolveDeadline(now.plusMinutes(30));
        when(ticketMapper.selectList(any())).thenReturn(List.of(ticket));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.warningCount());
        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertTrue(eventCaptor.getValue().getEventContent().contains("解决 SLA 即将超时"));
    }

    @Test
    void checkDueTickets_whenClosedOrNoPendingDeadlineFromLoadedCandidate_shouldSkip() {
        TicketEntity ticket = ticket(105L);
        ticket.setFirstReplyAt(now.minusHours(1));
        ticket.setClosedAt(now.minusMinutes(10));
        ticket.setSlaResponseDeadline(now.minusMinutes(30));
        ticket.setSlaResolveDeadline(now.minusMinutes(5));
        when(ticketMapper.selectList(any())).thenReturn(List.of(ticket));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.scannedCount());
        assertEquals(0, result.warningCount());
        assertEquals(0, result.breachCount());
        verify(ticketMapper, never()).update(eq(null), any());
        verify(mailSendService, never()).sendRawMail(any(), any(), any(), any(), any());
    }

    private TicketEntity ticket(Long id) {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(id);
        ticket.setTicketNo("TCK-20260727-" + id);
        ticket.setSubject("SLA 测试");
        ticket.setStatus("PROCESSING");
        ticket.setMailboxId(11L);
        ticket.setAssigneeId(2L);
        ticket.setCustomerEmail("customer@example.com");
        ticket.setSlaWarningSent(false);
        ticket.setSlaBreached(false);
        ticket.setSlaBreachNotified(false);
        ticket.setCreatedAt(now.minusHours(2));
        ticket.setUpdatedAt(now.minusHours(2));
        return ticket;
    }

    private SlaPolicyEntity policy(Long id, Integer warningRemainHours) {
        SlaPolicyEntity policy = new SlaPolicyEntity();
        policy.setId(id);
        policy.setPolicyName("标准 SLA");
        policy.setWarningRemainHours(warningRemainHours);
        return policy;
    }

    private UserEntity agent() {
        UserEntity user = new UserEntity();
        user.setId(2L);
        user.setAccount("agent");
        user.setDisplayName("验收客服");
        user.setEmail("agent@example.com");
        user.setRoleCode("AGENT");
        user.setEnabled(true);
        return user;
    }

    private static void initTableInfo(MybatisConfiguration configuration, String namespace, Class<?> entityClass) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, namespace);
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
