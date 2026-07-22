package com.sfonda.mailtrace.application.bizservice.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sfonda.mailtrace.application.bizservice.common.BusinessException;
import com.sfonda.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.sfonda.mailtrace.interfaces.vo.user.UserCreateRequest;
import com.sfonda.mailtrace.interfaces.vo.user.UserEnabledRequest;
import com.sfonda.mailtrace.interfaces.vo.user.UserPageResponse;
import com.sfonda.mailtrace.interfaces.vo.user.UserResetPasswordRequest;
import com.sfonda.mailtrace.interfaces.vo.user.UserSummaryVO;
import com.sfonda.mailtrace.interfaces.vo.user.UserUpdateRequest;
import com.sfonda.mailtrace.interfaces.vo.user.UserVO;
import com.sfonda.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.sfonda.mailtrace.repox.mysql.entity.UserEntity;
import com.sfonda.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.sfonda.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_FORBIDDEN = 40302;
    private static final int CODE_NOT_FOUND = 40401;
    private static final int CODE_CONFLICT = 40901;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_AGENT = "AGENT";

    private final UserMapper userMapper;
    private final OperationLogMapper operationLogMapper;
    private final PasswordEncoder passwordEncoder;

    public UserPageResponse pageUsers(CurrentUserPrincipal principal, String keyword, String roleCode, Boolean enabled,
                                      Integer page, Integer size) {
        assertAdmin(principal);
        long currentPage = normalizePage(page);
        long pageSize = normalizeSize(size);
        LambdaQueryWrapper<UserEntity> wrapper = buildQuery(keyword, roleCode, enabled)
                .orderByDesc(UserEntity::getUpdatedAt)
                .orderByDesc(UserEntity::getId);

        Page<UserEntity> result = userMapper.selectPage(Page.of(currentPage, pageSize), wrapper);
        List<UserVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new UserPageResponse(
                records,
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getPages(),
                buildSummary()
        );
    }

    @Transactional
    public UserVO createUser(CurrentUserPrincipal principal, UserCreateRequest request) {
        assertAdmin(principal);
        String account = normalize(request.getAccount());
        assertRole(request.getRoleCode());
        ensureAccountUnique(account);

        UserEntity user = new UserEntity();
        user.setAccount(account);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        user.setDisplayName(normalize(request.getDisplayName()));
        user.setEmail(normalize(request.getEmail()));
        user.setRoleCode(normalizeRole(request.getRoleCode()));
        user.setEnabled(request.getEnabled() == null || request.getEnabled());
        user.setCreatedBy(principal.account());
        user.setUpdatedBy(principal.account());
        userMapper.insert(user);

        recordLog(principal, "CREATE", user.getId(), "新建用户：" + user.getAccount());
        return toVO(userMapper.selectById(user.getId()));
    }

    @Transactional
    public UserVO updateUser(CurrentUserPrincipal principal, Long id, UserUpdateRequest request) {
        assertAdmin(principal);
        UserEntity existing = requireUser(id);
        String roleCode = normalizeRole(request.getRoleCode());
        assertRole(roleCode);
        Boolean enabled = request.getEnabled() == null || request.getEnabled();
        assertSelfGuard(principal, existing, roleCode, enabled);

        userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, id)
                .set(UserEntity::getDisplayName, normalize(request.getDisplayName()))
                .set(UserEntity::getEmail, normalize(request.getEmail()))
                .set(UserEntity::getRoleCode, roleCode)
                .set(UserEntity::getEnabled, enabled)
                .set(UserEntity::getUpdatedBy, principal.account()));

        recordLog(principal, "UPDATE", id, "编辑用户：" + existing.getAccount());
        return toVO(userMapper.selectById(id));
    }

    @Transactional
    public UserVO updateEnabled(CurrentUserPrincipal principal, Long id, UserEnabledRequest request) {
        assertAdmin(principal);
        UserEntity existing = requireUser(id);
        Boolean enabled = request.getEnabled();
        assertSelfGuard(principal, existing, existing.getRoleCode(), enabled);

        userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, id)
                .set(UserEntity::getEnabled, enabled)
                .set(UserEntity::getUpdatedBy, principal.account()));

        recordLog(principal, enabled ? "ENABLE" : "DISABLE", id,
                (enabled ? "启用用户：" : "停用用户：") + existing.getAccount());
        return toVO(userMapper.selectById(id));
    }

    @Transactional
    public void resetPassword(CurrentUserPrincipal principal, Long id, UserResetPasswordRequest request) {
        assertAdmin(principal);
        UserEntity existing = requireUser(id);
        userMapper.update(null, new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getId, id)
                .set(UserEntity::getPasswordHash, passwordEncoder.encode(request.getPassword().trim()))
                .set(UserEntity::getUpdatedBy, principal.account()));

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
            assertRole(normalizedRole);
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

    private void assertAdmin(CurrentUserPrincipal principal) {
        if (principal == null || !ROLE_ADMIN.equals(principal.roleCode())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅管理员可操作用户管理");
        }
    }

    private void assertRole(String roleCode) {
        String normalizedRole = normalizeRole(roleCode);
        if (!ROLE_ADMIN.equals(normalizedRole) && !ROLE_AGENT.equals(normalizedRole)) {
            throw new BusinessException(CODE_BAD_REQUEST, "角色仅支持 ADMIN 或 AGENT");
        }
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
        return new UserVO(
                user.getId(),
                user.getAccount(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRoleCode(),
                user.getEnabled(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
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
