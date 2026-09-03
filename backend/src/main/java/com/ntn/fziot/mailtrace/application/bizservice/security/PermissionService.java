package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleMapper;
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
    private final AuthorizationCacheQueryService authorizationCacheQueryService;
    private final RolePermissionCacheQueryService rolePermissionCacheQueryService;

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

        List<AuthorizationCacheQueryService.RoleAuthorization> roles = roleIds.stream()
                .map(authorizationCacheQueryService::getRoleAuthorization)
                .filter(this::isEnabled)
                .sorted(Comparator.comparing(AuthorizationCacheQueryService.RoleAuthorization::sortOrder,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(AuthorizationCacheQueryService.RoleAuthorization::id,
                                Comparator.nullsLast(Long::compareTo)))
                .toList();
        Set<Long> enabledRoleIds = roles.stream()
                .map(AuthorizationCacheQueryService.RoleAuthorization::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (enabledRoleIds.isEmpty()) {
            return PermissionContext.empty(userId);
        }

        Set<String> roleCodes = roles.stream()
                .map(AuthorizationCacheQueryService.RoleAuthorization::roleCode)
                .map(this::normalizeUpper)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> permissionCodes = resolvePermissionCodes(enabledRoleIds);
        // P2-CUTOVER 后角色只承载功能权限，旧数据范围返回空对象兼容现有响应结构。
        return new PermissionContext(userId, roleCodes, permissionCodes, Map.of());
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
            Long primaryRoleId = authorizationCacheQueryService.getPrimaryRoleId(userId);
            if (primaryRoleId != null) {
                roleIds.add(primaryRoleId);
            }
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
        return roleIds.stream()
                .flatMap(roleId -> rolePermissionCacheQueryService.getPermissionCodes(roleId).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isEnabled(AuthorizationCacheQueryService.RoleAuthorization role) {
        return role != null && Boolean.TRUE.equals(role.enabled());
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
