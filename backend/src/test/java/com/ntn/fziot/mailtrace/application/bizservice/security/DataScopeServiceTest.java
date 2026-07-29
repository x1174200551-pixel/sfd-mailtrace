package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.department.DepartmentMemberService;
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

        assertTrue(ex.getMessage().contains("仅管理员、主管和处理人"));
    }

    @Test
    void applyTicketScope_whenCustomRoleHasSelfScope_shouldLimitToOwnOrUnassignedTickets() {
        PermissionService permissionService = mock(PermissionService.class);
        DepartmentMemberService departmentMemberService = mock(DepartmentMemberService.class);
        when(permissionService.getCurrentPermissions(custom)).thenReturn(
                new PermissionService.PermissionContext(4L, Set.of("AUTHQA_VIEWER"), Set.of("ticket:read"),
                        Map.of("TICKET", Set.of("SELF"))));
        DataScopeService rbacDataScopeService = new DataScopeService(permissionService, departmentMemberService);
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<>();

        rbacDataScopeService.applyTicketScope(wrapper, custom);

        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("assignee_id"));
        assertTrue(sqlSegment.contains("IS NULL"));
    }

    @Test
    void applyTicketScope_whenCustomRoleHasDeptScope_shouldLimitToDeptMemberIds() {
        PermissionService permissionService = mock(PermissionService.class);
        DepartmentMemberService departmentMemberService = mock(DepartmentMemberService.class);
        when(permissionService.getCurrentPermissions(custom)).thenReturn(
                new PermissionService.PermissionContext(4L, Set.of("SUPERVISOR"), Set.of("ticket:read"),
                        Map.of("TICKET", Set.of("DEPT_AND_CHILDREN"))));
        when(departmentMemberService.resolveDeptAndChildrenMemberIds(4L)).thenReturn(Set.of(2L, 4L, 5L));
        DataScopeService rbacDataScopeService = new DataScopeService(permissionService, departmentMemberService);
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<>();

        rbacDataScopeService.applyTicketScope(wrapper, custom);

        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("IN"));
        assertTrue(sqlSegment.contains("IS NULL"));
    }

    @Test
    void assertTicketVisible_whenDeptScopeOtherDeptMember_shouldReject() {
        PermissionService permissionService = mock(PermissionService.class);
        DepartmentMemberService departmentMemberService = mock(DepartmentMemberService.class);
        when(permissionService.getCurrentPermissions(custom)).thenReturn(
                new PermissionService.PermissionContext(4L, Set.of("SUPERVISOR"), Set.of("ticket:read"),
                        Map.of("TICKET", Set.of("DEPT_AND_CHILDREN"))));
        when(departmentMemberService.resolveDeptAndChildrenMemberIds(4L)).thenReturn(Set.of(2L, 4L, 5L));
        DataScopeService rbacDataScopeService = new DataScopeService(permissionService, departmentMemberService);

        // assigneeId=6L 不在部门成员中
        BusinessException ex = assertThrows(BusinessException.class,
                () -> rbacDataScopeService.assertTicketVisible(custom, ticket(6L)));
        assertTrue(ex.getMessage().contains("无权查看"));
    }

    @Test
    void assertTicketOperable_whenDeptScopeDeptMember_shouldAllow() {
        PermissionService permissionService = mock(PermissionService.class);
        DepartmentMemberService departmentMemberService = mock(DepartmentMemberService.class);
        when(permissionService.getCurrentPermissions(custom)).thenReturn(
                new PermissionService.PermissionContext(4L, Set.of("SUPERVISOR"), Set.of("ticket:reply"),
                        Map.of("TICKET", Set.of("DEPT_AND_CHILDREN"))));
        when(departmentMemberService.resolveDeptAndChildrenMemberIds(4L)).thenReturn(Set.of(2L, 4L, 5L));
        DataScopeService rbacDataScopeService = new DataScopeService(permissionService, departmentMemberService);

        // assigneeId=2L 在部门成员中，应该可以操作
        assertDoesNotThrow(() -> rbacDataScopeService.assertTicketOperable(custom, ticket(2L)));
    }

    private TicketEntity ticket(Long assigneeId) {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(100L);
        ticket.setAssigneeId(assigneeId);
        return ticket;
    }
}
