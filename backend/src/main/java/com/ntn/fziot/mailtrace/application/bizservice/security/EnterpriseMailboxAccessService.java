package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserDataGrantEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserDataGrantMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseMailboxAccessService {

    public static final String GRANT_ENTERPRISE = "ENTERPRISE";
    public static final String GRANT_MAILBOX = "MAILBOX";

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_FORBIDDEN = 40302;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ATTR_ACCESS_SCOPE_CACHE =
            EnterpriseMailboxAccessService.class.getName() + ".ACCESS_SCOPE_CACHE";

    private final PermissionService permissionService;
    private final UserDataGrantMapper userDataGrantMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final MailboxMapper mailboxMapper;
    private final UserMapper userMapper;
    private final AccessCatalogCacheQueryService accessCatalogCacheQueryService;

    public boolean isAdmin(CurrentUserPrincipal principal) {
        return resolveAccessScope(principal).admin();
    }

    public Set<Long> resolveVisibleEnterpriseIds(CurrentUserPrincipal principal) {
        return resolveAccessScope(principal).visibleEnterpriseIds();
    }

    public Set<Long> resolveReadableMailboxIds(CurrentUserPrincipal principal) {
        return resolveAccessScope(principal).readableMailboxIds();
    }

    public Set<Long> resolveOperationalMailboxIds(CurrentUserPrincipal principal) {
        return resolveAccessScope(principal).operationalMailboxIds();
    }

    /**
     * 系统定时任务不绑定登录用户，只允许启用企业下的启用邮箱参与新业务。
     */
    public Set<Long> resolveSystemOperationalMailboxIds() {
        return accessCatalogCacheQueryService.getAccessCatalog().operationalMailboxIds();
    }

    public void assertSystemMailboxOperational(Long mailboxId) {
        if (mailboxId == null || !resolveSystemOperationalMailboxIds().contains(mailboxId)) {
            throw new BusinessException(CODE_FORBIDDEN, "邮箱或所属企业已停用，不能执行新业务");
        }
    }

    public void assertEnterpriseVisible(CurrentUserPrincipal principal, Long enterpriseId) {
        if (enterpriseId == null || !resolveVisibleEnterpriseIds(principal).contains(enterpriseId)) {
            throw new BusinessException(CODE_FORBIDDEN, "无权访问该企业");
        }
    }

    public void assertMailboxReadable(CurrentUserPrincipal principal, Long mailboxId) {
        if (mailboxId == null || !resolveReadableMailboxIds(principal).contains(mailboxId)) {
            throw new BusinessException(CODE_FORBIDDEN, "无权查看该邮箱的历史数据");
        }
    }

    public void assertMailboxOperational(CurrentUserPrincipal principal, Long mailboxId) {
        if (mailboxId == null || !resolveOperationalMailboxIds(principal).contains(mailboxId)) {
            throw new BusinessException(CODE_FORBIDDEN, "无权使用该邮箱执行当前业务操作");
        }
    }

    /**
     * 邮箱配置允许操作已停用邮箱，但所属企业必须启用。
     */
    public void assertMailboxConfigurable(CurrentUserPrincipal principal, Long mailboxId) {
        assertMailboxReadable(principal, mailboxId);
        MailboxEntity mailbox = mailboxMapper.selectById(mailboxId);
        EnterpriseEntity enterprise = mailbox == null || mailbox.getEnterpriseId() == null
                ? null
                : enterpriseMapper.selectById(mailbox.getEnterpriseId());
        if (enterprise == null || !Boolean.TRUE.equals(enterprise.getEnabled())) {
            throw new BusinessException(CODE_FORBIDDEN, "邮箱所属企业已停用，不能修改邮箱配置");
        }
    }

    public void assertAssigneeCanAccessMailbox(Long assigneeId, Long mailboxId) {
        if (assigneeId == null || mailboxId == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "处理人和邮箱不能为空");
        }
        assertTicketProcessor(assigneeId);
        UserEntity assignee = userMapper.selectById(assigneeId);
        AccessScope scope = resolveAccessScope(assignee.getId(), assignee.getRoleCode());
        if (!scope.operationalMailboxIds().contains(mailboxId)) {
            throw new BusinessException(CODE_FORBIDDEN, "处理人无权访问该邮箱");
        }
    }

    /**
     * 新邮箱尚未生成 ID 时，只允许管理员或拥有企业级授权的处理人作为候选。
     */
    public void assertAssigneeCanAccessEnterprise(Long assigneeId, Long enterpriseId) {
        if (assigneeId == null || enterpriseId == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "处理人和企业不能为空");
        }
        assertTicketProcessor(assigneeId);
        UserEntity assignee = userMapper.selectById(assigneeId);
        AccessScope scope = resolveAccessScope(assignee.getId(), assignee.getRoleCode());
        if (scope.admin()) {
            return;
        }
        Long count = userDataGrantMapper.selectCount(new LambdaQueryWrapper<UserDataGrantEntity>()
                .eq(UserDataGrantEntity::getUserId, assigneeId)
                .eq(UserDataGrantEntity::getGrantType, GRANT_ENTERPRISE)
                .eq(UserDataGrantEntity::getEnterpriseId, enterpriseId)
                .eq(UserDataGrantEntity::getEnabled, true));
        if (count == null || count == 0) {
            throw new BusinessException(CODE_FORBIDDEN, "处理人没有目标企业的企业级授权");
        }
    }

    public void assertTicketProcessor(Long assigneeId) {
        if (assigneeId == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "处理人不能为空");
        }
        UserEntity assignee = userMapper.selectById(assigneeId);
        if (assignee == null || !Boolean.TRUE.equals(assignee.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, "处理人不存在或已停用");
        }
        AccessScope scope = resolveAccessScope(assignee.getId(), assignee.getRoleCode());
        if (!scope.ticketProcessor()) {
            throw new BusinessException(CODE_FORBIDDEN, "处理人缺少工单回复权限");
        }
    }

    /**
     * MySQL 5.7 不依赖 CHECK 约束，授权写入前由服务层执行目标字段互斥校验。
     */
    public void validateGrantTarget(String grantType, Long enterpriseId, Long mailboxId) {
        if (!isValidGrantTarget(grantType, enterpriseId, mailboxId)) {
            throw new BusinessException(CODE_BAD_REQUEST,
                    "企业授权必须且只能填写 enterpriseId，邮箱授权必须且只能填写 mailboxId");
        }
    }

    AccessScope resolveAccessScope(CurrentUserPrincipal principal) {
        if (principal == null || principal.id() == null) {
            throw new BusinessException(CODE_FORBIDDEN, "未登录");
        }
        return resolveAccessScope(principal.id(), principal.roleCode());
    }

    private AccessScope resolveAccessScope(Long userId, String fallbackRoleCode) {
        Map<Long, AccessScope> requestCache = requestCache();
        AccessScope cached = requestCache == null ? null : requestCache.get(userId);
        if (cached != null) {
            return cached;
        }
        AccessScope loaded = loadAccessScope(userId, fallbackRoleCode);
        if (requestCache != null) {
            requestCache.put(userId, loaded);
        }
        return loaded;
    }

    private AccessScope loadAccessScope(Long userId, String fallbackRoleCode) {
        PermissionService.PermissionContext permissionContext =
                permissionService.getUserPermissions(userId, fallbackRoleCode);
        boolean admin = permissionContext.hasRole(ROLE_ADMIN);
        boolean ticketProcessor = admin || permissionContext.hasPermission("ticket:reply");
        if (admin) {
            return loadAdminScope(userId, ticketProcessor);
        }
        return loadGrantedScope(userId, ticketProcessor);
    }

    private AccessScope loadAdminScope(Long userId, boolean ticketProcessor) {
        AccessCatalogCacheQueryService.AccessCatalog catalog = accessCatalogCacheQueryService.getAccessCatalog();

        return new AccessScope(
                userId,
                true,
                ticketProcessor,
                catalog.visibleEnterpriseIds(),
                catalog.readableMailboxIds(),
                catalog.operationalMailboxIds()
        );
    }

    private AccessScope loadGrantedScope(Long userId, boolean ticketProcessor) {
        List<UserDataGrantEntity> grants = userDataGrantMapper.selectList(
                new LambdaQueryWrapper<UserDataGrantEntity>()
                        .eq(UserDataGrantEntity::getUserId, userId)
                        .eq(UserDataGrantEntity::getEnabled, true));

        Set<Long> grantedEnterpriseIds = new LinkedHashSet<>();
        Set<Long> grantedMailboxIds = new LinkedHashSet<>();
        for (UserDataGrantEntity grant : grants) {
            if (grant == null || !Boolean.TRUE.equals(grant.getEnabled())) {
                continue;
            }
            if (!isValidGrantTarget(grant.getGrantType(), grant.getEnterpriseId(), grant.getMailboxId())) {
                log.warn("忽略非法用户数据授权 userId={} grantId={} grantType={}",
                        userId, grant.getId(), normalizeGrantType(grant.getGrantType()));
                continue;
            }
            if (GRANT_ENTERPRISE.equals(normalizeGrantType(grant.getGrantType()))) {
                grantedEnterpriseIds.add(grant.getEnterpriseId());
            } else {
                grantedMailboxIds.add(grant.getMailboxId());
            }
        }
        if (grantedEnterpriseIds.isEmpty() && grantedMailboxIds.isEmpty()) {
            return AccessScope.empty(userId);
        }

        List<MailboxEntity> candidateMailboxes = loadGrantedMailboxes(grantedEnterpriseIds, grantedMailboxIds);
        Set<Long> candidateEnterpriseIds = new LinkedHashSet<>(grantedEnterpriseIds);
        candidateMailboxes.stream()
                .map(MailboxEntity::getEnterpriseId)
                .filter(Objects::nonNull)
                .forEach(candidateEnterpriseIds::add);
        Map<Long, EnterpriseEntity> enterpriseById = candidateEnterpriseIds.isEmpty()
                ? Map.of()
                : indexEnterprises(enterpriseMapper.selectBatchIds(candidateEnterpriseIds));

        Set<Long> readableMailboxIds = candidateMailboxes.stream()
                .filter(mailbox -> isGrantedMailbox(mailbox, grantedEnterpriseIds, grantedMailboxIds))
                .filter(mailbox -> enterpriseById.containsKey(mailbox.getEnterpriseId()))
                .map(MailboxEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> operationalMailboxIds = candidateMailboxes.stream()
                .filter(mailbox -> readableMailboxIds.contains(mailbox.getId()))
                .filter(mailbox -> isOperational(mailbox, enterpriseById))
                .map(MailboxEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> visibleEnterpriseIds = candidateEnterpriseIds.stream()
                .filter(enterpriseById::containsKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new AccessScope(
                userId,
                false,
                ticketProcessor,
                visibleEnterpriseIds,
                readableMailboxIds,
                operationalMailboxIds
        );
    }

    private List<MailboxEntity> loadGrantedMailboxes(Set<Long> enterpriseIds, Set<Long> mailboxIds) {
        LambdaQueryWrapper<MailboxEntity> wrapper = new LambdaQueryWrapper<>();
        if (!enterpriseIds.isEmpty() && !mailboxIds.isEmpty()) {
            wrapper.and(scope -> scope
                    .in(MailboxEntity::getEnterpriseId, enterpriseIds)
                    .or()
                    .in(MailboxEntity::getId, mailboxIds));
        } else if (!enterpriseIds.isEmpty()) {
            wrapper.in(MailboxEntity::getEnterpriseId, enterpriseIds);
        } else {
            wrapper.in(MailboxEntity::getId, mailboxIds);
        }
        return mailboxMapper.selectList(wrapper);
    }

    private boolean isGrantedMailbox(MailboxEntity mailbox, Set<Long> enterpriseIds, Set<Long> mailboxIds) {
        return mailbox != null && mailbox.getId() != null
                && (mailboxIds.contains(mailbox.getId()) || enterpriseIds.contains(mailbox.getEnterpriseId()));
    }

    private boolean isOperational(MailboxEntity mailbox, Map<Long, EnterpriseEntity> enterpriseById) {
        if (mailbox == null || mailbox.getId() == null || !Boolean.TRUE.equals(mailbox.getEnabled())) {
            return false;
        }
        EnterpriseEntity enterprise = enterpriseById.get(mailbox.getEnterpriseId());
        return enterprise != null && Boolean.TRUE.equals(enterprise.getEnabled());
    }

    private Map<Long, EnterpriseEntity> indexEnterprises(List<EnterpriseEntity> enterprises) {
        if (enterprises == null || enterprises.isEmpty()) {
            return Map.of();
        }
        return enterprises.stream()
                .filter(Objects::nonNull)
                .filter(enterprise -> enterprise.getId() != null)
                .collect(Collectors.toMap(
                        EnterpriseEntity::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private boolean isValidGrantTarget(String grantType, Long enterpriseId, Long mailboxId) {
        String normalized = normalizeGrantType(grantType);
        return GRANT_ENTERPRISE.equals(normalized) && enterpriseId != null && mailboxId == null
                || GRANT_MAILBOX.equals(normalized) && mailboxId != null && enterpriseId == null;
    }

    private String normalizeGrantType(String grantType) {
        return grantType == null ? "" : grantType.trim().toUpperCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, AccessScope> requestCache() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        Object existing = attributes.getRequest().getAttribute(ATTR_ACCESS_SCOPE_CACHE);
        if (existing instanceof Map<?, ?> map) {
            return (Map<Long, AccessScope>) map;
        }
        Map<Long, AccessScope> cache = new LinkedHashMap<>();
        attributes.getRequest().setAttribute(ATTR_ACCESS_SCOPE_CACHE, cache);
        return cache;
    }

    public record AccessScope(
            Long userId,
            boolean admin,
            boolean ticketProcessor,
            Set<Long> visibleEnterpriseIds,
            Set<Long> readableMailboxIds,
            Set<Long> operationalMailboxIds
    ) {
        public AccessScope {
            visibleEnterpriseIds = immutableSet(visibleEnterpriseIds);
            readableMailboxIds = immutableSet(readableMailboxIds);
            operationalMailboxIds = immutableSet(operationalMailboxIds);
        }

        public static AccessScope empty(Long userId) {
            return new AccessScope(userId, false, false, Set.of(), Set.of(), Set.of());
        }

        private static Set<Long> immutableSet(Set<Long> source) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(source == null ? Set.of() : source));
        }
    }
}
