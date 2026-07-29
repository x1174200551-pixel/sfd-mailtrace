package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataScopeServiceTest {

    private final DataScopeService dataScopeService = new DataScopeService();

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "系统管理员", "admin@example.com", "ADMIN");
    private final CurrentUserPrincipal agent = new CurrentUserPrincipal(
            2L, "agent", "处理人", "agent@example.com", "AGENT");
    private final CurrentUserPrincipal customer = new CurrentUserPrincipal(
            3L, "customer", "客户", "customer@example.com", "CUSTOMER");
    private final CurrentUserPrincipal custom = new CurrentUserPrincipal(
            4L, "custom", "自定义角色", "custom@example.com", "AUTHQA_VIEWER");

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "DataScopeServiceTest.TicketEntity");
        TableInfoHelper.initTableInfo(assistant, TicketEntity.class);
    }

    @Test
    void applyTicketScope_whenAdmin_shouldNotAppendAssigneeCondition() {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<>();

        dataScopeService.applyTicketScope(wrapper, admin);

        assertTrue(wrapper.getSqlSegment().isBlank());
    }

    @Test
    void applyTicketScope_whenAgent_shouldLimitToOwnOrUnassignedTickets() {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<>();

        dataScopeService.applyTicketScope(wrapper, agent);

        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("assignee_id"));
        assertTrue(sqlSegment.contains("IS NULL"));
    }

    @Test
    void assertTicketVisible_whenAgentViewsUnassignedOrOwnTicket_shouldAllow() {
        assertDoesNotThrow(() -> dataScopeService.assertTicketVisible(agent, ticket(null)));
        assertDoesNotThrow(() -> dataScopeService.assertTicketVisible(agent, ticket(2L)));
    }

    @Test
    void assertTicketOperable_whenAgentOperatesUnassignedTicket_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dataScopeService.assertTicketOperable(agent, ticket(null)));

        assertTrue(ex.getMessage().contains("无权操作"));
    }

    @Test
    void assertAgentOrAdmin_whenCustomer_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dataScopeService.assertAgentOrAdmin(customer));

        assertTrue(ex.getMessage().contains("仅管理员和处理人"));
    }

    @Test
    void applyTicketScope_whenCustomRoleHasSelfScope_shouldLimitToOwnOrUnassignedTickets() {
        PermissionService permissionService = mock(PermissionService.class);
        when(permissionService.getCurrentPermissions(custom)).thenReturn(
                new PermissionService.PermissionContext(4L, Set.of("AUTHQA_VIEWER"), Set.of("ticket:read"),
                        Map.of("TICKET", Set.of("SELF"))));
        DataScopeService rbacDataScopeService = new DataScopeService(permissionService);
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<>();

        rbacDataScopeService.applyTicketScope(wrapper, custom);

        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("assignee_id"));
        assertTrue(sqlSegment.contains("IS NULL"));
    }

    private TicketEntity ticket(Long assigneeId) {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(100L);
        ticket.setAssigneeId(assigneeId);
        return ticket;
    }
}
