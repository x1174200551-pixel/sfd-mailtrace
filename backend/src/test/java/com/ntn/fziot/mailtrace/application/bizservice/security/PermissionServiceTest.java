package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.PermissionEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RolePermissionEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserRoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.PermissionMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RolePermissionMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private RoleMapper roleMapper;
    @Mock
    private PermissionMapper permissionMapper;
    @Mock
    private RolePermissionMapper rolePermissionMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @InjectMocks
    private PermissionService permissionService;

    private final CurrentUserPrincipal agent = new CurrentUserPrincipal(
            2L, "agent", "处理人", "agent@example.com", "AGENT");

    @Test
    void getCurrentPermissions_shouldResolveRolesAndPermissionsWithoutLegacyDataScopes() {
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole(20L)));
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(role(20L, "AGENT", 20)));
        when(rolePermissionMapper.selectList(any())).thenReturn(List.of(rolePermission(101L), rolePermission(102L)));
        when(permissionMapper.selectBatchIds(any())).thenReturn(List.of(
                permission(101L, "menu:tickets", 10),
                permission(102L, "ticket:read", 20)
        ));
        PermissionService.PermissionContext context = permissionService.getCurrentPermissions(agent);

        assertEquals(2L, context.userId());
        assertTrue(context.hasRole("AGENT"));
        assertTrue(context.hasPermission("menu:tickets"));
        assertTrue(context.hasPermission("ticket:read"));
        assertTrue(context.dataScopes().isEmpty());
    }

    @Test
    void getUserPermissions_whenUserRoleMissing_shouldFallbackToLegacyRoleCode() {
        when(userRoleMapper.selectList(any())).thenReturn(List.of());
        when(roleMapper.selectOne(any())).thenReturn(role(10L, "ADMIN", 10));
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(role(10L, "ADMIN", 10)));
        when(rolePermissionMapper.selectList(any())).thenReturn(List.of(rolePermission(1001L)));
        when(permissionMapper.selectBatchIds(any())).thenReturn(List.of(permission(1001L, "user:read", 10)));
        PermissionService.PermissionContext context = permissionService.getUserPermissions(99L, "ADMIN");

        assertTrue(context.hasRole("admin"));
        assertTrue(context.hasPermission("user:read"));
        assertTrue(context.dataScopes().isEmpty());
    }

    @Test
    void getUserPermissions_whenNoRoleResolved_shouldReturnEmptyContext() {
        when(userRoleMapper.selectList(any())).thenReturn(List.of());
        when(roleMapper.selectOne(any())).thenReturn(null);

        PermissionService.PermissionContext context = permissionService.getUserPermissions(99L, "UNKNOWN");

        assertEquals(99L, context.userId());
        assertTrue(context.roles().isEmpty());
        assertTrue(context.permissions().isEmpty());
        assertTrue(context.dataScopes().isEmpty());
        verify(rolePermissionMapper, never()).selectList(any());
    }

    @Test
    void getCurrentPermissions_whenPrincipalMissing_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> permissionService.getCurrentPermissions(null));

        assertTrue(ex.getMessage().contains("未登录"));
    }

    private RoleEntity role(Long id, String roleCode, Integer sortOrder) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setEnabled(true);
        role.setSortOrder(sortOrder);
        return role;
    }

    private UserRoleEntity userRole(Long roleId) {
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setRoleId(roleId);
        userRole.setPrimaryRole(true);
        return userRole;
    }

    private RolePermissionEntity rolePermission(Long permissionId) {
        RolePermissionEntity rolePermission = new RolePermissionEntity();
        rolePermission.setPermissionId(permissionId);
        return rolePermission;
    }

    private PermissionEntity permission(Long id, String permissionCode, Integer sortOrder) {
        PermissionEntity permission = new PermissionEntity();
        permission.setId(id);
        permission.setPermissionCode(permissionCode);
        permission.setEnabled(true);
        permission.setSortOrder(sortOrder);
        return permission;
    }

}
