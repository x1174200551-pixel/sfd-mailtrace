package com.ntn.fziot.mailtrace.application.bizservice.dashboard;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.DataScopeService;
import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.dashboard.DashboardReportVO;
import com.ntn.fziot.mailtrace.interfaces.vo.dashboard.DashboardSummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.dashboard.DashboardTodoListResponse;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailFetchLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailFetchLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailSendLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MailboxMapper mailboxMapper;
    @Mock
    private EnterpriseMapper enterpriseMapper;
    @Mock
    private MailFetchLogMapper mailFetchLogMapper;
    @Mock
    private MailSendLogMapper mailSendLogMapper;
    @Mock
    private PermissionService permissionService;
    @Mock
    private DataScopeService dataScopeService;
    @Mock
    private EnterpriseMailboxAccessService enterpriseMailboxAccessService;

    @InjectMocks
    private DashboardService dashboardService;

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "系统管理员", "admin@example.com", "ADMIN");
    private final CurrentUserPrincipal agent = new CurrentUserPrincipal(
            2L, "agent", "处理人", "agent@example.com", "AGENT");
    private final CurrentUserPrincipal customer = new CurrentUserPrincipal(
            3L, "customer", "客户", "customer@example.com", "CUSTOMER");

    @BeforeEach
    void setUp() {
        allowAdminAndAgentOperationalPermissions();
        org.mockito.Mockito.lenient().when(enterpriseMailboxAccessService.resolveReadableMailboxIds(any()))
                .thenReturn(Set.of(11L));
    }

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        initTableInfo(configuration, "DashboardServiceTest.TicketEntity", TicketEntity.class);
        initTableInfo(configuration, "DashboardServiceTest.MailFetchLogEntity", MailFetchLogEntity.class);
        initTableInfo(configuration, "DashboardServiceTest.MailSendLogEntity", MailSendLogEntity.class);
    }

    @Test
    void summary_shouldReturnDashboardMetricsWithSameTicketStatsCounting() {
        when(ticketMapper.selectCount(any())).thenReturn(20L, 3L, 7L, 4L, 2L, 5L);

        DashboardSummaryVO summary = dashboardService.summary(admin);

        assertEquals(20L, summary.totalCount());
        assertEquals(3L, summary.pendingAssignCount());
        assertEquals(7L, summary.processingCount());
        assertEquals(4L, summary.waitingCustomerCount());
        assertEquals(2L, summary.slaOverdueCount());
        assertEquals(5L, summary.closedTodayCount());
        assertEquals(14L, summary.activeCount());
        verify(ticketMapper, org.mockito.Mockito.times(6)).selectCount(any());
    }

    @Test
    void summary_whenAgent_shouldAllow() {
        when(ticketMapper.selectCount(any())).thenReturn(0L);

        DashboardSummaryVO summary = dashboardService.summary(agent);

        assertEquals(0L, summary.totalCount());
    }

    @Test
    void summary_whenAgent_shouldApplyMailboxScopeToEveryMetric() {
        when(ticketMapper.selectCount(any())).thenReturn(0L);

        dashboardService.summary(agent);

        verify(dataScopeService, org.mockito.Mockito.times(6))
                .applyTicketScope(any(), org.mockito.ArgumentMatchers.eq(agent));
    }

    @Test
    void summary_whenNotAgentOrAdmin_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dashboardService.summary(customer));

        assertTrue(ex.getMessage().contains("无权查看工作台"));
    }

    @Test
    void summary_whenAnonymous_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dashboardService.summary(null));

        assertTrue(ex.getMessage().contains("未登录"));
    }

    @Test
    void myTodos_shouldReturnCurrentUserTodosAndSummary() {
        when(ticketMapper.selectList(any())).thenReturn(List.of(
                ticket(100L, "PROCESSING"),
                ticket(101L, "WAITING_CUSTOMER")
        ));
        when(ticketMapper.selectCount(any())).thenReturn(2L, 1L, 1L, 1L);
        when(userMapper.selectById(2L)).thenReturn(user(2L, "张三"));
        when(mailboxMapper.selectById(11L)).thenReturn(mailbox(11L, "客服邮箱"));

        DashboardTodoListResponse response = dashboardService.myTodos(agent, 5);

        assertEquals(2, response.records().size());
        assertEquals(2L, response.totalCount());
        assertEquals(1L, response.processingCount());
        assertEquals(1L, response.waitingCustomerCount());
        assertEquals(1L, response.slaOverdueCount());
        assertEquals(5, response.limit());
        assertEquals("张三", response.records().get(0).assigneeName());
        assertEquals("客服邮箱", response.records().get(0).mailboxName());
        verify(ticketMapper).selectList(any());
    }

    @Test
    void myTodos_whenLimitMissing_shouldUseDefaultLimit() {
        when(ticketMapper.selectList(any())).thenReturn(List.of());
        when(ticketMapper.selectCount(any())).thenReturn(0L);

        DashboardTodoListResponse response = dashboardService.myTodos(agent, null);

        assertEquals(10, response.limit());
    }

    @Test
    void myTodos_whenLimitTooLarge_shouldCapAtMax() {
        when(ticketMapper.selectList(any())).thenReturn(List.of());
        when(ticketMapper.selectCount(any())).thenReturn(0L);

        DashboardTodoListResponse response = dashboardService.myTodos(agent, 200);

        assertEquals(50, response.limit());
    }

    @Test
    void myTodos_whenLimitInvalid_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dashboardService.myTodos(agent, 0));

        assertTrue(ex.getMessage().contains("待办条数必须大于 0"));
    }

    @Test
    void myTodos_whenNotAgentOrAdmin_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dashboardService.myTodos(customer, 10));

        assertTrue(ex.getMessage().contains("无权查看工作台"));
    }

    @Test
    void report_shouldReturnDashboardReportFromBackendMetrics() {
        when(ticketMapper.selectCount(any())).thenReturn(
                5L, 3L, 10L, 2L, 1L, 4L, 1L, 2L,
                10L, 1L, 1L, 5L, 1L,
                1L, 2L, 5L, 0L
        );
        when(mailFetchLogMapper.selectList(any())).thenReturn(List.of(
                fetchLog(true, 8, 5, 2),
                fetchLog(false, 2, 0, 1)
        ));
        when(mailSendLogMapper.selectCount(any())).thenReturn(1L);
        when(ticketMapper.selectList(any())).thenReturn(List.of(
                ticket(100L, "PROCESSING"),
                ticket(101L, "WAITING_CUSTOMER")
        ));
        when(userMapper.selectById(2L)).thenReturn(user(2L, "张三"));

        DashboardReportVO report = dashboardService.report(agent);

        assertEquals(60, report.efficiency().completionRate());
        assertEquals("5", report.efficiency().items().get(0).value());
        assertEquals("普通", report.priorityDistribution().items().get(2).label());
        assertEquals(5L, report.priorityDistribution().items().get(2).value());
        assertEquals("80%", report.slaHealth().items().get(0).value());
        assertEquals("50%", report.mailFlow().items().get(0).value());
        assertEquals("5", report.mailFlow().items().get(1).value());
        assertEquals(1L, report.actionPanel().items().get(3).value());
        assertEquals("张三", report.assigneeLoads().get(0).name());
        assertEquals(2L, report.qualityChecks().get(0).value());
    }

    private TicketEntity ticket(Long id, String status) {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(id);
        ticket.setTicketNo("TCK-20260727-" + id);
        ticket.setSubject("待办工单");
        ticket.setStatus(status);
        ticket.setPriority("NORMAL");
        ticket.setMailboxId(11L);
        ticket.setCustomerEmail("customer@example.com");
        ticket.setAssigneeId(2L);
        ticket.setLinkSuspect(false);
        ticket.setSlaResponseDeadline(LocalDateTime.parse("2026-07-27T18:00:00"));
        ticket.setSlaBreached(Long.valueOf(101L).equals(id));
        ticket.setCreatedAt(LocalDateTime.parse("2026-07-27T10:00:00"));
        return ticket;
    }

    private UserEntity user(Long id, String displayName) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setDisplayName(displayName);
        return user;
    }

    private MailboxEntity mailbox(Long id, String mailboxName) {
        MailboxEntity mailbox = new MailboxEntity();
        mailbox.setId(id);
        mailbox.setMailboxName(mailboxName);
        return mailbox;
    }

    private MailFetchLogEntity fetchLog(boolean success, int fetchedCount, int createdTicketCount, int linkedCount) {
        MailFetchLogEntity log = new MailFetchLogEntity();
        log.setSuccess(success);
        log.setFetchedCount(fetchedCount);
        log.setCreatedTicketCount(createdTicketCount);
        log.setLinkedCount(linkedCount);
        return log;
    }

    private static void initTableInfo(MybatisConfiguration configuration, String namespace, Class<?> entityClass) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, namespace);
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }

    private void allowAdminAndAgentOperationalPermissions() {
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
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
        }).when(permissionService).assertPermission(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private boolean isAgentOperationalPermission(CurrentUserPrincipal principal, String permissionCode) {
        return "AGENT".equals(principal.roleCode())
                && (permissionCode.startsWith("ticket:")
                || permissionCode.startsWith("ticket_attachment:")
                || "customer:read".equals(permissionCode)
                || "dashboard:read".equals(permissionCode));
    }
}
