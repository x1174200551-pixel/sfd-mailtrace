package com.ntn.fziot.mailtrace.application.bizservice.role;

import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.OperationLogService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.role.RoleDataScopeRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.role.RolePermissionSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.role.RoleSaveRequest;
import com.ntn.fziot.mailtrace.repox.mysql.entity.PermissionEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleDataScopeEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RolePermissionEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.PermissionMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleDataScopeMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RolePermissionMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class RoleManagementServiceTest {

    @Mock
    private RoleMapper roleMapper;
    @Mock
    private PermissionMapper permissionMapper;
    @Mock
    private RolePermissionMapper rolePermissionMapper;
    @Mock
    private RoleDataScopeMapper roleDataScopeMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private OperationLogService operationLogService;
    @Mock
    private PermissionService permissionService;

    private RoleManagementService roleManagementService;

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "管理员", "admin@example.com", "ADMIN");

    @BeforeEach
    void setUp() {
        roleManagementService = new RoleManagementService(
                roleMapper,
                permissionMapper,
                rolePermissionMapper,
                roleDataScopeMapper,
                userRoleMapper,
                operationLogService,
                permissionService
        );
    }

    @Test
    void createRole_shouldInsertCustomRoleAndRecordLog() {
        RoleSaveRequest request = new RoleSaveRequest();
        request.setRoleCode("quality_checker");
        request.setRoleName("工单质检");
        request.setRoleDesc("查看工单和客户");
        request.setEnabled(true);

        when(roleMapper.selectCount(any())).thenReturn(0L);
        when(roleMapper.selectOne(any())).thenReturn(role(20L, "AGENT", "客服处理人", true, true, 20));
        doAnswer(invocation -> {
            RoleEntity role = invocation.getArgument(0);
            role.setId(30L);
            return 1;
        }).when(roleMapper).insert(any(RoleEntity.class));
        when(roleMapper.selectById(30L)).thenReturn(role(30L, "QUALITY_CHECKER", "工单质检", false, true, 30));
        when(rolePermissionMapper.selectList(any())).thenReturn(List.of());
        when(roleDataScopeMapper.selectList(any())).thenReturn(List.of());
        when(userRoleMapper.selectCount(any())).thenReturn(0L);

        roleManagementService.createRole(admin, request);

        ArgumentCaptor<RoleEntity> roleCaptor = ArgumentCaptor.forClass(RoleEntity.class);
        verify(roleMapper).insert(roleCaptor.capture());
        RoleEntity inserted = roleCaptor.getValue();
        assertEquals("QUALITY_CHECKER", inserted.getRoleCode());
        assertEquals("工单质检", inserted.getRoleName());
        assertFalse(inserted.getSystemRole());
        assertEquals(30, inserted.getSortOrder());
        verify(operationLogService).record(any(), eq("ROLE"), any(), any(), any());
    }

    @Test
    void updateRole_whenSystemRole_shouldReject() {
        RoleSaveRequest request = new RoleSaveRequest();
        request.setRoleName("系统管理员");
        request.setEnabled(true);
        when(roleMapper.selectById(10L)).thenReturn(role(10L, "ADMIN", "系统管理员", true, true, 10));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> roleManagementService.updateRole(admin, 10L, request));

        assertEquals(40302, ex.getCode());
    }

    @Test
    void saveRolePermissions_shouldReplacePermissionAndDataScopeRelations() {
        RolePermissionSaveRequest request = new RolePermissionSaveRequest();
        request.setPermissionCodes(List.of("menu:tickets", "ticket:read"));
        RoleDataScopeRequest ticketScope = new RoleDataScopeRequest();
        ticketScope.setResourceType("TICKET");
        ticketScope.setScopeCode("SELF");
        ticketScope.setScopeDesc("自己负责工单");
        request.setDataScopes(List.of(ticketScope));

        when(roleMapper.selectById(30L)).thenReturn(role(30L, "QUALITY_CHECKER", "工单质检", false, true, 30));
        when(permissionMapper.selectList(any())).thenReturn(List.of(
                permission(101L, "menu:tickets", 21),
                permission(102L, "ticket:read", 2010)
        ));
        when(rolePermissionMapper.selectList(any())).thenReturn(List.of());
        when(roleDataScopeMapper.selectList(any())).thenReturn(List.of());
        when(userRoleMapper.selectCount(any())).thenReturn(0L);

        roleManagementService.saveRolePermissions(admin, 30L, request);

        verify(rolePermissionMapper).physicalDeleteByRoleId(30L);
        verify(roleDataScopeMapper).physicalDeleteByRoleId(30L);
        verify(rolePermissionMapper, times(2)).insert(any(RolePermissionEntity.class));
        ArgumentCaptor<RoleDataScopeEntity> dataScopeCaptor = ArgumentCaptor.forClass(RoleDataScopeEntity.class);
        verify(roleDataScopeMapper).insert(dataScopeCaptor.capture());
        assertEquals("TICKET", dataScopeCaptor.getValue().getResourceType());
        assertEquals("SELF", dataScopeCaptor.getValue().getScopeCode());
        verify(operationLogService).record(any(), eq("ROLE"), any(), any(), any());
    }

    @Test
    void saveRolePermissions_whenDeptAndChildrenScope_shouldAllow() {
        RolePermissionSaveRequest request = new RolePermissionSaveRequest();
        request.setPermissionCodes(List.of("menu:tickets", "ticket:read"));
        RoleDataScopeRequest ticketScope = new RoleDataScopeRequest();
        ticketScope.setResourceType("TICKET");
        ticketScope.setScopeCode("DEPT_AND_CHILDREN");
        ticketScope.setScopeDesc("部门及下级工单");
        request.setDataScopes(List.of(ticketScope));

        when(roleMapper.selectById(30L)).thenReturn(role(30L, "QUALITY_CHECKER", "工单质检", false, true, 30));
        when(permissionMapper.selectList(any())).thenReturn(List.of(
                permission(101L, "menu:tickets", 21),
                permission(102L, "ticket:read", 2010)
        ));
        when(rolePermissionMapper.selectList(any())).thenReturn(List.of());
        when(roleDataScopeMapper.selectList(any())).thenReturn(List.of());
        when(userRoleMapper.selectCount(any())).thenReturn(0L);

        roleManagementService.saveRolePermissions(admin, 30L, request);

        ArgumentCaptor<RoleDataScopeEntity> dataScopeCaptor = ArgumentCaptor.forClass(RoleDataScopeEntity.class);
        verify(roleDataScopeMapper).insert(dataScopeCaptor.capture());
        assertEquals("TICKET", dataScopeCaptor.getValue().getResourceType());
        assertEquals("DEPT_AND_CHILDREN", dataScopeCaptor.getValue().getScopeCode());
    }

    @Test
    void saveRolePermissions_whenUnsupportedResourceType_shouldReject() {
        RolePermissionSaveRequest request = new RolePermissionSaveRequest();
        request.setPermissionCodes(List.of("ticket:read"));
        RoleDataScopeRequest mailboxScope = new RoleDataScopeRequest();
        mailboxScope.setResourceType("MAILBOX");
        mailboxScope.setScopeCode("SELF");
        request.setDataScopes(List.of(mailboxScope));

        when(roleMapper.selectById(30L)).thenReturn(role(30L, "QUALITY_CHECKER", "工单质检", false, true, 30));
        when(permissionMapper.selectList(any())).thenReturn(List.of(permission(102L, "ticket:read", 2010)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> roleManagementService.saveRolePermissions(admin, 30L, request));

        assertEquals(40001, ex.getCode());
    }

    private RoleEntity role(Long id, String roleCode, String roleName, Boolean systemRole, Boolean enabled, Integer sortOrder) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setSystemRole(systemRole);
        role.setEnabled(enabled);
        role.setSortOrder(sortOrder);
        return role;
    }

    private PermissionEntity permission(Long id, String code, Integer sortOrder) {
        PermissionEntity permission = new PermissionEntity();
        permission.setId(id);
        permission.setPermissionCode(code);
        permission.setEnabled(true);
        permission.setSortOrder(sortOrder);
        return permission;
    }
}
