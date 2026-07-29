package com.ntn.fziot.mailtrace.application.bizservice.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserCreateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserResetPasswordRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserSummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserUpdateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.DepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserDepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserRoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.DepartmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserDepartmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserRoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_FORBIDDEN = 40302;
    private static final int CODE_NOT_FOUND = 40401;
    private static final int CODE_CONFLICT = 40901;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_AGENT = "AGENT";
    private static final String DEFAULT_DEPT_CODE = "DEFAULT";

    private final UserMapper userMapper;
    private final OperationLogMapper operationLogMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final DepartmentMapper departmentMapper;
    private final UserDepartmentMapper userDepartmentMapper;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;

    /**
     * 分页查询用户列表。
     */
    public UserPageResponse pageUsers(CurrentUserPrincipal principal, String keyword, String roleCode, Boolean enabled,
                                      Integer page, Integer size) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "user:read", "无权查看用户管理");
        // 2、规范化分页参数，并按搜索、角色、启用状态构建查询条件
        long currentPage = normalizePage(page);
        long pageSize = normalizeSize(size);
        LambdaQueryWrapper<UserEntity> wrapper = buildQuery(keyword, roleCode, enabled)
                .orderByDesc(UserEntity::getUpdatedAt)
                .orderByDesc(UserEntity::getId);

        // 3、使用 MyBatis-Plus 分页拦截器执行分页查询
        Page<UserEntity> result = userMapper.selectPage(Page.of(currentPage, pageSize), wrapper);
        List<UserVO> records = result.getRecords().stream().map(this::toVO).toList();
        // 4、查询用户统计摘要并组装页面响应
        return new UserPageResponse(
                records,
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getPages(),
                buildSummary()
        );
    }

    /**
     * 查询可分配的处理人列表（ADMIN/AGENT 均可调用）。
     */
    public List<UserVO> listAssignableUsers(CurrentUserPrincipal principal) {
        assertAgentOrAdmin(principal);
        return userMapper.selectList(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getEnabled, true)
                        .orderByAsc(UserEntity::getDisplayName)
        ).stream().map(this::toVO).toList();
    }

    /**
     * 新建系统用户。
     */
    @Transactional
    public UserVO createUser(CurrentUserPrincipal principal, UserCreateRequest request) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "user:create", "无权新建用户");
        // 2、校验主角色、角色集合与账号唯一性
        String account = normalize(request.getAccount());
        String primaryRoleCode = normalizeRole(request.getRoleCode());
        List<String> roleCodes = normalizeAssignedRoleCodes(primaryRoleCode, request.getRoleCodes());
        List<RoleEntity> roles = requireRoles(roleCodes);
        DepartmentEntity department = requireTargetDepartment(request.getDepartmentId());
        ensureAccountUnique(account);

        // 3、使用 BCrypt 加密初始密码并写入用户表
        UserEntity user = new UserEntity();
        user.setAccount(account);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        user.setDisplayName(normalize(request.getDisplayName()));
        user.setEmail(normalize(request.getEmail()));
        user.setRoleCode(primaryRoleCode);
        user.setEnabled(request.getEnabled() == null || request.getEnabled());
        user.setCreatedBy(principal.account());
        user.setUpdatedBy(principal.account());
        userMapper.insert(user);
        syncUserRoles(user.getId(), primaryRoleCode, roles, principal.account());
        syncUserDepartment(user.getId(), department.getId(), principal.account());

        // 4、写入用户创建操作日志
        recordLog(principal, "CREATE", user.getId(), "新建用户：" + user.getAccount());
        // 5、返回新建后的用户详情
        return toVO(userMapper.selectById(user.getId()));
    }

    /**
     * 编辑用户基础资料和启用状态。
     */
    @Transactional
    public UserVO updateUser(CurrentUserPrincipal principal, Long id, UserUpdateRequest request) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "user:update", "无权编辑用户");
        // 2、查询目标用户并校验主角色和角色集合合法性
        UserEntity existing = requireUser(id);
        String primaryRoleCode = normalizeRole(request.getRoleCode());
        List<String> roleCodes = normalizeAssignedRoleCodes(primaryRoleCode, request.getRoleCodes());
        List<RoleEntity> roles = requireRoles(roleCodes);
        DepartmentEntity department = requireTargetDepartment(request.getDepartmentId());
        Boolean enabled = request.getEnabled() == null || request.getEnabled();
        // 3、防止管理员把自己降级或停用
        assertSelfGuard(principal, existing, primaryRoleCode, enabled);

        // 4、更新姓名、邮箱、角色、启用状态和更新人
        userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, id)
                .set(UserEntity::getDisplayName, normalize(request.getDisplayName()))
                .set(UserEntity::getEmail, normalize(request.getEmail()))
                .set(UserEntity::getRoleCode, primaryRoleCode)
                .set(UserEntity::getEnabled, enabled)
                .set(UserEntity::getUpdatedBy, principal.account()));
        syncUserRoles(id, primaryRoleCode, roles, principal.account());
        syncUserDepartment(id, department.getId(), principal.account());

        // 5、写入用户编辑操作日志并返回最新用户详情
        recordLog(principal, "UPDATE", id, "编辑用户：" + existing.getAccount());
        return toVO(userMapper.selectById(id));
    }

    /**
     * 启用或停用用户。
     */
    @Transactional
    public UserVO updateEnabled(CurrentUserPrincipal principal, Long id, UserEnabledRequest request) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "user:enable", "无权启停用户");
        // 2、查询目标用户并校验不能停用当前登录账号
        UserEntity existing = requireUser(id);
        Boolean enabled = request.getEnabled();
        assertSelfGuard(principal, existing, existing.getRoleCode(), enabled);

        // 3、更新启用状态和更新人
        userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, id)
                .set(UserEntity::getEnabled, enabled)
                .set(UserEntity::getUpdatedBy, principal.account()));

        // 4、写入启用或停用操作日志
        recordLog(principal, enabled ? "ENABLE" : "DISABLE", id,
                (enabled ? "启用用户：" : "停用用户：") + existing.getAccount());
        // 5、返回最新用户详情
        return toVO(userMapper.selectById(id));
    }

    /**
     * 重置用户密码。
     */
    @Transactional
    public void resetPassword(CurrentUserPrincipal principal, Long id, UserResetPasswordRequest request) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "user:reset_password", "无权重置用户密码");
        // 2、查询目标用户是否存在
        UserEntity existing = requireUser(id);
        // 3、使用 BCrypt 加密新密码并更新密码哈希
        userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, id)
                .set(UserEntity::getPasswordHash, passwordEncoder.encode(request.getPassword().trim()))
                .set(UserEntity::getUpdatedBy, principal.account()));

        // 4、写入重置密码操作日志
        recordLog(principal, "RESET_PASSWORD", id, "重置密码：" + existing.getAccount());
    }

    private LambdaQueryWrapper<UserEntity> buildQuery(String keyword, String roleCode, Boolean enabled) {
        String normalizedKeyword = normalize(keyword);
        String normalizedRole = normalize(roleCode);
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        if (!normalizedKeyword.isEmpty()) {
            wrapper.and(query -> query
                    .like(UserEntity::getAccount, normalizedKeyword)
                    .or()
                    .like(UserEntity::getDisplayName, normalizedKeyword)
                    .or()
                    .like(UserEntity::getEmail, normalizedKeyword));
        }
        if (!normalizedRole.isEmpty()) {
            requireRole(normalizedRole);
            wrapper.eq(UserEntity::getRoleCode, normalizeRole(normalizedRole));
        }
        if (enabled != null) {
            wrapper.eq(UserEntity::getEnabled, enabled);
        }
        return wrapper;
    }

    private UserSummaryVO buildSummary() {
        long total = userMapper.selectCount(new LambdaQueryWrapper<>());
        long enabled = userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getEnabled, true));
        long admin = userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getRoleCode, ROLE_ADMIN));
        long agent = userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getRoleCode, ROLE_AGENT));
        return new UserSummaryVO(total, enabled, total - enabled, admin, agent);
    }

    private UserEntity requireUser(Long id) {
        if (id == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "用户ID不能为空");
        }
        UserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(CODE_NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void ensureAccountUnique(String account) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getAccount, account));
        if (count != null && count > 0) {
            throw new BusinessException(CODE_CONFLICT, "账号已存在，请更换账号");
        }
    }

    private void assertAgentOrAdmin(CurrentUserPrincipal principal) {
        if (principal == null || (!ROLE_ADMIN.equals(principal.roleCode()) && !ROLE_AGENT.equals(principal.roleCode()))) {
            throw new BusinessException(CODE_FORBIDDEN, "无操作权限");
        }
    }

    private void syncUserRoles(Long userId, String primaryRoleCode, List<RoleEntity> roles, String operator) {
        userRoleMapper.physicalDeleteByUserId(userId);
        for (RoleEntity role : roles) {
            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            userRole.setPrimaryRole(primaryRoleCode.equals(normalizeRole(role.getRoleCode())));
            userRole.setCreatedBy(operator);
            userRole.setUpdatedBy(operator);
            userRoleMapper.insert(userRole);
        }
    }

    private RoleEntity requireRole(String roleCode) {
        String normalizedRole = normalizeRole(roleCode);
        if (normalizedRole.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "请选择角色");
        }
        RoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getRoleCode, normalizedRole)
                .eq(RoleEntity::getEnabled, true)
                .last("LIMIT 1"));
        if (role == null || role.getId() == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "角色不存在或已停用");
        }
        return role;
    }

    private List<RoleEntity> requireRoles(List<String> roleCodes) {
        List<RoleEntity> roles = roleCodes.stream().map(this::requireRole).toList();
        Set<Long> roleIds = new LinkedHashSet<>();
        for (RoleEntity role : roles) {
            if (role.getId() == null || !roleIds.add(role.getId())) {
                throw new BusinessException(CODE_BAD_REQUEST, "角色配置重复或无效");
            }
        }
        return roles;
    }

    private List<String> normalizeAssignedRoleCodes(String primaryRoleCode, List<String> requestedRoleCodes) {
        String normalizedPrimary = normalizeRole(primaryRoleCode);
        if (normalizedPrimary.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "请选择主角色");
        }
        if (requestedRoleCodes != null) {
            List<String> extraRoleCodes = requestedRoleCodes.stream()
                    .map(this::normalizeRole)
                    .filter(code -> !code.isEmpty())
                    .filter(code -> !normalizedPrimary.equals(code))
                    .toList();
            if (!extraRoleCodes.isEmpty()) {
                throw new BusinessException(CODE_BAD_REQUEST, "当前版本一个用户只能分配一个角色");
            }
        }
        return List.of(normalizedPrimary);
    }

    private void assertSelfGuard(CurrentUserPrincipal principal, UserEntity target, String nextRoleCode, Boolean nextEnabled) {
        if (!principal.id().equals(target.getId())) {
            return;
        }
        if (!ROLE_ADMIN.equals(nextRoleCode)) {
            throw new BusinessException(CODE_FORBIDDEN, "不能修改自己的管理员角色");
        }
        if (!Boolean.TRUE.equals(nextEnabled)) {
            throw new BusinessException(CODE_FORBIDDEN, "不能停用当前登录账号");
        }
    }

    private void recordLog(CurrentUserPrincipal principal, String actionCode, Long bizId, String content) {
        OperationLogEntity log = new OperationLogEntity();
        log.setOperator(principal.account());
        log.setModuleCode("USER");
        log.setActionCode(actionCode);
        log.setBizId(String.valueOf(bizId));
        log.setContent(content);
        log.setCreatedBy(principal.account());
        log.setUpdatedBy(principal.account());
        operationLogMapper.insert(log);
    }

    private UserVO toVO(UserEntity user) {
        DepartmentEntity department = primaryDepartment(user.getId());
        return new UserVO(
                user.getId(),
                user.getAccount(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRoleCode(),
                roleCodes(user.getId(), user.getRoleCode()),
                department == null ? null : department.getId(),
                department == null ? null : department.getDeptName(),
                department == null ? null : department.getDeptPath(),
                user.getEnabled(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private List<String> roleCodes(Long userId, String fallbackRoleCode) {
        List<UserRoleEntity> rows = userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getUserId, userId)
                .orderByDesc(UserRoleEntity::getPrimaryRole)
                .orderByAsc(UserRoleEntity::getId));
        Long roleId = rows.stream().map(UserRoleEntity::getRoleId).filter(id -> id != null).findFirst().orElse(null);
        if (roleId == null) {
            String normalizedFallback = normalizeRole(fallbackRoleCode);
            return normalizedFallback.isEmpty() ? List.of() : List.of(normalizedFallback);
        }
        Map<Long, String> codeById = roleMapper.selectBatchIds(List.of(roleId)).stream()
                .collect(Collectors.toMap(RoleEntity::getId, role -> normalizeRole(role.getRoleCode()), (left, right) -> left, LinkedHashMap::new));
        String roleCode = codeById.get(roleId);
        return roleCode == null || roleCode.isEmpty() ? List.of() : List.of(roleCode);
    }

    private void syncUserDepartment(Long userId, Long departmentId, String operator) {
        userDepartmentMapper.physicalDeleteByUserId(userId);
        UserDepartmentEntity userDepartment = new UserDepartmentEntity();
        userDepartment.setUserId(userId);
        userDepartment.setDepartmentId(departmentId);
        userDepartment.setPrimaryDepartment(true);
        userDepartment.setCreatedBy(operator);
        userDepartment.setUpdatedBy(operator);
        userDepartmentMapper.insert(userDepartment);
    }

    private DepartmentEntity requireTargetDepartment(Long departmentId) {
        DepartmentEntity department = departmentId == null ? defaultDepartment() : departmentMapper.selectById(departmentId);
        if (department == null || !Boolean.TRUE.equals(department.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, "部门不存在或已停用");
        }
        return department;
    }

    private DepartmentEntity defaultDepartment() {
        return departmentMapper.selectOne(new LambdaQueryWrapper<DepartmentEntity>()
                .eq(DepartmentEntity::getDeptCode, DEFAULT_DEPT_CODE)
                .eq(DepartmentEntity::getEnabled, true)
                .last("LIMIT 1"));
    }

    private DepartmentEntity primaryDepartment(Long userId) {
        UserDepartmentEntity relation = userDepartmentMapper.selectList(new LambdaQueryWrapper<UserDepartmentEntity>()
                        .eq(UserDepartmentEntity::getUserId, userId)
                        .orderByDesc(UserDepartmentEntity::getPrimaryDepartment)
                        .orderByAsc(UserDepartmentEntity::getId))
                .stream()
                .findFirst()
                .orElse(null);
        if (relation == null || relation.getDepartmentId() == null) {
            return null;
        }
        return departmentMapper.selectById(relation.getDepartmentId());
    }

    private long normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private String normalizeRole(String value) {
        return normalize(value).toUpperCase();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
