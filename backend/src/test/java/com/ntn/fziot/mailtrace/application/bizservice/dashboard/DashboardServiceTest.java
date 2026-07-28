package com.ntn.fziot.mailtrace.application.bizservice.dashboard;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.dashboard.DashboardSummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.dashboard.DashboardTodoListResponse;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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

    @InjectMocks
    private DashboardService dashboardService;

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "系统管理员", "admin@example.com", "ADMIN");
    private final CurrentUserPrincipal agent = new CurrentUserPrincipal(
            2L, "agent", "处理人", "agent@example.com", "AGENT");
    private final CurrentUserPrincipal customer = new CurrentUserPrincipal(
            3L, "customer", "客户", "customer@example.com", "CUSTOMER");

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        initTableInfo(configuration, "DashboardServiceTest.TicketEntity", TicketEntity.class);
    }

    @Test
    void summary_shouldReturnDashboardMetricsWithSameTicketStatsCounting() {
        when(ticketMapper.countActiveTotal()).thenReturn(20L);
        when(ticketMapper.countByStatus("PENDING_ASSIGN")).thenReturn(3L);
        when(ticketMapper.countByStatus("PROCESSING")).thenReturn(7L);
        when(ticketMapper.countByStatus("WAITING_CUSTOMER")).thenReturn(4L);
        when(ticketMapper.countSlaOverdue()).thenReturn(2L);
        when(ticketMapper.countClosedToday()).thenReturn(5L);

        DashboardSummaryVO summary = dashboardService.summary(admin);

        assertEquals(20L, summary.totalCount());
        assertEquals(3L, summary.pendingAssignCount());
        assertEquals(7L, summary.processingCount());
        assertEquals(4L, summary.waitingCustomerCount());
        assertEquals(2L, summary.slaOverdueCount());
        assertEquals(5L, summary.closedTodayCount());
        assertEquals(14L, summary.activeCount());
        verify(ticketMapper).countActiveTotal();
    }

    @Test
    void summary_whenAgent_shouldAllow() {
        when(ticketMapper.countActiveTotal()).thenReturn(0L);
        when(ticketMapper.countByStatus("PENDING_ASSIGN")).thenReturn(0L);
        when(ticketMapper.countByStatus("PROCESSING")).thenReturn(0L);
        when(ticketMapper.countByStatus("WAITING_CUSTOMER")).thenReturn(0L);
        when(ticketMapper.countSlaOverdue()).thenReturn(0L);
        when(ticketMapper.countClosedToday()).thenReturn(0L);

        DashboardSummaryVO summary = dashboardService.summary(agent);

        assertEquals(0L, summary.totalCount());
    }

    @Test
    void summary_whenNotAgentOrAdmin_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dashboardService.summary(customer));

        assertTrue(ex.getMessage().contains("仅管理员和处理人"));
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

        assertTrue(ex.getMessage().contains("仅管理员和处理人"));
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

    private static void initTableInfo(MybatisConfiguration configuration, String namespace, Class<?> entityClass) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, namespace);
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
