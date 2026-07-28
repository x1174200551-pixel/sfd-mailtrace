package com.ntn.fziot.mailtrace.application.bizservice.ticket;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntn.fziot.mailtrace.application.bizservice.assignment.AssignmentRuleMatchResult;
import com.ntn.fziot.mailtrace.application.bizservice.assignment.AssignmentRuleService;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.application.bizservice.security.DataScopeService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.application.bizservice.sla.SlaDeadlineResult;
import com.ntn.fziot.mailtrace.application.bizservice.sla.SlaDeadlineService;
import com.ntn.fziot.mailtrace.application.bizservice.sysparam.TicketNumberRuleService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketAssignRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketPriorityRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketReplyRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketStatusRequest;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEventEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketMessageEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketAttachmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketEventMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMessageMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketBizServiceTest {

    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private TicketEventMapper ticketEventMapper;
    @Mock
    private TicketMessageMapper ticketMessageMapper;
    @Mock
    private MailboxMapper mailboxMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private OperationLogMapper operationLogMapper;
    @Mock
    private TicketNumberRuleService ticketNumberRuleService;
    @Mock
    private AutoReplyService autoReplyService;
    @Mock
    private MailSendService mailSendService;
    @Mock
    private TicketAttachmentMapper ticketAttachmentMapper;
    @Mock
    private AssignmentRuleService assignmentRuleService;
    @Mock
    private SlaDeadlineService slaDeadlineService;
    @Mock
    private PermissionService permissionService;
    @Spy
    private DataScopeService dataScopeService = new DataScopeService();

    @InjectMocks
    private TicketBizService ticketBizService;

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "系统管理员", "admin@example.com", "ADMIN");
    private final CurrentUserPrincipal agent = new CurrentUserPrincipal(
            2L, "agent", "处理人", "agent@example.com", "AGENT");

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        initTableInfo(configuration, "TicketBizServiceTest.TicketEntity", TicketEntity.class);
        initTableInfo(configuration, "TicketBizServiceTest.TicketMessageEntity", TicketMessageEntity.class);
        initTableInfo(configuration, "TicketBizServiceTest.TicketEventEntity", TicketEventEntity.class);
    }

    @BeforeEach
    void setUp() {
        allowAdminAndAgentOperationalPermissions();
        lenient().when(ticketMessageMapper.selectList(any())).thenReturn(List.of());
        lenient().when(ticketEventMapper.selectList(any())).thenReturn(List.of());
        lenient().when(mailboxMapper.selectById(11L)).thenReturn(mailbox());
        lenient().when(userMapper.selectById(2L)).thenReturn(agent(2L, "张三", "agent@example.com"));
        lenient().when(assignmentRuleService.matchForTicket(any(), any(), any(), any())).thenReturn(null);
        lenient().when(slaDeadlineService.calculateForNewTicket(any())).thenReturn(SlaDeadlineResult.none());
    }

    private void allowAdminAndAgentOperationalPermissions() {
        lenient().doAnswer(invocation -> {
            CurrentUserPrincipal principal = invocation.getArgument(0);
            String permissionCode = invocation.getArgument(1);
            String message = invocation.getArgument(2);
            if (principal == null) {
                throw new BusinessException(40302, "未登录");
            }
            if ("ADMIN".equals(principal.roleCode()) || isAgentOperationalPermission(principal, permissionCode)) {
                return null;
            }
            throw new BusinessException(40302, message);
        }).when(permissionService).assertPermission(any(), any(), any());
    }

    private boolean isAgentOperationalPermission(CurrentUserPrincipal principal, String permissionCode) {
        return "AGENT".equals(principal.roleCode())
                && (permissionCode.startsWith("ticket:")
                || permissionCode.startsWith("ticket_attachment:")
                || "customer:read".equals(permissionCode)
                || "dashboard:read".equals(permissionCode));
    }

    @Test
    void replyTicket_externalReply_shouldSetFirstReplyAndAgentReplyEvents() {
        TicketEntity ticket = ticket(100L, TicketBizService.STATUS_PROCESSING);
        when(ticketMapper.selectById(100L)).thenReturn(ticket);
        when(mailSendService.sendRawMail(eq(11L), eq("customer@example.com"), any(), any(), eq("AGENT_REPLY")))
                .thenReturn(new MailSendService.SendResult(true, "OK"));

        ticketBizService.replyTicket(admin, 100L,
                new TicketReplyRequest("处理完成", "<p>处理完成</p>", false, List.of()));

        ArgumentCaptor<TicketEntity> updateCaptor = ArgumentCaptor.forClass(TicketEntity.class);
        verify(ticketMapper, org.mockito.Mockito.atLeastOnce()).updateById(updateCaptor.capture());
        assertTrue(updateCaptor.getAllValues().stream().anyMatch(update -> update.getFirstReplyAt() != null));

        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper, org.mockito.Mockito.atLeast(2)).insert(eventCaptor.capture());
        List<String> eventTypes = eventCaptor.getAllValues().stream().map(TicketEventEntity::getEventType).toList();
        assertTrue(eventTypes.contains(TicketBizService.EVENT_FIRST_REPLY));
        assertTrue(eventTypes.contains(TicketBizService.EVENT_AGENT_REPLY));
    }

    @Test
    void replyTicket_internalNote_shouldNotSendMailOrSetFirstReply() {
        TicketEntity ticket = ticket(101L, TicketBizService.STATUS_PROCESSING);
        when(ticketMapper.selectById(101L)).thenReturn(ticket);

        ticketBizService.replyTicket(admin, 101L,
                new TicketReplyRequest("内部观察", "<p>内部观察</p>", true, List.of()));

        verify(mailSendService, never()).sendRawMail(anyLong(), any(), any(), any(), any());
        verify(ticketMapper, never()).updateById(any(TicketEntity.class));

        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals(TicketBizService.EVENT_INTERNAL_NOTE, eventCaptor.getValue().getEventType());
    }

    @Test
    void assignTicket_shouldWriteReasonAndAllowSkippingNotify() {
        TicketEntity ticket = ticket(102L, TicketBizService.STATUS_PENDING_ASSIGN);
        when(ticketMapper.selectById(102L)).thenReturn(ticket);

        ticketBizService.assignTicket(admin, 102L, new TicketAssignRequest(2L, "专业组处理", false));

        verify(mailSendService, never()).sendRawMail(anyLong(), any(), any(), any(), any());
        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals(TicketBizService.EVENT_ASSIGNED, eventCaptor.getValue().getEventType());
        assertTrue(eventCaptor.getValue().getEventContent().contains("原因：专业组处理"));
    }

    @Test
    void claimTicket_whenUnassigned_shouldAssignCurrentUserAndWriteEvent() {
        TicketEntity ticket = ticket(111L, TicketBizService.STATUS_PENDING_ASSIGN);
        TicketEntity claimed = ticket(111L, TicketBizService.STATUS_PROCESSING);
        claimed.setAssigneeId(2L);
        when(ticketMapper.selectById(111L)).thenReturn(ticket, claimed);
        when(ticketMapper.update(eq(null), any())).thenReturn(1);

        ticketBizService.claimTicket(agent, 111L);

        ArgumentCaptor<LambdaUpdateWrapper<TicketEntity>> updateCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(ticketMapper).update(eq(null), updateCaptor.capture());
        String sqlSet = updateCaptor.getValue().getSqlSet();
        String sqlSegment = updateCaptor.getValue().getSqlSegment();
        assertTrue(sqlSet.contains("assignee_id"));
        assertTrue(sqlSet.contains("status"));
        assertTrue(sqlSegment.contains("assignee_id IS NULL"));

        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals(TicketBizService.EVENT_ASSIGNED, eventCaptor.getValue().getEventType());
        assertTrue(eventCaptor.getValue().getEventContent().contains("领取未分配工单"));
    }

    @Test
    void claimTicket_whenAlreadyAssigned_shouldReject() {
        TicketEntity ticket = ticket(112L, TicketBizService.STATUS_PROCESSING);
        ticket.setAssigneeId(3L);
        when(ticketMapper.selectById(112L)).thenReturn(ticket);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketBizService.claimTicket(admin, 112L));

        assertTrue(ex.getMessage().contains("工单已分配"));
        verify(ticketMapper, never()).update(eq(null), any());
    }

    @Test
    void claimTicket_whenConcurrentClaimed_shouldReject() {
        TicketEntity ticket = ticket(113L, TicketBizService.STATUS_PENDING_ASSIGN);
        when(ticketMapper.selectById(113L)).thenReturn(ticket);
        when(ticketMapper.update(eq(null), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketBizService.claimTicket(agent, 113L));

        assertTrue(ex.getMessage().contains("已被领取或已分配"));
    }

    @Test
    void updatePriority_shouldWriteChinesePriorityAndReason() {
        TicketEntity ticket = ticket(103L, TicketBizService.STATUS_PROCESSING);
        ticket.setPriority("NORMAL");
        when(ticketMapper.selectById(103L)).thenReturn(ticket);

        ticketBizService.updatePriority(admin, 103L, new TicketPriorityRequest("URGENT", "客户升级"));

        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals(TicketBizService.EVENT_PRIORITY_CHANGED, eventCaptor.getValue().getEventType());
        assertTrue(eventCaptor.getValue().getEventContent().contains("P3 普通 → P1 紧急"));
        assertTrue(eventCaptor.getValue().getEventContent().contains("说明：客户升级"));
    }

    @Test
    void closeTicket_shouldWriteSingleClosedEventWithChineseStatus() {
        TicketEntity ticket = ticket(104L, TicketBizService.STATUS_WAITING_CUSTOMER);
        when(ticketMapper.selectById(104L)).thenReturn(ticket);

        ticketBizService.closeTicket(admin, 104L, new TicketStatusRequest("CLOSED", "客户确认完成"));

        ArgumentCaptor<TicketEntity> updateCaptor = ArgumentCaptor.forClass(TicketEntity.class);
        verify(ticketMapper, org.mockito.Mockito.atLeastOnce()).updateById(updateCaptor.capture());
        assertTrue(updateCaptor.getAllValues().stream().anyMatch(update -> update.getClosedAt() != null));

        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals(TicketBizService.EVENT_CLOSED, eventCaptor.getValue().getEventType());
        assertTrue(eventCaptor.getValue().getEventContent().contains("待客户回复 → 已关闭"));
        assertTrue(eventCaptor.getValue().getEventContent().contains("说明：客户确认完成"));
    }

    @Test
    void handleCustomerFollowUp_whenClosed_shouldReopenToProcessing() {
        TicketEntity ticket = ticket(105L, TicketBizService.STATUS_CLOSED);
        when(ticketMapper.selectById(105L)).thenReturn(ticket);
        LocalDateTime mailSentAt = LocalDateTime.parse("2026-07-27T09:30:00");

        ticketBizService.handleCustomerFollowUp(
                105L, "Re: test", "customer@example.com", "追信", "<p>追信</p>",
                "<msg-1@example.com>", mailSentAt);

        ArgumentCaptor<TicketEntity> updateCaptor = ArgumentCaptor.forClass(TicketEntity.class);
        verify(ticketMapper, org.mockito.Mockito.atLeastOnce()).updateById(updateCaptor.capture());
        assertTrue(updateCaptor.getAllValues().stream()
                .anyMatch(update -> mailSentAt.equals(update.getLastCustomerMailAt())));

        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals(TicketBizService.EVENT_REOPENED, eventCaptor.getValue().getEventType());
        assertTrue(eventCaptor.getValue().getEventContent().contains("原状态：已关闭"));
    }

    @Test
    void handleCustomerFollowUp_whenWaitingCustomer_shouldReturnToProcessing() {
        TicketEntity ticket = ticket(107L, TicketBizService.STATUS_WAITING_CUSTOMER);
        when(ticketMapper.selectById(107L)).thenReturn(ticket);
        LocalDateTime mailSentAt = LocalDateTime.parse("2026-07-27T10:15:00");

        ticketBizService.handleCustomerFollowUp(
                107L, "Re: waiting", "customer@example.com", "已补充", "<p>已补充</p>",
                "<msg-2@example.com>", mailSentAt);

        verify(ticketMapper, org.mockito.Mockito.atLeastOnce()).update(eq(null), any());
        ArgumentCaptor<TicketEntity> updateCaptor = ArgumentCaptor.forClass(TicketEntity.class);
        verify(ticketMapper, org.mockito.Mockito.atLeastOnce()).updateById(updateCaptor.capture());
        assertTrue(updateCaptor.getAllValues().stream()
                .anyMatch(update -> mailSentAt.equals(update.getLastCustomerMailAt())));

        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper).insert(eventCaptor.capture());
        assertEquals(TicketBizService.EVENT_CUSTOMER_FOLLOWUP, eventCaptor.getValue().getEventType());
        assertTrue(eventCaptor.getValue().getEventContent().contains("自动转为处理中"));
    }

    @Test
    void pageTickets_whenSlaBreachedProvided_shouldQueryBySlaBreached() {
        Page<TicketEntity> pageResult = Page.of(1, 10);
        pageResult.setRecords(List.of(ticket(108L, TicketBizService.STATUS_PROCESSING)));
        pageResult.setTotal(1);
        when(ticketMapper.selectPage(any(), any())).thenReturn(pageResult);

        ticketBizService.pageTickets(admin, null, "ALL", true,
                null, null, null, null, 1, 10);

        ArgumentCaptor<LambdaQueryWrapper<TicketEntity>> queryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(ticketMapper).selectPage(any(), queryCaptor.capture());
        assertTrue(queryCaptor.getValue().getSqlSegment().contains("sla_breached"));
    }

    @Test
    void pageTickets_whenAgent_shouldLimitToOwnOrUnassignedTickets() {
        Page<TicketEntity> pageResult = Page.of(1, 10);
        pageResult.setRecords(List.of());
        when(ticketMapper.selectPage(any(), any())).thenReturn(pageResult);

        ticketBizService.pageTickets(agent, null, null, null,
                null, null, null, null, 1, 10);

        ArgumentCaptor<LambdaQueryWrapper<TicketEntity>> queryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(ticketMapper).selectPage(any(), queryCaptor.capture());
        String sqlSegment = queryCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("assignee_id"));
        assertTrue(sqlSegment.contains("IS NULL"));
    }

    @Test
    void getTicket_whenAgentViewsOtherAssignedTicket_shouldReject() {
        TicketEntity ticket = ticket(109L, TicketBizService.STATUS_PROCESSING);
        ticket.setAssigneeId(3L);
        when(ticketMapper.selectById(109L)).thenReturn(ticket);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketBizService.getTicket(agent, 109L));

        assertTrue(ex.getMessage().contains("无权查看"));
    }

    @Test
    void replyTicket_whenAgentOperatesUnassignedTicket_shouldReject() {
        TicketEntity ticket = ticket(110L, TicketBizService.STATUS_PENDING_ASSIGN);
        when(ticketMapper.selectById(110L)).thenReturn(ticket);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketBizService.replyTicket(agent, 110L,
                        new TicketReplyRequest("处理", null, false, List.of())));

        assertTrue(ex.getMessage().contains("无权操作"));
    }

    @Test
    void createTicket_whenAssignmentRuleMatches_shouldAssignByRuleAndRespectNotifySwitch() {
        when(ticketNumberRuleService.generateNextTicketNo()).thenReturn("TCK-20260727-200");
        when(ticketMapper.insert(any(TicketEntity.class))).thenAnswer(invocation -> {
            TicketEntity ticket = invocation.getArgument(0);
            ticket.setId(200L);
            return 1;
        });
        when(assignmentRuleService.matchForTicket(eq(11L), eq("support@example.com"),
                eq("VIP 订单咨询"), eq("vip@example.com")))
                .thenReturn(new AssignmentRuleMatchResult(
                        301L, "VIP 规则", "SUBJECT_KEYWORD", "VIP",
                        5L, "李四", "lisi@example.com", false));

        Long ticketId = ticketBizService.createTicket(
                11L, "VIP 订单咨询", "vip@example.com", "VIP 客户",
                "正文", "<p>正文</p>", "<msg-200@example.com>",
                null, null, LocalDateTime.now());

        assertEquals(200L, ticketId);
        ArgumentCaptor<TicketEntity> ticketCaptor = ArgumentCaptor.forClass(TicketEntity.class);
        verify(ticketMapper, org.mockito.Mockito.atLeastOnce()).updateById(ticketCaptor.capture());
        assertTrue(ticketCaptor.getAllValues().stream()
                .anyMatch(update -> Long.valueOf(5L).equals(update.getAssigneeId())
                        && TicketBizService.STATUS_PROCESSING.equals(update.getStatus())));
        verify(mailSendService, never()).sendRawMail(anyLong(), any(), any(), any(), any());

        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper, org.mockito.Mockito.atLeast(2)).insert(eventCaptor.capture());
        assertTrue(eventCaptor.getAllValues().stream()
                .anyMatch(event -> TicketBizService.EVENT_ASSIGNED.equals(event.getEventType())
                        && event.getEventContent().contains("命中规则：VIP 规则")));
    }

    @Test
    void createTicket_shouldApplySlaDeadlines() {
        LocalDateTime mailSentAt = LocalDateTime.parse("2026-07-27T10:00:00");
        LocalDateTime responseDeadline = LocalDateTime.parse("2026-07-27T14:00:00");
        LocalDateTime resolveDeadline = LocalDateTime.parse("2026-07-28T11:00:00");
        when(ticketNumberRuleService.generateNextTicketNo()).thenReturn("TCK-20260727-202");
        when(slaDeadlineService.calculateForNewTicket(mailSentAt))
                .thenReturn(new SlaDeadlineResult(20L, responseDeadline, resolveDeadline));
        when(ticketMapper.insert(any(TicketEntity.class))).thenAnswer(invocation -> {
            TicketEntity ticket = invocation.getArgument(0);
            ticket.setId(202L);
            return 1;
        });

        Long ticketId = ticketBizService.createTicket(
                11L, "SLA 咨询", "customer@example.com", "客户",
                "正文", "<p>正文</p>", "<msg-202@example.com>",
                null, null, mailSentAt);

        assertEquals(202L, ticketId);
        ArgumentCaptor<TicketEntity> ticketCaptor = ArgumentCaptor.forClass(TicketEntity.class);
        verify(ticketMapper).insert(ticketCaptor.capture());
        TicketEntity saved = ticketCaptor.getValue();
        assertEquals(TicketBizService.STATUS_PENDING_ASSIGN, saved.getStatus());
        assertEquals(11L, saved.getMailboxId());
        assertEquals("customer@example.com", saved.getCustomerEmail());
        assertEquals(20L, saved.getSlaPolicyId());
        assertEquals(responseDeadline, saved.getSlaResponseDeadline());
        assertEquals(resolveDeadline, saved.getSlaResolveDeadline());
        assertEquals(false, saved.getSlaBreached());
        assertEquals(false, saved.getSlaWarningSent());
        assertEquals(false, saved.getSlaBreachNotified());
        assertEquals(mailSentAt, saved.getLastCustomerMailAt());
    }

    @Test
    void createTicket_whenNoRuleMatches_shouldFallbackMailboxDefaultAssignee() {
        MailboxEntity mailbox = mailbox();
        mailbox.setDefaultAssigneeId(2L);
        when(mailboxMapper.selectById(11L)).thenReturn(mailbox);
        when(ticketNumberRuleService.generateNextTicketNo()).thenReturn("TCK-20260727-201");
        when(mailSendService.sendRawMail(eq(11L), eq("agent@example.com"), any(), any(), eq("ASSIGN_NOTIFY")))
                .thenReturn(new MailSendService.SendResult(true, "OK"));
        when(ticketMapper.insert(any(TicketEntity.class))).thenAnswer(invocation -> {
            TicketEntity ticket = invocation.getArgument(0);
            ticket.setId(201L);
            return 1;
        });

        Long ticketId = ticketBizService.createTicket(
                11L, "普通咨询", "customer@example.com", "普通客户",
                "正文", "<p>正文</p>", "<msg-201@example.com>",
                null, null, LocalDateTime.now());

        assertEquals(201L, ticketId);
        verify(assignmentRuleService).matchForTicket(11L, "support@example.com", "普通咨询", "customer@example.com");
        ArgumentCaptor<TicketEntity> ticketCaptor = ArgumentCaptor.forClass(TicketEntity.class);
        verify(ticketMapper).insert(ticketCaptor.capture());
        assertEquals(2L, ticketCaptor.getValue().getAssigneeId());
        assertEquals(TicketBizService.STATUS_PROCESSING, ticketCaptor.getValue().getStatus());

        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(ticketEventMapper, org.mockito.Mockito.atLeast(2)).insert(eventCaptor.capture());
        assertTrue(eventCaptor.getAllValues().stream()
                .anyMatch(event -> TicketBizService.EVENT_ASSIGNED.equals(event.getEventType())
                        && event.getEventContent().contains("来源：邮箱默认处理人")));
    }

    @Test
    void updateStatus_whenIllegalWaitingCustomer_shouldReject() {
        TicketEntity ticket = ticket(106L, TicketBizService.STATUS_PROCESSING);
        when(ticketMapper.selectById(106L)).thenReturn(ticket);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ticketBizService.updateStatus(admin, 106L, new TicketStatusRequest("WAITING_CUSTOMER", "manual")));

        assertTrue(ex.getMessage().contains("不能手动变更"));
    }

    @Test
    void stats_shouldUseExplicitActiveTotalCount() {
        when(ticketMapper.selectCount(any())).thenReturn(20L, 3L, 7L, 4L, 2L, 5L);

        TicketBizService.TicketStats stats = ticketBizService.stats(admin);

        assertEquals(20L, stats.totalCount());
        assertEquals(3L, stats.pendingAssignCount());
        assertEquals(7L, stats.processingCount());
        assertEquals(4L, stats.waitingCustomerCount());
        assertEquals(2L, stats.slaOverdueCount());
        assertEquals(5L, stats.closedTodayCount());
    }

    private TicketEntity ticket(Long id, String status) {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(id);
        ticket.setTicketNo("TCK-20260727-" + id);
        ticket.setSubject("测试工单");
        ticket.setStatus(status);
        ticket.setPriority("NORMAL");
        ticket.setMailboxId(11L);
        ticket.setCustomerEmail("customer@example.com");
        ticket.setLinkSuspect(false);
        ticket.setSlaBreached(false);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        return ticket;
    }

    private UserEntity agent(Long id, String displayName, String email) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setAccount("agent" + id);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setRoleCode("AGENT");
        user.setEnabled(true);
        return user;
    }

    private MailboxEntity mailbox() {
        MailboxEntity mailbox = new MailboxEntity();
        mailbox.setId(11L);
        mailbox.setMailboxName("客服邮箱");
        mailbox.setEmailAddress("support@example.com");
        return mailbox;
    }

    private static void initTableInfo(MybatisConfiguration configuration, String namespace, Class<?> entityClass) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, namespace);
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
