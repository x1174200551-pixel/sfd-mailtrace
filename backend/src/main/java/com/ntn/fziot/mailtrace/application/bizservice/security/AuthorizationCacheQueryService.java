package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.infrastructure.cache.MtRedisCacheable;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserRoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 认证链路中的低频变更数据查询入口，独立成 Spring Bean 以确保缓存切面生效。
 */
@Service
@RequiredArgsConstructor
public class AuthorizationCacheQueryService {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    @MtRedisCacheable(cacheName = "user-primary-role-id", key = "#userId", ttlSeconds = 600)
    public Long getPrimaryRoleId(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                        .eq(UserRoleEntity::getUserId, userId)
                        .orderByDesc(UserRoleEntity::getPrimaryRole)
                        .orderByAsc(UserRoleEntity::getId))
                .stream()
                .map(UserRoleEntity::getRoleId)
                .filter(roleId -> roleId != null)
                .findFirst()
                .orElse(null);
    }

    @MtRedisCacheable(cacheName = "role-authorization", key = "#roleId", ttlSeconds = 600)
    public RoleAuthorization getRoleAuthorization(Long roleId) {
        if (roleId == null) {
            return null;
        }
        RoleEntity role = roleMapper.selectById(roleId);
        if (role == null) {
            return null;
        }
        return new RoleAuthorization(role.getId(), role.getRoleCode(), role.getEnabled(), role.getSortOrder());
    }

    public record RoleAuthorization(Long id, String roleCode, Boolean enabled, Integer sortOrder) {
    }
}
