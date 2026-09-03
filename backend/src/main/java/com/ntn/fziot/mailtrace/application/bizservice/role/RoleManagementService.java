package com.ntn.fziot.mailtrace.application.bizservice.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.application.bizservice.security.OperationLogService;
import com.ntn.fziot.mailtrace.infrastructure.cache.MtRedisCacheDoubleDelete;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.role.PermissionTreeNodeVO;
import com.ntn.fziot.mailtrace.interfaces.vo.role.RoleEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.role.RoleListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.role.RolePermissionSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.role.RoleSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.role.RoleVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.PermissionEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RolePermissionEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserRoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.PermissionMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RolePermissionMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleManagementService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_FORBIDDEN = 40302;
    private static final int CODE_NOT_FOUND = 40401;
    private static final int CODE_CONFLICT = 40901;
    private static final Set<String> SYSTEM_ROLE_CODES = Set.of("ADMIN", "AGENT", "SUPERVISOR");

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final OperationLogService operationLogService;
    private final PermissionService permissionService;

    /**
     * 查询角色列表和页面统计。
     */
    public RoleListResponse listRoles(CurrentUserPrincipal principal, String keyword, Boolean enabled) {
        // 1、校验当前用户具备角色查看权限
        permissionService.assertPermission(principal, "role:read", "无权查看角色管理");
        // 2、按关键字和启用状态构建查询条件
        String normalizedKeyword = normalize(keyword);
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        if (!normalizedKeyword.isEmpty()) {
            wrapper.and(query -> query
                    .like(RoleEntity::getRoleName, normalizedKeyword)
                    .or()
                    .like(RoleEntity::getRoleCode, normalizedKeyword)
                    .or()
                    .like(RoleEntity::getRoleDesc, normalizedKeyword));
        }
        if (enabled != null) {
            wrapper.eq(RoleEntity::getEnabled, enabled);
        }
        wrapper.orderByAsc(RoleEntity::getSortOrder).orderByAsc(RoleEntity::getId);

        // 3、查询角色主表并补齐权限、数据范围和关联用户数
        List<RoleEntity> roles = roleMapper.selectList(wrapper);
        List<RoleVO> records = roles.stream().map(this::toVO).toList();
        // 4、汇总角色页顶部统计
        long total = roleMapper.selectCount(new LambdaQueryWrapper<>());
        long enabledCount = roleMapper.selectCount(new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getEnabled, true));
        long systemCount = roleMapper.selectCount(new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getSystemRole, true));
        long permissionTotal = permissionMapper.selectCount(new LambdaQueryWrapper<PermissionEntity>().eq(PermissionEntity::getEnabled, true));
        long userTotal = userRoleMapper.selectCount(new LambdaQueryWrapper<>());
        // 5、返回页面列表响应
        return new RoleListResponse(records, total, enabledCount, systemCount, total - systemCount, permissionTotal, userTotal);
    }

    /**
     * 查询可配置权限树。
     */
    public List<PermissionTreeNodeVO> listPermissionTree(CurrentUserPrincipal principal) {
        // 1、校验当前用户具备角色查看权限
        permissionService.assertPermission(principal, "role:read", "无权查看角色管理");
        // 2、查询所有启用权限并按排序值稳定排序
        List<PermissionEntity> permissions = permissionMapper.selectList(new LambdaQueryWrapper<PermissionEntity>()
                .eq(PermissionEntity::getEnabled, true)
                .orderByAsc(PermissionEntity::getSortOrder)
                .orderByAsc(PermissionEntity::getId));
        // 3、按 parentId 组装菜单/动作权限树
        Map<Long, List<PermissionEntity>> childrenByParent = permissions.stream()
                .filter(permission -> permission.getParentId() != null)
                .collect(Collectors.groupingBy(PermissionEntity::getParentId, LinkedHashMap::new, Collectors.toList()));
        return permissions.stream()
                .filter(permission -> permission.getParentId() == null)
                .map(permission -> toPermissionNode(permission, childrenByParent))
                .toList();
    }

    /**
     * 新建自定义角色。
     */
    @Transactional
    public RoleVO createRole(CurrentUserPrincipal principal, RoleSaveRequest request) {
        // 1、校验当前用户具备角色新建权限
        permissionService.assertPermission(principal, "role:create", "无权新建角色");
        // 2、校验角色名称、编码和唯一性
        String roleName = requireRoleName(request.getRoleName());
        String roleCode = normalizeRoleCode(request.getRoleCode());
        if (roleCode.isEmpty()) {
            roleCode = generateRoleCode();
        }
        assertCustomRoleCode(roleCode);
        ensureRoleCodeUnique(roleCode);

        // 3、写入角色主表，系统内置标记固定为否
        RoleEntity role = new RoleEntity();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setRoleDesc(normalize(request.getRoleDesc()));
        role.setSystemRole(false);
        role.setEnabled(request.getEnabled() == null || request.getEnabled());
        role.setSortOrder(nextSortOrder());
        role.setCreatedBy(principal.account());
        role.setUpdatedBy(principal.account());
        roleMapper.insert(role);
        List<PermissionEntity> defaultPermissions = loadEnabledPermissions();
        assertPermissionsGrantable(principal, defaultPermissions);
        assignPermissions(role.getId(), defaultPermissions, principal.account(), false);

        // 4、记录操作日志并返回角色详情
        operationLogService.record(principal, "ROLE", "CREATE", role.getId(), "新建角色：" + roleName);
        return toVO(roleMapper.selectById(role.getId()));
    }

    /**
     * 编辑自定义角色基础信息。
     */
    @Transactional
    @MtRedisCacheDoubleDelete(cacheName = "role-authorization", key = "#id", delayMillis = 500)
    public RoleVO updateRole(CurrentUserPrincipal principal, Long id, RoleSaveRequest request) {
        // 1、校验当前用户具备角色编辑权限
        permissionService.assertPermission(principal, "role:update", "无权编辑角色");
        // 2、查询角色并校验内置角色只读保护
        RoleEntity existing = requireRole(id);
        assertCustomRole(existing, "内置角色不可编辑");
        // 3、校验角色名称和启用状态
        String roleName = requireRoleName(request.getRoleName());
        Boolean enabled = request.getEnabled() == null || request.getEnabled();

        // 4、更新角色基础信息
        roleMapper.update(null, new LambdaUpdateWrapper<RoleEntity>()
                .eq(RoleEntity::getId, id)
                .set(RoleEntity::getRoleName, roleName)
                .set(RoleEntity::getRoleDesc, normalize(request.getRoleDesc()))
                .set(RoleEntity::getEnabled, enabled)
                .set(RoleEntity::getUpdatedBy, principal.account()));

        // 5、记录操作日志并返回角色详情
        operationLogService.record(principal, "ROLE", "UPDATE", id, "编辑角色：" + existing.getRoleName());
        return toVO(roleMapper.selectById(id));
    }

    /**
     * 启用或停用自定义角色。
     */
    @Transactional
    @MtRedisCacheDoubleDelete(cacheName = "role-authorization", key = "#id", delayMillis = 500)
    public RoleVO updateEnabled(CurrentUserPrincipal principal, Long id, RoleEnabledRequest request) {
        // 1、校验当前用户具备角色启停权限
        permissionService.assertPermission(principal, "role:enable", "无权启停角色");
        // 2、查询角色并校验内置角色保护
        RoleEntity existing = requireRole(id);
        assertCustomRole(existing, "内置角色不可停用");
        // 3、更新启用状态
        roleMapper.update(null, new LambdaUpdateWrapper<RoleEntity>()
                .eq(RoleEntity::getId, id)
                .set(RoleEntity::getEnabled, request.getEnabled())
                .set(RoleEntity::getUpdatedBy, principal.account()));
        // 4、记录操作日志并返回角色详情
        operationLogService.record(principal, "ROLE", Boolean.TRUE.equals(request.getEnabled()) ? "ENABLE" : "DISABLE", id,
                (Boolean.TRUE.equals(request.getEnabled()) ? "启用角色：" : "停用角色：") + existing.getRoleName());
        return toVO(roleMapper.selectById(id));
    }

    /**
     * 保存自定义角色的功能权限清单。旧 dataScopes 请求字段兼容接收但不再校验或写表。
     */
    @MtRedisCacheDoubleDelete(cacheName = "role-permission-codes", key = "#id", delayMillis = 500)
    @Transactional
    public RoleVO saveRolePermissions(CurrentUserPrincipal principal, Long id, RolePermissionSaveRequest request) {
        // 1、校验当前用户具备角色权限配置权限
        permissionService.assertPermission(principal, "role:permission_update", "无权配置角色权限");
        // 2、查询角色并校验内置角色只读保护
        RoleEntity role = requireRole(id);
        assertCustomRole(role, "内置角色不可配置权限");
        // 3、校验权限编码均存在且启用
        List<PermissionEntity> selectedPermissions = requirePermissionsWithAncestors(request.getPermissionCodes());
        assertPermissionsGrantable(principal, selectedPermissions);
        // 4、替换角色权限关系；P2-CUTOVER 后不再读写 mt_role_data_scope
        assignPermissions(id, selectedPermissions, principal.account(), true);

        // 5、记录操作日志并返回最新角色详情
        operationLogService.record(principal, "ROLE", "PERMISSION_UPDATE", id, "配置角色权限：" + role.getRoleName());
        return toVO(roleMapper.selectById(id));
    }

    private RoleVO toVO(RoleEntity role) {
        if (role == null) {
            return null;
        }
        return new RoleVO(
                role.getId(),
                role.getRoleCode(),
                role.getRoleName(),
                role.getRoleDesc(),
                role.getSystemRole(),
                role.getEnabled(),
                role.getSortOrder(),
                permissionCodes(role.getId()),
                List.of(),
                userCount(role.getId()),
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    private PermissionTreeNodeVO toPermissionNode(PermissionEntity permission, Map<Long, List<PermissionEntity>> childrenByParent) {
        List<PermissionTreeNodeVO> children = childrenByParent.getOrDefault(permission.getId(), List.of()).stream()
                .sorted(Comparator.comparing(PermissionEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(PermissionEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(child -> toPermissionNode(child, childrenByParent))
                .toList();
        return new PermissionTreeNodeVO(
                permission.getId(),
                permission.getPermissionCode(),
                permission.getPermissionName(),
                permission.getPermissionType(),
                permission.getModuleCode(),
                permission.getParentId(),
                children
        );
    }

    private List<String> permissionCodes(Long roleId) {
        List<RolePermissionEntity> rows = rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermissionEntity>()
                .eq(RolePermissionEntity::getRoleId, roleId));
        Set<Long> permissionIds = rows.stream()
                .map(RolePermissionEntity::getPermissionId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectBatchIds(permissionIds).stream()
                .filter(permission -> permission != null && Boolean.TRUE.equals(permission.getEnabled()))
                .sorted(Comparator.comparing(PermissionEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(PermissionEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(PermissionEntity::getPermissionCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
    }

    private Long userCount(Long roleId) {
        return userRoleMapper.selectCount(new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getRoleId, roleId));
    }

    private List<PermissionEntity> requirePermissionsWithAncestors(List<String> permissionCodes) {
        Set<String> codes = (permissionCodes == null ? List.<String>of() : permissionCodes).stream()
                .map(this::normalize)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (codes.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "请选择权限");
        }
        List<PermissionEntity> enabledPermissions = loadEnabledPermissions();
        Map<String, PermissionEntity> permissionByCode = enabledPermissions.stream()
                .collect(Collectors.toMap(PermissionEntity::getPermissionCode, permission -> permission));
        Map<Long, PermissionEntity> permissionById = enabledPermissions.stream()
                .collect(Collectors.toMap(PermissionEntity::getId, permission -> permission));
        List<String> missing = codes.stream().filter(code -> !permissionByCode.containsKey(code)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "权限不存在或已停用：" + String.join(",", missing));
        }

        Set<Long> selectedIds = new LinkedHashSet<>();
        for (String code : codes) {
            PermissionEntity current = permissionByCode.get(code);
            while (current != null && selectedIds.add(current.getId())) {
                Long parentId = current.getParentId();
                if (parentId == null) {
                    break;
                }
                current = permissionById.get(parentId);
                if (current == null) {
                    throw new BusinessException(CODE_BAD_REQUEST, "权限树缺少有效父节点：" + code);
                }
            }
        }
        return enabledPermissions.stream()
                .filter(permission -> selectedIds.contains(permission.getId()))
                .sorted(Comparator.comparing(PermissionEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(PermissionEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private List<PermissionEntity> loadEnabledPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<PermissionEntity>()
                        .eq(PermissionEntity::getEnabled, true)
                        .orderByAsc(PermissionEntity::getSortOrder)
                        .orderByAsc(PermissionEntity::getId))
                .stream()
                .filter(permission -> permission != null && permission.getId() != null)
                .toList();
    }

    private void assertPermissionsGrantable(CurrentUserPrincipal principal,
                                            List<PermissionEntity> selectedPermissions) {
        if (permissionService.hasRole(principal, "ADMIN")) {
            return;
        }
        Set<String> operatorPermissions = permissionService.getCurrentPermissions(principal).permissions();
        List<String> exceededPermissions = selectedPermissions.stream()
                .map(PermissionEntity::getPermissionCode)
                .filter(code -> !operatorPermissions.contains(code))
                .toList();
        if (!exceededPermissions.isEmpty()) {
            throw new BusinessException(CODE_FORBIDDEN,
                    "不能授予超出当前账号范围的权限：" + String.join(",", exceededPermissions));
        }
    }

    private void assignPermissions(Long roleId, List<PermissionEntity> permissions,
                                   String operator, boolean replaceExisting) {
        if (replaceExisting) {
            rolePermissionMapper.physicalDeleteByRoleId(roleId);
        }
        for (PermissionEntity permission : permissions) {
            RolePermissionEntity row = new RolePermissionEntity();
            row.setRoleId(roleId);
            row.setPermissionId(permission.getId());
            row.setCreatedBy(operator);
            row.setUpdatedBy(operator);
            rolePermissionMapper.insert(row);
        }
    }

    private RoleEntity requireRole(Long id) {
        if (id == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "角色ID不能为空");
        }
        RoleEntity role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(CODE_NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private String requireRoleName(String value) {
        String roleName = normalize(value);
        if (roleName.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "请输入角色名称");
        }
        return roleName;
    }

    private void assertCustomRole(RoleEntity role, String message) {
        if (Boolean.TRUE.equals(role.getSystemRole()) || SYSTEM_ROLE_CODES.contains(normalizeUpper(role.getRoleCode()))) {
            throw new BusinessException(CODE_FORBIDDEN, message);
        }
    }

    private void assertCustomRoleCode(String roleCode) {
        if (SYSTEM_ROLE_CODES.contains(normalizeUpper(roleCode))) {
            throw new BusinessException(CODE_BAD_REQUEST, "系统内置角色编码不可用于自定义角色");
        }
    }

    private void ensureRoleCodeUnique(String roleCode) {
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getRoleCode, roleCode));
        if (count != null && count > 0) {
            throw new BusinessException(CODE_CONFLICT, "角色编码已存在");
        }
    }

    private Integer nextSortOrder() {
        RoleEntity latest = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .orderByDesc(RoleEntity::getSortOrder)
                .orderByDesc(RoleEntity::getId)
                .last("LIMIT 1"));
        return latest == null || latest.getSortOrder() == null ? 100 : latest.getSortOrder() + 10;
    }

    private String generateRoleCode() {
        return "CUSTOM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String normalizeRoleCode(String value) {
        return normalize(value).toUpperCase();
    }

    private String normalizeUpper(String value) {
        return normalize(value).toUpperCase();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

}
