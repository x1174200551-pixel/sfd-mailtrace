package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.ntn.fziot.mailtrace.repox.mysql.entity.PermissionEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RolePermissionEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.PermissionMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RolePermissionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolePermissionCacheQueryServiceTest {

    @Mock
    private PermissionMapper permissionMapper;
    @Mock
    private RolePermissionMapper rolePermissionMapper;
    @InjectMocks
    private RolePermissionCacheQueryService service;

    @Test
    void getPermissionCodes_shouldReturnEnabledCodesInStableOrder() {
        when(rolePermissionMapper.selectList(any())).thenReturn(List.of(rolePermission(101L), rolePermission(102L)));
        PermissionEntity disabled = permission(101L, "disabled:read", 10, false);
        PermissionEntity enabled = permission(102L, " ticket:read ", 20, true);
        when(permissionMapper.selectBatchIds(any())).thenReturn(List.of(enabled, disabled));

        Set<String> result = service.getPermissionCodes(20L);

        assertEquals(List.of("ticket:read"), result.stream().toList());
    }

    @Test
    void getPermissionCodes_whenRoleHasNoPermission_shouldSkipPermissionQuery() {
        when(rolePermissionMapper.selectList(any())).thenReturn(List.of());

        assertEquals(Set.of(), service.getPermissionCodes(20L));

        verify(permissionMapper, never()).selectBatchIds(any());
    }

    private RolePermissionEntity rolePermission(Long permissionId) {
        RolePermissionEntity rolePermission = new RolePermissionEntity();
        rolePermission.setPermissionId(permissionId);
        return rolePermission;
    }

    private PermissionEntity permission(Long id, String code, Integer sortOrder, boolean enabled) {
        PermissionEntity permission = new PermissionEntity();
        permission.setId(id);
        permission.setPermissionCode(code);
        permission.setSortOrder(sortOrder);
        permission.setEnabled(enabled);
        return permission;
    }
}
