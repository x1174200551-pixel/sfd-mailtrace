package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.PermissionEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleDataScopeEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RolePermissionEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserRoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.PermissionMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleDataScopeMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RolePermissionMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private static final int CODE_FORBIDDEN = 40302;
    private static final String ATTR_PERMISSION_CONTEXT = PermissionService.class.getName() + ".PERMISSION_CONTEXT";

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleDataScopeMapper roleDataScopeMapper;

    public PermissionContext getCurrentPermissions(CurrentUserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(CODE_FORBIDDEN, "未登录");
        }
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes attributes)) {
            return getUserPermissions(principal.id(), principal.roleCode());
        }
        Object cached = attributes.getRequest().getAttribute(ATTR_PERMISSION_CONTEXT);
        if (cached instanceof PermissionContext context && Objects.equals(principal.id(), context.userId())) {
            return context;
        }
        PermissionContext context = getUserPermissions(principal.id(), principal.roleCode());
        attributes.getRequest().setAttribute(ATTR_PERMISSION_CONTEXT, context);
        return context;
    }

    public PermissionContext getUserPermissions(Long userId, String fallbackRoleCode) {
        Set<Long> roleIds = resolveRoleIds(userId, fallbackRoleCode);
        if (roleIds.isEmpty()) {
            return PermissionContext.empty(userId);
        }

        List<RoleEntity> roles = roleMapper.selectBatchIds(roleIds).stream()
                .filter(this::isEnabled)
                .sorted(Comparator.comparing(RoleEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RoleEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        Set<Long> enabledRoleIds = roles.stream().map(RoleEntity::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (enabledRoleIds.isEmpty()) {
            return PermissionContext.empty(userId);
        }

        Set<String> roleCodes = roles.stream()
                .map(RoleEntity::getRoleCode)
                .map(this::normalizeUpper)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> permissionCodes = resolvePermissionCodes(enabledRoleIds);
        Map<String, Set<String>> dataScopes = resolveDataScopes(enabledRoleIds);

        return new PermissionContext(userId, roleCodes, permissionCodes, dataScopes);
    }

    public boolean hasPermission(CurrentUserPrincipal principal, String permissionCode) {
        String normalized = normalizePermissionCode(permissionCode);
        return !normalized.isEmpty() && getCurrentPermissions(principal).hasPermission(normalized);
    }

    public void assertPermission(CurrentUserPrincipal principal, String permissionCode, String forbiddenMessage) {
        if (principal == null) {
            throw new BusinessException(CODE_FORBIDDEN, "未登录");
        }
        if (!hasPermission(principal, permissionCode)) {
            throw new BusinessException(CODE_FORBIDDEN, forbiddenMessage);
        }
    }

    public boolean hasRole(CurrentUserPrincipal principal, String roleCode) {
        String normalized = normalizeUpper(roleCode);
        return !normalized.isEmpty() && getCurrentPermissions(principal).hasRole(normalized);
    }

    private Set<Long> resolveRoleIds(Long userId, String fallbackRoleCode) {
        Set<Long> roleIds = new LinkedHashSet<>();
        if (userId != null) {
            userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                            .eq(UserRoleEntity::getUserId, userId)
                            .orderByDesc(UserRoleEntity::getPrimaryRole)
                            .orderByAsc(UserRoleEntity::getId))
                    .stream()
                    .map(UserRoleEntity::getRoleId)
                    .filter(id -> id != null)
                    .findFirst()
                    .ifPresent(roleIds::add);
        }
        if (roleIds.isEmpty()) {
            RoleEntity fallbackRole = findRoleByCode(fallbackRoleCode);
            if (fallbackRole != null && fallbackRole.getId() != null) {
                roleIds.add(fallbackRole.getId());
            }
        }
        return roleIds;
    }

    private RoleEntity findRoleByCode(String roleCode) {
        String normalized = normalizeUpper(roleCode);
        if (normalized.isEmpty()) {
            return null;
        }
        return roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getRoleCode, normalized)
                .last("LIMIT 1"));
    }

    private Set<String> resolvePermissionCodes(Set<Long> roleIds) {
        List<RolePermissionEntity> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermissionEntity>().in(RolePermissionEntity::getRoleId, roleIds));
        Set<Long> permissionIds = rolePermissions.stream()
                .map(RolePermissionEntity::getPermissionId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (permissionIds.isEmpty()) {
            return Set.of();
        }

        return permissionMapper.selectBatchIds(permissionIds).stream()
                .filter(this::isEnabled)
                .sorted(Comparator.comparing(PermissionEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(PermissionEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(PermissionEntity::getPermissionCode)
                .map(this::normalizePermissionCode)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, Set<String>> resolveDataScopes(Set<Long> roleIds) {
        List<RoleDataScopeEntity> rows = roleDataScopeMapper.selectList(
                new LambdaQueryWrapper<RoleDataScopeEntity>().in(RoleDataScopeEntity::getRoleId, roleIds));
        Map<String, Set<String>> grouped = new LinkedHashMap<>();
        for (RoleDataScopeEntity row : rows) {
            String resourceType = normalizeUpper(row.getResourceType());
            String scopeCode = normalizeUpper(row.getScopeCode());
            if (resourceType.isEmpty() || scopeCode.isEmpty()) {
                continue;
            }
            grouped.computeIfAbsent(resourceType, key -> new LinkedHashSet<>()).add(scopeCode);
        }

        Map<String, Set<String>> immutable = new LinkedHashMap<>();
        grouped.forEach((resourceType, scopes) -> immutable.put(resourceType, Collections.unmodifiableSet(scopes)));
        return Collections.unmodifiableMap(immutable);
    }

    private boolean isEnabled(RoleEntity role) {
        return role != null && Boolean.TRUE.equals(role.getEnabled());
    }

    private boolean isEnabled(PermissionEntity permission) {
        return permission != null && Boolean.TRUE.equals(permission.getEnabled());
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String normalizePermissionCode(String value) {
        return value == null ? "" : value.trim();
    }

    public record PermissionContext(
            Long userId,
            Set<String> roles,
            Set<String> permissions,
            Map<String, Set<String>> dataScopes
    ) {
        public PermissionContext {
            roles = Collections.unmodifiableSet(new LinkedHashSet<>(roles == null ? Set.of() : roles));
            permissions = Collections.unmodifiableSet(new LinkedHashSet<>(permissions == null ? Set.of() : permissions));
            dataScopes = immutableDataScopes(dataScopes);
        }

        public static PermissionContext empty(Long userId) {
            return new PermissionContext(userId, Set.of(), Set.of(), Map.of());
        }

        public boolean hasRole(String roleCode) {
            return roles.contains(normalizeStatic(roleCode));
        }

        public boolean hasPermission(String permissionCode) {
            return permissions.contains(normalizePermissionCodeStatic(permissionCode));
        }

        public boolean hasDataScope(String resourceType, String scopeCode) {
            Set<String> scopes = dataScopes.get(normalizeStatic(resourceType));
            return scopes != null && scopes.contains(normalizeStatic(scopeCode));
        }

        private static Map<String, Set<String>> immutableDataScopes(Map<String, Set<String>> source) {
            if (source == null || source.isEmpty()) {
                return Map.of();
            }
            Map<String, Set<String>> result = new LinkedHashMap<>();
            source.forEach((resourceType, scopes) -> {
                String normalizedResource = normalizeStatic(resourceType);
                if (normalizedResource.isEmpty()) {
                    return;
                }
                Set<String> normalizedScopes = (scopes == null ? Set.<String>of() : scopes).stream()
                        .map(PermissionContext::normalizeStatic)
                        .filter(scope -> !scope.isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                result.put(normalizedResource, Collections.unmodifiableSet(normalizedScopes));
            });
            return Collections.unmodifiableMap(result);
        }

        private static String normalizeStatic(String value) {
            return value == null ? "" : value.trim().toUpperCase();
        }

        private static String normalizePermissionCodeStatic(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
