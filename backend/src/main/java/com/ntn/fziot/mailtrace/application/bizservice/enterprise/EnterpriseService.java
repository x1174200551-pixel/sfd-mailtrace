package com.ntn.fziot.mailtrace.application.bizservice.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.notification.FeishuNotificationService;
import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseOptionVO;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseVO;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.FeishuGroupTestResponse;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnterpriseService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_NOT_FOUND = 40401;
    private static final int CODE_CONFLICT = 40901;
    private static final String MODULE_ENTERPRISE = "ENTERPRISE";

    private final EnterpriseMapper enterpriseMapper;
    private final OperationLogMapper operationLogMapper;
    private final PermissionService permissionService;
    private final EnterpriseMailboxAccessService enterpriseMailboxAccessService;
    private final MailboxMapper mailboxMapper;
    private final TicketMapper ticketMapper;
    private final FeishuNotificationService feishuNotificationService;

    public EnterpriseListResponse listEnterprises(CurrentUserPrincipal principal, String keyword, Boolean enabled,
                                                   Integer page, Integer size) {
        permissionService.assertPermission(principal, "enterprise:read", "无权查看企业配置");
        LambdaQueryWrapper<EnterpriseEntity> wrapper = buildQuery(keyword, enabled)
                .orderByDesc(EnterpriseEntity::getEnabled)
                .orderByAsc(EnterpriseEntity::getEnterpriseName)
                .orderByAsc(EnterpriseEntity::getId);
        Page<EnterpriseEntity> result = enterpriseMapper.selectPage(
                Page.of(normalizePage(page), normalizeSize(size)), wrapper);
        List<EnterpriseVO> records = result.getRecords().stream().map(this::toVO).toList();
        long total = enterpriseMapper.selectCount(new LambdaQueryWrapper<>());
        long enabledCount = enterpriseMapper.selectCount(
                new LambdaQueryWrapper<EnterpriseEntity>().eq(EnterpriseEntity::getEnabled, true));
        return new EnterpriseListResponse(records, result.getTotal(), result.getCurrent(), result.getSize(),
                result.getPages(), total, enabledCount, total - enabledCount);
    }

    public EnterpriseVO getEnterprise(CurrentUserPrincipal principal, Long id) {
        permissionService.assertPermission(principal, "enterprise:read", "无权查看企业配置");
        return toVO(requireEnterprise(id));
    }

    /**
     * 业务页面使用的企业选项不要求配置权限，但严格限制为当前用户可见企业。
     */
    public List<EnterpriseOptionVO> listVisibleOptions(CurrentUserPrincipal principal, Boolean enabled) {
        if (principal == null || principal.id() == null) {
            throw new BusinessException(40302, "未登录");
        }
        Set<Long> visibleEnterpriseIds = enterpriseMailboxAccessService.resolveVisibleEnterpriseIds(principal);
        if (visibleEnterpriseIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<EnterpriseEntity> wrapper = new LambdaQueryWrapper<EnterpriseEntity>()
                .in(EnterpriseEntity::getId, visibleEnterpriseIds)
                .orderByAsc(EnterpriseEntity::getEnterpriseName)
                .orderByAsc(EnterpriseEntity::getId);
        if (enabled != null) {
            wrapper.eq(EnterpriseEntity::getEnabled, enabled);
        }
        return enterpriseMapper.selectList(wrapper).stream()
                .map(entity -> new EnterpriseOptionVO(entity.getId(), entity.getEnterpriseName(), entity.getEnabled()))
                .toList();
    }

    @Transactional
    public EnterpriseVO createEnterprise(CurrentUserPrincipal principal, EnterpriseSaveRequest request) {
        permissionService.assertPermission(principal, "enterprise:create", "无权新建企业配置");
        String enterpriseName = normalize(request.getEnterpriseName());
        ensureNameUnique(enterpriseName, null);

        EnterpriseEntity entity = new EnterpriseEntity();
        fillEnterprise(entity, request, principal.account());
        entity.setEnterpriseName(enterpriseName);
        entity.setCreatedBy(principal.account());
        applyFeishuConfig(entity, null, request);
        enterpriseMapper.insert(entity);
        recordLog(principal, "CREATE", entity.getId(), "新建企业：" + enterpriseName);
        return toVO(enterpriseMapper.selectById(entity.getId()));
    }

    @Transactional
    public EnterpriseVO updateEnterprise(CurrentUserPrincipal principal, Long id, EnterpriseSaveRequest request) {
        permissionService.assertPermission(principal, "enterprise:update", "无权编辑企业配置");
        EnterpriseEntity existing = requireEnterprise(id);
        String enterpriseName = normalize(request.getEnterpriseName());
        ensureNameUnique(enterpriseName, id);

        EnterpriseEntity update = new EnterpriseEntity();
        update.setId(id);
        fillEnterprise(update, request, principal.account());
        update.setEnterpriseName(enterpriseName);
        applyFeishuConfig(update, existing, request);
        enterpriseMapper.updateById(update);
        if (Boolean.TRUE.equals(request.getClearFeishuConfig())) {
            enterpriseMapper.update(null, new LambdaUpdateWrapper<EnterpriseEntity>()
                    .eq(EnterpriseEntity::getId, id)
                    .set(EnterpriseEntity::getFeishuNotifyEnabled, false)
                    .set(EnterpriseEntity::getFeishuGroupName, null)
                    .set(EnterpriseEntity::getFeishuWebhookUrl, null)
                    .set(EnterpriseEntity::getFeishuSigningSecret, null)
                    .set(EnterpriseEntity::getFeishuConfigVersion, update.getFeishuConfigVersion())
                    .set(EnterpriseEntity::getFeishuConnectionStatus, "UNCONFIGURED")
                    .set(EnterpriseEntity::getFeishuLastTestAt, null)
                    .set(EnterpriseEntity::getFeishuLastError, null)
                    .set(EnterpriseEntity::getUpdatedBy, principal.account()));
        }
        recordLog(principal, "UPDATE", id, "编辑企业：" + existing.getEnterpriseName());
        return toVO(enterpriseMapper.selectById(id));
    }

    @Transactional
    public EnterpriseVO updateEnabled(CurrentUserPrincipal principal, Long id, EnterpriseEnabledRequest request) {
        permissionService.assertPermission(principal, "enterprise:enable", "无权启停企业配置");
        EnterpriseEntity existing = requireEnterprise(id);
        enterpriseMapper.update(null, new LambdaUpdateWrapper<EnterpriseEntity>()
                .eq(EnterpriseEntity::getId, id)
                .set(EnterpriseEntity::getEnabled, request.getEnabled())
                .set(EnterpriseEntity::getUpdatedBy, principal.account()));
        recordLog(principal, Boolean.TRUE.equals(request.getEnabled()) ? "ENABLE" : "DISABLE", id,
                (Boolean.TRUE.equals(request.getEnabled()) ? "启用企业：" : "停用企业：")
                        + existing.getEnterpriseName());
        return toVO(enterpriseMapper.selectById(id));
    }

    public FeishuGroupTestResponse testFeishuGroup(CurrentUserPrincipal principal, Long enterpriseId) {
        permissionService.assertPermission(principal, "enterprise:update", "无权测试企业飞书通知群");
        requireEnterprise(enterpriseId);
        FeishuGroupTestResponse response = feishuNotificationService.sendTest(enterpriseId);
        try {
            recordLog(principal, "TEST_FEISHU", enterpriseId,
                    response.accepted() ? "企业飞书通知群测试成功" : "企业飞书通知群测试失败");
        } catch (RuntimeException exception) {
            // 飞书已经产生外部副作用，审计日志失败不能把已发送结果伪装成接口失败。
            log.warn("记录企业飞书通知群测试审计日志失败 enterpriseId={} operator={} errorType={}",
                    enterpriseId, principal.account(), exception.getClass().getSimpleName());
        }
        return response;
    }

    private LambdaQueryWrapper<EnterpriseEntity> buildQuery(String keyword, Boolean enabled) {
        LambdaQueryWrapper<EnterpriseEntity> wrapper = new LambdaQueryWrapper<>();
        String normalizedKeyword = normalize(keyword);
        if (!normalizedKeyword.isEmpty()) {
            wrapper.and(query -> query
                    .like(EnterpriseEntity::getEnterpriseName, normalizedKeyword)
                    .or()
                    .like(EnterpriseEntity::getContactName, normalizedKeyword)
                    .or()
                    .like(EnterpriseEntity::getContactEmail, normalizedKeyword)
                    .or()
                    .like(EnterpriseEntity::getContactPhone, normalizedKeyword));
        }
        if (enabled != null) {
            wrapper.eq(EnterpriseEntity::getEnabled, enabled);
        }
        return wrapper;
    }

    private void ensureNameUnique(String enterpriseName, Long excludeId) {
        if (enterpriseName.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "企业名称不能为空");
        }
        LambdaQueryWrapper<EnterpriseEntity> wrapper = new LambdaQueryWrapper<EnterpriseEntity>()
                .eq(EnterpriseEntity::getEnterpriseName, enterpriseName);
        if (excludeId != null) {
            wrapper.ne(EnterpriseEntity::getId, excludeId);
        }
        Long count = enterpriseMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(CODE_CONFLICT, "企业名称已存在");
        }
    }

    private EnterpriseEntity requireEnterprise(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(CODE_BAD_REQUEST, "企业ID不能为空");
        }
        EnterpriseEntity entity = enterpriseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(CODE_NOT_FOUND, "企业不存在");
        }
        return entity;
    }

    private void fillEnterprise(EnterpriseEntity entity, EnterpriseSaveRequest request, String operator) {
        entity.setContactName(normalizeNullable(request.getContactName()));
        entity.setContactEmail(normalizeLowerNullable(request.getContactEmail()));
        entity.setContactPhone(normalizeNullable(request.getContactPhone()));
        entity.setEnabled(request.getEnabled() == null || request.getEnabled());
        entity.setRemark(normalizeNullable(request.getRemark()));
        entity.setUpdatedBy(operator);
    }

    private void applyFeishuConfig(EnterpriseEntity target, EnterpriseEntity existing,
                                   EnterpriseSaveRequest request) {
        if (Boolean.TRUE.equals(request.getClearFeishuConfig())) {
            target.setFeishuNotifyEnabled(false);
            target.setFeishuGroupName(null);
            target.setFeishuWebhookUrl(null);
            target.setFeishuSigningSecret(null);
            target.setFeishuConfigVersion(nextVersion(existing));
            target.setFeishuConnectionStatus("UNCONFIGURED");
            target.setFeishuLastTestAt(null);
            target.setFeishuLastError(null);
            return;
        }

        String groupName = firstConfigured(request.getFeishuGroupName(),
                existing == null ? null : existing.getFeishuGroupName());
        String webhook = firstConfigured(request.getFeishuWebhookUrl(),
                existing == null ? null : existing.getFeishuWebhookUrl());
        String secret = firstConfigured(request.getFeishuSigningSecret(),
                existing == null ? null : existing.getFeishuSigningSecret());
        boolean configChanged = existing == null
                ? hasText(groupName) || hasText(webhook) || hasText(secret)
                : changedWhenProvided(request.getFeishuWebhookUrl(), existing.getFeishuWebhookUrl())
                        || changedWhenProvided(request.getFeishuSigningSecret(), existing.getFeishuSigningSecret());

        target.setFeishuGroupName(groupName);
        target.setFeishuWebhookUrl(webhook);
        target.setFeishuSigningSecret(secret);
        target.setFeishuConfigVersion(configChanged ? nextVersion(existing) : currentVersion(existing));
        target.setFeishuConnectionStatus(configChanged
                ? (isComplete(groupName, webhook, secret) ? "UNTESTED" : "UNCONFIGURED")
                : currentStatus(existing));
        target.setFeishuLastTestAt(configChanged ? null : existing == null ? null : existing.getFeishuLastTestAt());
        target.setFeishuLastError(configChanged ? null : existing == null ? null : existing.getFeishuLastError());

        boolean notifyEnabled = request.getFeishuNotifyEnabled() == null
                ? existing != null && Boolean.TRUE.equals(existing.getFeishuNotifyEnabled())
                : Boolean.TRUE.equals(request.getFeishuNotifyEnabled());
        if (notifyEnabled) {
            validateCompleteConfig(groupName, webhook, secret);
        }
        target.setFeishuNotifyEnabled(notifyEnabled);
    }

    private void validateCompleteConfig(String groupName, String webhook, String secret) {
        if (!isComplete(groupName, webhook, secret)) {
            throw new BusinessException(CODE_BAD_REQUEST, "启用飞书通知前请完整配置群名称、Webhook 和签名密钥");
        }
        try {
            feishuNotificationService.validateWebhook(webhook);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(CODE_BAD_REQUEST, exception.getMessage());
        }
    }

    private boolean isComplete(String groupName, String webhook, String secret) {
        return hasText(groupName) && hasText(webhook) && hasText(secret);
    }

    private boolean changedWhenProvided(String requestValue, String existingValue) {
        return hasText(requestValue) && !normalize(existingValue).equals(normalize(requestValue));
    }

    private String firstConfigured(String requestValue, String existingValue) {
        String normalized = normalizeNullable(requestValue);
        return normalized == null ? normalizeNullable(existingValue) : normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int nextVersion(EnterpriseEntity existing) {
        return currentVersion(existing) + 1;
    }

    private int currentVersion(EnterpriseEntity existing) {
        return existing == null || existing.getFeishuConfigVersion() == null ? 0 : existing.getFeishuConfigVersion();
    }

    private String currentStatus(EnterpriseEntity existing) {
        return existing == null || existing.getFeishuConnectionStatus() == null
                ? "UNCONFIGURED" : existing.getFeishuConnectionStatus();
    }

    private void recordLog(CurrentUserPrincipal principal, String actionCode, Long bizId, String content) {
        OperationLogEntity log = new OperationLogEntity();
        log.setOperator(principal.account());
        log.setModuleCode(MODULE_ENTERPRISE);
        log.setActionCode(actionCode);
        log.setBizId(bizId == null ? null : String.valueOf(bizId));
        log.setContent(content);
        log.setCreatedBy(principal.account());
        log.setUpdatedBy(principal.account());
        operationLogMapper.insert(log);
    }

    private EnterpriseVO toVO(EnterpriseEntity entity) {
        Long mailboxCount = mailboxMapper.selectCount(new LambdaQueryWrapper<MailboxEntity>()
                .eq(MailboxEntity::getEnterpriseId, entity.getId()));
        Long ticketCount = ticketMapper.selectCount(new LambdaQueryWrapper<TicketEntity>()
                .eq(TicketEntity::getEnterpriseId, entity.getId()));
        return new EnterpriseVO(
                entity.getId(), entity.getEnterpriseName(), entity.getContactName(), entity.getContactEmail(),
                entity.getContactPhone(), mailboxCount == null ? 0L : mailboxCount,
                ticketCount == null ? 0L : ticketCount, entity.getEnabled(), entity.getFeishuNotifyEnabled(),
                entity.getFeishuGroupName(), isComplete(entity.getFeishuGroupName(),
                        entity.getFeishuWebhookUrl(), entity.getFeishuSigningSecret()),
                entity.getFeishuConnectionStatus(), entity.getFeishuLastTestAt(), entity.getFeishuLastError(), entity.getRemark(),
                entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeLowerNullable(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase();
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
}
