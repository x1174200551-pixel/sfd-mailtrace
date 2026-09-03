package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleMapper;
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
    private AuthorizationCacheQueryService authorizationCacheQueryService;
    @Mock
    private RolePermissionCacheQueryService rolePermissionCacheQueryService;
    @InjectMocks
    private PermissionService permissionService;

    private final CurrentUserPrincipal agent = new CurrentUserPrincipal(
            2L, "agent", "处理人", "agent@example.com", "AGENT");

    @Test
    void getCurrentPermissions_shouldResolveRolesAndPermissionsWithoutLegacyDataScopes() {
        when(authorizationCacheQueryService.getPrimaryRoleId(2L)).thenReturn(20L);
        when(authorizationCacheQueryService.getRoleAuthorization(20L))
                .thenReturn(roleAuthorization(20L, "AGENT", 20));
        when(rolePermissionCacheQueryService.getPermissionCodes(20L))
                .thenReturn(java.util.Set.of("menu:tickets", "ticket:read"));
        PermissionService.PermissionContext context = permissionService.getCurrentPermissions(agent);

        assertEquals(2L, context.userId());
        assertTrue(context.hasRole("AGENT"));
        assertTrue(context.hasPermission("menu:tickets"));
        assertTrue(context.hasPermission("ticket:read"));
        assertTrue(context.dataScopes().isEmpty());
    }

    @Test
    void getUserPermissions_whenUserRoleMissing_shouldFallbackToLegacyRoleCode() {
        when(authorizationCacheQueryService.getPrimaryRoleId(99L)).thenReturn(null);
        when(roleMapper.selectOne(any())).thenReturn(role(10L, "ADMIN", 10));
        when(authorizationCacheQueryService.getRoleAuthorization(10L))
                .thenReturn(roleAuthorization(10L, "ADMIN", 10));
        when(rolePermissionCacheQueryService.getPermissionCodes(10L)).thenReturn(java.util.Set.of("user:read"));
        PermissionService.PermissionContext context = permissionService.getUserPermissions(99L, "ADMIN");

        assertTrue(context.hasRole("admin"));
        assertTrue(context.hasPermission("user:read"));
        assertTrue(context.dataScopes().isEmpty());
    }

    @Test
    void getUserPermissions_whenNoRoleResolved_shouldReturnEmptyContext() {
        when(authorizationCacheQueryService.getPrimaryRoleId(99L)).thenReturn(null);
        when(roleMapper.selectOne(any())).thenReturn(null);

        PermissionService.PermissionContext context = permissionService.getUserPermissions(99L, "UNKNOWN");

        assertEquals(99L, context.userId());
        assertTrue(context.roles().isEmpty());
        assertTrue(context.permissions().isEmpty());
        assertTrue(context.dataScopes().isEmpty());
        verify(rolePermissionCacheQueryService, never()).getPermissionCodes(any());
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

    private AuthorizationCacheQueryService.RoleAuthorization roleAuthorization(
            Long id, String roleCode, Integer sortOrder) {
        return new AuthorizationCacheQueryService.RoleAuthorization(id, roleCode, true, sortOrder);
    }

}
