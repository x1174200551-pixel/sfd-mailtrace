package com.ntn.fziot.mailtrace.application.bizservice.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.application.bizservice.security.OperationLogService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserCreateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserDataGrantDetailVO;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserDataGrantSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserDataGrantVO;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserResetPasswordRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserSummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserUpdateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.DepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.AssignmentRuleGroupEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.RoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserDataGrantEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserDepartmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserRoleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.DepartmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.AssignmentRuleGroupMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.RoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserDepartmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserRoleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserDataGrantMapper;
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
    private static final String GRANT_ENTERPRISE = "ENTERPRISE";
    private static final String GRANT_MAILBOX = "MAILBOX";

    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final DepartmentMapper departmentMapper;
    private final UserDepartmentMapper userDepartmentMapper;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;
    private final EnterpriseMailboxAccessService enterpriseMailboxAccessService;
    private final UserDataGrantMapper userDataGrantMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final MailboxMapper mailboxMapper;
    private final AssignmentRuleGroupMapper assignmentRuleGroupMapper;

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
    public List<UserVO> listAssignableUsers(CurrentUserPrincipal principal, Long enterpriseId,
                                            Long mailboxId, Long assignmentRuleGroupId) {
        assertAuthenticated(principal);
        AssignableTarget target = resolveAssignableTarget(principal, enterpriseId, mailboxId, assignmentRuleGroupId);
        return userMapper.selectList(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getEnabled, true)
                        .orderByAsc(UserEntity::getDisplayName)
        ).stream()
                .filter(user -> isAssignableToTarget(user.getId(), target))
                .map(this::toVO)
                .toList();
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
        assertRoleAssignmentAllowed(principal, primaryRoleCode);
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
        replaceDataGrants(principal, user, request.getEnterpriseIds(), request.getMailboxIds());

        // 4、写入用户创建操作日志
        operationLogService.record(principal, "USER", "CREATE", user.getId(), "新建用户：" + user.getAccount());
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
        assertTargetManageable(principal, existing);
        assertRoleAssignmentAllowed(principal, primaryRoleCode);
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
        UserEntity grantTarget = new UserEntity();
        grantTarget.setId(id);
        grantTarget.setAccount(existing.getAccount());
        grantTarget.setRoleCode(primaryRoleCode);
        replaceDataGrants(principal, grantTarget, request.getEnterpriseIds(), request.getMailboxIds());

        // 5、写入用户编辑操作日志并返回最新用户详情
        operationLogService.record(principal, "USER", "UPDATE", id, "编辑用户：" + existing.getAccount());
        return toVO(userMapper.selectById(id));
    }

    public UserDataGrantDetailVO getDataGrants(CurrentUserPrincipal principal, Long userId) {
        permissionService.assertPermission(principal, "user:read", "无权查看用户数据授权");
        UserEntity user = requireUser(userId);
        if (ROLE_ADMIN.equals(normalizeRole(user.getRoleCode()))) {
            return new UserDataGrantDetailVO(userId, true, List.of());
        }
        return new UserDataGrantDetailVO(userId, false, loadDataGrants(userId));
    }

    @Transactional
    public UserDataGrantDetailVO saveDataGrants(CurrentUserPrincipal principal, Long userId,
                                                UserDataGrantSaveRequest request) {
        permissionService.assertPermission(principal, "user:update", "无权编辑用户数据授权");
        UserEntity user = requireUser(userId);
        return replaceDataGrants(principal, user,
                request == null ? null : request.getEnterpriseIds(),
                request == null ? null : request.getMailboxIds());
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
        assertTargetManageable(principal, existing);
        Boolean enabled = request.getEnabled();
        assertSelfGuard(principal, existing, existing.getRoleCode(), enabled);

        // 3、更新启用状态和更新人
        userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, id)
                .set(UserEntity::getEnabled, enabled)
                .set(UserEntity::getUpdatedBy, principal.account()));

        // 4、写入启用或停用操作日志
        operationLogService.record(principal, "USER", enabled ? "ENABLE" : "DISABLE", id,
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
        assertTargetManageable(principal, existing);
        // 3、使用 BCrypt 加密新密码并更新密码哈希
        userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, id)
                .set(UserEntity::getPasswordHash, passwordEncoder.encode(request.getPassword().trim()))
                .set(UserEntity::getUpdatedBy, principal.account()));

        // 4、写入重置密码操作日志
        operationLogService.record(principal, "USER", "RESET_PASSWORD", id, "重置密码：" + existing.getAccount());
    }

    private UserDataGrantDetailVO replaceDataGrants(CurrentUserPrincipal principal, UserEntity user,
                                                     Set<Long> requestedEnterpriseIds,
                                                     Set<Long> requestedMailboxIds) {
        assertTargetManageable(principal, user);
        Set<Long> enterpriseIds = normalizeIds(requestedEnterpriseIds);
        Set<Long> mailboxIds = normalizeIds(requestedMailboxIds);
        assertGrantOperatorScope(principal, user.getId(), enterpriseIds, mailboxIds);

        if (ROLE_ADMIN.equals(normalizeRole(user.getRoleCode()))) {
            userDataGrantMapper.physicalDeleteByUserId(user.getId());
            operationLogService.record(principal, "USER", "UPDATE_DATA_GRANT", user.getId(),
                    "管理员用户保持全部数据可见：" + user.getAccount());
            return new UserDataGrantDetailVO(user.getId(), true, List.of());
        }
        if (enterpriseIds.isEmpty() && mailboxIds.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "普通用户至少需要一条企业或邮箱数据授权");
        }

        Map<Long, EnterpriseEntity> enterpriseById = validateEnterprises(enterpriseIds);
        Map<Long, MailboxEntity> mailboxById = validateMailboxes(mailboxIds);
        mailboxIds.removeIf(mailboxId -> {
            MailboxEntity mailbox = mailboxById.get(mailboxId);
            return mailbox != null && enterpriseIds.contains(mailbox.getEnterpriseId());
        });

        userDataGrantMapper.physicalDeleteByUserId(user.getId());
        enterpriseIds.forEach(enterpriseId -> insertGrant(
                user.getId(), GRANT_ENTERPRISE, enterpriseId, null, principal.account()));
        mailboxIds.forEach(mailboxId -> insertGrant(
                user.getId(), GRANT_MAILBOX, null, mailboxId, principal.account()));
        operationLogService.record(principal, "USER", "UPDATE_DATA_GRANT", user.getId(),
                "更新用户数据授权：" + user.getAccount()
                        + "，企业" + enterpriseById.size() + "个，单邮箱" + mailboxIds.size() + "个");
        return new UserDataGrantDetailVO(user.getId(), false, loadDataGrants(user.getId()));
    }

    private void assertGrantOperatorScope(CurrentUserPrincipal principal, Long targetUserId,
                                          Set<Long> enterpriseIds, Set<Long> mailboxIds) {
        if (enterpriseMailboxAccessService.isAdmin(principal)) {
            return;
        }
        if (principal.id().equals(targetUserId)) {
            throw new BusinessException(CODE_FORBIDDEN, "非管理员不能修改自己的数据授权");
        }
        if (!enterpriseMailboxAccessService.resolveVisibleEnterpriseIds(principal).containsAll(enterpriseIds)
                || !enterpriseMailboxAccessService.resolveReadableMailboxIds(principal).containsAll(mailboxIds)) {
            throw new BusinessException(CODE_FORBIDDEN, "不能授予超出当前账号可见范围的企业或邮箱");
        }
    }

    private void assertRoleAssignmentAllowed(CurrentUserPrincipal principal, String roleCode) {
        if (ROLE_ADMIN.equals(normalizeRole(roleCode)) && !enterpriseMailboxAccessService.isAdmin(principal)) {
            throw new BusinessException(CODE_FORBIDDEN, "只有管理员可以分配管理员角色");
        }
    }

    private void assertTargetManageable(CurrentUserPrincipal principal, UserEntity target) {
        if (target != null && ROLE_ADMIN.equals(normalizeRole(target.getRoleCode()))
                && !enterpriseMailboxAccessService.isAdmin(principal)) {
            throw new BusinessException(CODE_FORBIDDEN, "非管理员不能修改管理员账号");
        }
    }

    private AssignableTarget resolveAssignableTarget(CurrentUserPrincipal principal, Long enterpriseId,
                                                      Long mailboxId, Long assignmentRuleGroupId) {
        if (mailboxId != null) {
            MailboxEntity mailbox = mailboxMapper.selectById(mailboxId);
            if (mailbox == null) {
                throw new BusinessException(CODE_NOT_FOUND, "目标邮箱不存在");
            }
            if (enterpriseId != null && !enterpriseId.equals(mailbox.getEnterpriseId())) {
                throw new BusinessException(CODE_BAD_REQUEST, "企业与目标邮箱不匹配");
            }
            enterpriseMailboxAccessService.assertMailboxReadable(principal, mailboxId);
            return new AssignableTarget(mailbox.getEnterpriseId(), Set.of(mailboxId));
        }
        if (assignmentRuleGroupId != null) {
            AssignmentRuleGroupEntity group = assignmentRuleGroupMapper.selectById(assignmentRuleGroupId);
            if (group == null) {
                throw new BusinessException(CODE_NOT_FOUND, "分配规则组不存在");
            }
            if (enterpriseId != null && !enterpriseId.equals(group.getEnterpriseId())) {
                throw new BusinessException(CODE_BAD_REQUEST, "企业与分配规则组不匹配");
            }
            enterpriseMailboxAccessService.assertEnterpriseVisible(principal, group.getEnterpriseId());
            Set<Long> boundMailboxIds = mailboxMapper.selectList(new LambdaQueryWrapper<MailboxEntity>()
                            .eq(MailboxEntity::getAssignmentRuleGroupId, assignmentRuleGroupId))
                    .stream()
                    .map(MailboxEntity::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return new AssignableTarget(group.getEnterpriseId(), boundMailboxIds);
        }
        if (enterpriseId != null) {
            enterpriseMailboxAccessService.assertEnterpriseVisible(principal, enterpriseId);
            return new AssignableTarget(enterpriseId, Set.of());
        }
        throw new BusinessException(CODE_BAD_REQUEST, "请选择目标企业、邮箱或分配规则组");
    }

    private boolean isAssignableToTarget(Long userId, AssignableTarget target) {
        try {
            if (target.mailboxIds().isEmpty()) {
                enterpriseMailboxAccessService.assertAssigneeCanAccessEnterprise(userId, target.enterpriseId());
            } else {
                target.mailboxIds().forEach(mailboxId ->
                        enterpriseMailboxAccessService.assertAssigneeCanAccessMailbox(userId, mailboxId));
            }
            return true;
        } catch (BusinessException exception) {
            return false;
        }
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

    private void assertAuthenticated(CurrentUserPrincipal principal) {
        if (principal == null || principal.id() == null) {
            throw new BusinessException(CODE_FORBIDDEN, "未登录");
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
        if (!normalizeRole(target.getRoleCode()).equals(normalizeRole(nextRoleCode))) {
            throw new BusinessException(CODE_FORBIDDEN, "不能修改自己的角色");
        }
        if (!Boolean.TRUE.equals(nextEnabled)) {
            throw new BusinessException(CODE_FORBIDDEN, "不能停用当前登录账号");
        }
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

    private List<UserDataGrantVO> loadDataGrants(Long userId) {
        return userDataGrantMapper.selectList(new LambdaQueryWrapper<UserDataGrantEntity>()
                        .eq(UserDataGrantEntity::getUserId, userId)
                        .eq(UserDataGrantEntity::getEnabled, true)
                        .orderByAsc(UserDataGrantEntity::getGrantType)
                        .orderByAsc(UserDataGrantEntity::getId))
                .stream()
                .map(grant -> new UserDataGrantVO(grant.getId(), grant.getUserId(), grant.getGrantType(),
                        grant.getEnterpriseId(), grant.getMailboxId(), grant.getEnabled(), grant.getRemark(),
                        grant.getCreatedAt(), grant.getUpdatedAt()))
                .toList();
    }

    private Set<Long> normalizeIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<Long, EnterpriseEntity> validateEnterprises(Set<Long> enterpriseIds) {
        if (enterpriseIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, EnterpriseEntity> entities = enterpriseMapper.selectBatchIds(enterpriseIds).stream()
                .collect(Collectors.toMap(EnterpriseEntity::getId, entity -> entity));
        if (entities.size() != enterpriseIds.size()) {
            throw new BusinessException(CODE_BAD_REQUEST, "授权企业不存在");
        }
        return entities;
    }

    private Map<Long, MailboxEntity> validateMailboxes(Set<Long> mailboxIds) {
        if (mailboxIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, MailboxEntity> mailboxes = mailboxMapper.selectBatchIds(mailboxIds).stream()
                .collect(Collectors.toMap(MailboxEntity::getId, mailbox -> mailbox));
        if (mailboxes.size() != mailboxIds.size()) {
            throw new BusinessException(CODE_BAD_REQUEST, "授权邮箱不存在");
        }
        return mailboxes;
    }

    private void insertGrant(Long userId, String grantType, Long enterpriseId, Long mailboxId, String operator) {
        UserDataGrantEntity grant = new UserDataGrantEntity();
        grant.setUserId(userId);
        grant.setGrantType(grantType);
        grant.setEnterpriseId(enterpriseId);
        grant.setMailboxId(mailboxId);
        grant.setEnabled(true);
        grant.setCreatedBy(operator);
        grant.setUpdatedBy(operator);
        userDataGrantMapper.insert(grant);
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

    private record AssignableTarget(Long enterpriseId, Set<Long> mailboxIds) {
    }
}
