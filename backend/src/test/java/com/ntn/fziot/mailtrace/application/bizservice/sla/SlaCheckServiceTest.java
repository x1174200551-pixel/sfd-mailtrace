package com.ntn.fziot.mailtrace.application.bizservice.sla;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.application.bizservice.notification.FeishuNotificationService;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEventEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
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
import static org.mockito.ArgumentMatchers.isNull;
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
    private NotificationTemplateMapper notificationTemplateMapper;
    @Mock
    private MailboxMapper mailboxMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MailSendService mailSendService;
    @Mock
    private FeishuNotificationService feishuNotificationService;
    @Mock
    private SlaDeadlineService slaDeadlineService;
    @Mock
    private SlaNotificationPolicyService slaNotificationPolicyService;

    @InjectMocks
    private SlaCheckService slaCheckService;

    private final LocalDateTime now = LocalDateTime.parse("2026-07-27T10:00:00");

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        initTableInfo(configuration, "SlaCheckServiceTest.TicketEntity", TicketEntity.class);
        initTableInfo(configuration, "SlaCheckServiceTest.TicketEventEntity", TicketEventEntity.class);
        initTableInfo(configuration, "SlaCheckServiceTest.UserEntity", UserEntity.class);
    }

    @BeforeEach
    void setUp() {
        lenient().when(ticketMapper.update(eq(null), any())).thenReturn(1);
        lenient().when(userMapper.selectById(2L)).thenReturn(agent());
        MailboxEntity mailbox = new MailboxEntity();
        mailbox.setId(11L);
        lenient().when(mailboxMapper.selectById(11L)).thenReturn(mailbox);
        lenient().when(mailSendService.sendRawMail(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(MailSendService.SendResult.ok("发送成功"));
        lenient().when(slaNotificationPolicyService.resolve(any()))
                .thenReturn(new SlaNotificationPolicyService.NotificationSettings(true, true, false, true, true, false));
        lenient().when(slaNotificationPolicyService.isEnabled(any(), any())).thenAnswer(invocation -> {
            SlaNotificationPolicyService.NotificationSettings settings = invocation.getArgument(0);
            String sendType = invocation.getArgument(1);
            return switch (sendType) {
                case SlaNotificationPolicyService.RESPONSE_WARNING -> settings.responseWarning();
                case SlaNotificationPolicyService.RESPONSE_BREACH -> settings.responseBreach();
                case SlaNotificationPolicyService.RESPONSE_ESCALATION -> settings.responseEscalation();
                case SlaNotificationPolicyService.RESOLVE_WARNING -> settings.resolveWarning();
                case SlaNotificationPolicyService.RESOLVE_BREACH -> settings.resolveBreach();
                case SlaNotificationPolicyService.RESOLVE_ESCALATION -> settings.resolveEscalation();
                default -> true;
            };
        });
    }

    @Test
    void checkDueTickets_whenResponseDeadlineNear_shouldMarkWarningOnce() {
        TicketEntity ticket = ticket(100L);
        ticket.setSlaPolicyId(20L);
        ticket.setSlaResponseDeadline(now.plusMinutes(30));
        ticket.setSlaResponseWarningAt(now.minusMinutes(30));
        when(ticketMapper.selectSlaCandidates(now)).thenReturn(List.of(ticket));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.scannedCount());
        assertEquals(1, result.warningCount());
        assertEquals(0, result.breachCount());

        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals("SLA_RESPONSE_WARNING", eventCaptor.getValue().getEventType());
        assertTrue(eventCaptor.getValue().getEventContent().contains("首次响应 SLA 即将超时"));
        verify(mailSendService).sendRawMail(eq(11L), eq("agent@example.com"),
                any(), any(), eq(SlaNotificationPolicyService.RESPONSE_WARNING),
                eq(100L), eq(null), eq(SlaCheckService.EVENT_SLA_WARNING));
    }

    @Test
    void checkDueTickets_whenResponseWarningDisabled_shouldRecordNodeWithoutSending() {
        TicketEntity ticket = ticket(115L);
        ticket.setSlaPolicyId(20L);
        ticket.setSlaResponseDeadline(now.plusMinutes(30));
        ticket.setSlaResponseWarningAt(now.minusMinutes(30));
        when(ticketMapper.selectSlaCandidates(now)).thenReturn(List.of(ticket));
        when(slaNotificationPolicyService.resolve(20L))
                .thenReturn(new SlaNotificationPolicyService.NotificationSettings(false, true, false, true, true, false));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.warningCount());
        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals(SlaNotificationPolicyService.RESPONSE_WARNING, eventCaptor.getValue().getEventType());
        assertTrue(eventCaptor.getValue().getEventContent().contains("通知已按 SLA 策略关闭"));
        verify(mailSendService, never()).sendRawMail(any(), any(), any(), any(), any(), any(), any(), any());
        verify(feishuNotificationService, never()).enqueue(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkDueTickets_shouldUseDistinctTitleAndIncludeBothDeadlines() {
        TicketEntity ticket = ticket(116L);
        ticket.setSlaPolicyId(20L);
        ticket.setSlaResponseDeadline(now.plusMinutes(30));
        ticket.setSlaResolveDeadline(now.plusHours(4));
        ticket.setSlaResponseWarningAt(now.minusMinutes(1));
        ticket.setSlaResolveWarningAt(now.plusHours(3));
        when(ticketMapper.selectSlaCandidates(now)).thenReturn(List.of(ticket));

        slaCheckService.checkDueTickets(now);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailSendService).sendRawMail(eq(11L), eq("agent@example.com"),
                subjectCaptor.capture(), contentCaptor.capture(), eq(SlaNotificationPolicyService.RESPONSE_WARNING),
                eq(116L), eq(null), eq(SlaCheckService.EVENT_SLA_WARNING));
        assertTrue(subjectCaptor.getValue().startsWith("【首次响应预警】"));
        assertTrue(contentCaptor.getValue().contains("首次响应截止：2026-07-27 10:30"));
        assertTrue(contentCaptor.getValue().contains("解决截止：2026-07-27 14:00"));
        assertTrue(contentCaptor.getValue().contains("本次触发时间：2026-07-27 10:00"));
    }

    @Test
    void checkDueTickets_whenResponseDeadlinePassed_shouldMarkBreachAndWarningSent() {
        TicketEntity ticket = ticket(101L);
        ticket.setSlaPolicyId(20L);
        ticket.setSlaResponseDeadline(now.minusMinutes(1));
        ticket.setSlaResponseWarningAt(now.minusHours(1));
        when(ticketMapper.selectSlaCandidates(now)).thenReturn(List.of(ticket));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.scannedCount());
        assertEquals(0, result.warningCount());
        assertEquals(1, result.breachCount());

        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals("SLA_RESPONSE_BREACH", eventCaptor.getValue().getEventType());
        assertTrue(eventCaptor.getValue().getEventContent().contains("首次响应 SLA 已超时"));
        verify(mailSendService).sendRawMail(eq(11L), eq("agent@example.com"),
                any(), any(), eq(SlaNotificationPolicyService.RESPONSE_BREACH),
                eq(101L), eq(null), eq(SlaCheckService.EVENT_SLA_BREACH));
    }

    @Test
    void checkDueTickets_whenWarningAlreadySent_shouldNotRepeatWarning() {
        TicketEntity ticket = ticket(102L);
        ticket.setSlaPolicyId(20L);
        ticket.setSlaResponseWarningTriggeredAt(now.minusMinutes(10));
        ticket.setSlaResponseDeadline(now.plusMinutes(30));
        ticket.setSlaResponseWarningAt(now.minusMinutes(30));
        when(ticketMapper.selectSlaCandidates(now)).thenReturn(List.of(ticket));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.scannedCount());
        assertEquals(0, result.warningCount());
        assertEquals(0, result.breachCount());
        verify(ticketMapper, never()).update(eq(null), any());
        verify(ticketEventMapper, never()).insert(any());
        verify(mailSendService, never()).sendRawMail(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkDueTickets_whenBreachAlreadyNotified_shouldNotRepeatBreach() {
        TicketEntity ticket = ticket(103L);
        ticket.setSlaPolicyId(20L);
        ticket.setSlaResponseDeadline(now.minusMinutes(1));
        ticket.setSlaResponseWarningAt(now.minusHours(1));
        ticket.setSlaResponseBreachTriggeredAt(now.minusSeconds(30));
        when(ticketMapper.selectSlaCandidates(now)).thenReturn(List.of(ticket));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.scannedCount());
        assertEquals(0, result.warningCount());
        assertEquals(0, result.breachCount());
        verify(ticketEventMapper, never()).insert(any());
        verify(mailSendService, never()).sendRawMail(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkDueTickets_whenFirstReplyDone_shouldCheckResolveDeadline() {
        TicketEntity ticket = ticket(104L);
        ticket.setSlaPolicyId(20L);
        ticket.setFirstReplyAt(now.minusHours(2));
        ticket.setSlaResponseDeadline(now.minusHours(1));
        ticket.setSlaResponseWarningAt(now.minusHours(2));
        ticket.setSlaResolveDeadline(now.plusMinutes(30));
        ticket.setSlaResolveWarningAt(now.minusMinutes(30));
        when(ticketMapper.selectSlaCandidates(now)).thenReturn(List.of(ticket));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.warningCount());
        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertTrue(eventCaptor.getValue().getEventContent().contains("解决 SLA 即将超时"));
    }

    @Test
    void checkDueTickets_whenAssigneeMissing_shouldStillEnqueueGroupNotification() {
        TicketEntity ticket = ticket(106L);
        ticket.setAssigneeId(null);
        ticket.setSlaPolicyId(20L);
        ticket.setSlaResponseDeadline(now.plusMinutes(30));
        ticket.setSlaResponseWarningAt(now.minusMinutes(30));
        when(ticketMapper.selectSlaCandidates(now)).thenReturn(List.of(ticket));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.warningCount());
        verify(mailSendService, never()).sendRawMail(any(), any(), any(), any(), any(), any(), any(), any());
        verify(feishuNotificationService).enqueue(
                isNull(), eq(ticket), isNull(), eq("未分配"), eq(SlaNotificationPolicyService.RESPONSE_WARNING),
                isNull(), any(), any());
    }

    @Test
    void checkDueTickets_whenClosedOrNoPendingDeadlineFromLoadedCandidate_shouldSkip() {
        TicketEntity ticket = ticket(105L);
        ticket.setFirstReplyAt(now.minusHours(1));
        ticket.setClosedAt(now.minusMinutes(10));
        ticket.setSlaResponseDeadline(now.minusMinutes(30));
        ticket.setSlaResponseWarningAt(now.minusHours(1));
        ticket.setSlaResolveDeadline(now.minusMinutes(5));
        ticket.setSlaResolveWarningAt(now.minusHours(1));
        when(ticketMapper.selectSlaCandidates(now)).thenReturn(List.of(ticket));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.scannedCount());
        assertEquals(0, result.warningCount());
        assertEquals(0, result.breachCount());
        verify(ticketMapper, never()).update(eq(null), any());
        verify(mailSendService, never()).sendRawMail(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkDueTickets_whenResolveWarningPending_shouldNotBeBlockedByResponseWarningHistory() {
        TicketEntity ticket = ticket(107L);
        ticket.setFirstReplyAt(now.minusHours(1));
        ticket.setSlaResponseDeadline(now.plusHours(1));
        ticket.setSlaResponseWarningAt(now.minusMinutes(30));
        ticket.setSlaResponseWarningTriggeredAt(now.minusMinutes(20));
        ticket.setSlaResolveDeadline(now.plusMinutes(30));
        ticket.setSlaResolveWarningAt(now.minusMinutes(10));
        when(ticketMapper.selectSlaCandidates(now)).thenReturn(List.of(ticket));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.warningCount());
        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals("SLA_RESOLVE_WARNING", eventCaptor.getValue().getEventType());
    }

    @Test
    void checkTicket_whenFirstReplyCompletedLate_shouldCompensateBreach() {
        TicketEntity ticket = ticket(108L);
        ticket.setFirstReplyAt(now);
        ticket.setSlaResponseDeadline(now.minusMinutes(1));
        ticket.setSlaResponseWarningAt(now.minusHours(1));
        when(ticketMapper.selectById(108L)).thenReturn(ticket);

        SlaCheckResult result = slaCheckService.checkTicket(108L, now);

        assertEquals(1, result.breachCount());
        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals("SLA_RESPONSE_BREACH", eventCaptor.getValue().getEventType());
    }

    @Test
    void checkDueTickets_whenEscalationTimeReached_shouldSendEscalation() {
        TicketEntity ticket = ticket(109L);
        ticket.setSlaResponseDeadline(now.minusHours(2));
        ticket.setSlaResponseWarningAt(now.minusHours(3));
        ticket.setSlaResponseEscalationAt(now.minusMinutes(1));
        ticket.setSlaResponseBreachTriggeredAt(now.minusHours(2));
        ticket.setSlaEscalateAfterBreachHoursSnapshot(2);
        when(ticketMapper.selectSlaCandidates(now)).thenReturn(List.of(ticket));
        when(slaNotificationPolicyService.resolve(any()))
                .thenReturn(new SlaNotificationPolicyService.NotificationSettings(true, true, true, true, true, false));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.escalationCount());
        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals("SLA_RESPONSE_ESCALATION", eventCaptor.getValue().getEventType());
        verify(mailSendService).sendRawMail(eq(11L), eq("agent@example.com"),
                any(), any(), eq(SlaNotificationPolicyService.RESPONSE_ESCALATION),
                eq(109L), eq(null), eq(SlaCheckService.EVENT_SLA_BREACH));
    }

    @Test
    void checkDueTickets_whenAtExactDeadline_shouldTreatAsBreach() {
        TicketEntity ticket = ticket(110L);
        ticket.setSlaResponseDeadline(now);
        ticket.setSlaResponseWarningAt(now.minusHours(1));
        when(ticketMapper.selectSlaCandidates(now)).thenReturn(List.of(ticket));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(1, result.breachCount());
    }

    @Test
    void checkTicket_whenClosedWithoutReplyAfterResponseDeadline_shouldCompensateResponseBreach() {
        TicketEntity ticket = ticket(111L);
        ticket.setStatus("CLOSED");
        ticket.setClosedAt(now);
        ticket.setSlaResponseDeadline(now.minusMinutes(1));
        ticket.setSlaResponseWarningAt(now.minusHours(1));
        ticket.setSlaResolveDeadline(now.plusHours(1));
        ticket.setSlaResolveWarningAt(now.plusMinutes(30));
        when(ticketMapper.selectById(111L)).thenReturn(ticket);

        SlaCheckResult result = slaCheckService.checkTicket(111L, now);

        assertEquals(1, result.breachCount());
        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals("SLA_RESPONSE_BREACH", eventCaptor.getValue().getEventType());
    }

    @Test
    void checkDueTickets_whenLegacySnapshotMissing_shouldBackfillScheduleWithoutChangingDeadlines() {
        TicketEntity ticket = ticket(112L);
        LocalDateTime responseDeadline = now.plusHours(2);
        ticket.setSlaPolicyId(20L);
        ticket.setSlaResponseDeadline(responseDeadline);
        when(ticketMapper.selectSlaCandidates(now)).thenReturn(List.of(ticket));
        when(slaDeadlineService.calculateForStoredDeadlines(20L, responseDeadline, null))
                .thenReturn(new SlaDeadlineResult(
                        20L, 1, 2,
                        responseDeadline, null,
                        now.plusHours(1), now.plusHours(4), null, null));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(0, result.warningCount());
        assertEquals(responseDeadline, ticket.getSlaResponseDeadline());
        assertEquals(now.plusHours(1), ticket.getSlaResponseWarningAt());
        assertEquals(1, ticket.getSlaWarningRemainHoursSnapshot());
        assertEquals(2, ticket.getSlaEscalateAfterBreachHoursSnapshot());
        verify(slaDeadlineService).calculateForStoredDeadlines(20L, responseDeadline, null);
        verify(ticketEventMapper, never()).insert(any());
    }

    @Test
    void checkTicket_whenHistoricalNotificationSuppressed_shouldNotCreateAnyNotification() {
        TicketEntity ticket = ticket(113L);
        ticket.setSlaNotificationSuppressed(true);
        ticket.setSlaResponseDeadline(now.minusHours(1));
        ticket.setSlaResolveDeadline(now.minusMinutes(30));
        when(ticketMapper.selectById(113L)).thenReturn(ticket);

        SlaCheckResult result = slaCheckService.checkTicket(113L, now);

        assertEquals(0, result.scannedCount());
        assertEquals(0, result.warningCount());
        assertEquals(0, result.breachCount());
        assertEquals(0, result.escalationCount());
        verify(slaDeadlineService, never()).calculateForStoredDeadlines(any(), any(), any());
        verify(ticketMapper, never()).update(isNull(), any());
        verify(ticketEventMapper, never()).insert(any());
        verify(mailSendService, never()).sendRawMail(any(), any(), any(), any(), any(), any(), any(), any());
        verify(feishuNotificationService, never()).enqueue(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void checkDueTickets_whenSuppressedTicketUnexpectedlyReturned_shouldDefensivelySkip() {
        TicketEntity ticket = ticket(114L);
        ticket.setSlaNotificationSuppressed(true);
        ticket.setSlaResponseDeadline(now.minusHours(1));
        when(ticketMapper.selectSlaCandidates(now)).thenReturn(List.of(ticket));

        SlaCheckResult result = slaCheckService.checkDueTickets(now);

        assertEquals(0, result.scannedCount());
        verify(ticketEventMapper, never()).insert(any());
        verify(mailSendService, never()).sendRawMail(any(), any(), any(), any(), any(), any(), any(), any());
        verify(feishuNotificationService, never()).enqueue(any(), any(), any(), any(), any(), any(), any(), any());
    }

    private TicketEntity ticket(Long id) {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(id);
        ticket.setEnterpriseId(1L);
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
