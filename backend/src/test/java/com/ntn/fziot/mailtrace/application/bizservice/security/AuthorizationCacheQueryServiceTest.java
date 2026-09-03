package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.ntn.fziot.mailtrace.infrastructure.cache.MtRedisCacheable;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserRoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationCacheQueryServiceTest {

    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private RoleMapper roleMapper;
    @InjectMocks
    private AuthorizationCacheQueryService service;

    @Test
    void getPrimaryRoleId_shouldReturnFirstOrderedRole() throws Exception {
        UserRoleEntity relation = new UserRoleEntity();
        relation.setRoleId(20L);
        relation.setPrimaryRole(true);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(relation));

        assertEquals(20L, service.getPrimaryRoleId(2L));
        assertCache("getPrimaryRoleId", Long.class, "user-primary-role-id", 600);
    }

    @Test
    void getRoleAuthorization_shouldReturnSafeRoleSnapshot() throws Exception {
        RoleEntity role = new RoleEntity();
        role.setId(20L);
        role.setRoleCode("AGENT");
        role.setEnabled(true);
        role.setSortOrder(20);
        when(roleMapper.selectById(20L)).thenReturn(role);

        AuthorizationCacheQueryService.RoleAuthorization result = service.getRoleAuthorization(20L);

        assertEquals(20L, result.id());
        assertEquals("AGENT", result.roleCode());
        assertEquals(true, result.enabled());
        assertCache("getRoleAuthorization", Long.class, "role-authorization", 600);
    }

    @Test
    void nullIds_shouldNotQueryDatabase() {
        assertNull(service.getPrimaryRoleId(null));
        assertNull(service.getRoleAuthorization(null));
    }

    private void assertCache(String methodName, Class<?> parameterType,
                             String cacheName, long ttlSeconds) throws Exception {
        Method method = AuthorizationCacheQueryService.class.getMethod(methodName, parameterType);
        MtRedisCacheable annotation = method.getAnnotation(MtRedisCacheable.class);
        assertEquals(cacheName, annotation.cacheName());
        assertEquals(ttlSeconds, annotation.ttlSeconds());
    }
}
