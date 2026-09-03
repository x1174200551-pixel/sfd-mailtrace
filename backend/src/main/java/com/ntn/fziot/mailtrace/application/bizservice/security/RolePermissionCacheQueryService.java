package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.infrastructure.cache.MtRedisCacheable;
import com.ntn.fziot.mailtrace.repox.mysql.entity.PermissionEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RolePermissionEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.PermissionMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionCacheQueryService {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @MtRedisCacheable(cacheName = "role-permission-codes", key = "#roleId", ttlSeconds = 600)
    public Set<String> getPermissionCodes(Long roleId) {
        if (roleId == null) {
            return Set.of();
        }
        List<RolePermissionEntity> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermissionEntity>().eq(RolePermissionEntity::getRoleId, roleId));
        Set<Long> permissionIds = rolePermissions.stream()
                .map(RolePermissionEntity::getPermissionId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (permissionIds.isEmpty()) {
            return Set.of();
        }

        Set<String> permissionCodes = permissionMapper.selectBatchIds(permissionIds).stream()
                .filter(permission -> permission != null && Boolean.TRUE.equals(permission.getEnabled()))
                .sorted(Comparator.comparing(PermissionEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(PermissionEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(PermissionEntity::getPermissionCode)
                .map(this::normalizePermissionCode)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(permissionCodes);
    }

    private String normalizePermissionCode(String value) {
        return value == null ? "" : value.trim();
    }
}
