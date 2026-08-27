package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailboxAccessShadowService {

    private final EnterpriseMailboxAccessService enterpriseMailboxAccessService;
    private final MailboxMapper mailboxMapper;
    private final TicketMapper ticketMapper;
    private final DataScopeService dataScopeService;

    @Value("${mailtrace.permission.shadow-enabled:true}")
    private boolean shadowEnabled = true;

    /**
     * 当前邮箱配置列表在旧实现中通过动作权限后可见全部未删除邮箱。
     * 本方法只比较该实际结果与新邮箱授权范围，不参与查询条件或响应组装。
     */
    public ShadowComparison compareMailboxListScope(CurrentUserPrincipal principal) {
        Set<Long> legacyMailboxIds = mailboxMapper.selectList(new LambdaQueryWrapper<MailboxEntity>()).stream()
                .map(MailboxEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> targetMailboxIds = enterpriseMailboxAccessService.resolveReadableMailboxIds(principal);
        ShadowComparison comparison = compare(legacyMailboxIds, targetMailboxIds);
        log.info("邮箱权限影子对比 userId={} legacyCount={} targetCount={} legacyOnlyCount={} targetOnlyCount={}",
                principal == null ? null : principal.id(),
                comparison.legacyCount(),
                comparison.targetCount(),
                comparison.legacyOnlyCount(),
                comparison.targetOnlyCount());
        return comparison;
    }

    public void compareMailboxListScopeSafely(CurrentUserPrincipal principal) {
        if (!shadowEnabled) {
            return;
        }
        try {
            compareMailboxListScope(principal);
        } catch (RuntimeException ex) {
            log.warn("邮箱权限影子对比失败，不影响原请求 userId={} errorType={}",
                    principal == null ? null : principal.id(), ex.getClass().getSimpleName());
        }
    }

    /**
     * 用旧 DataScopeService 生成真实工单过滤条件，再比较旧结果涉及的邮箱和新可读邮箱。
     * 查询结果只用于影子日志，不参与工单列表的生产过滤。
     */
    public ShadowComparison compareTicketScope(CurrentUserPrincipal principal) {
        LambdaQueryWrapper<TicketEntity> legacyWrapper = new LambdaQueryWrapper<>();
        dataScopeService.applyTicketScope(legacyWrapper, principal);
        legacyWrapper.select(TicketEntity::getMailboxId)
                .isNotNull(TicketEntity::getMailboxId)
                .groupBy(TicketEntity::getMailboxId);
        Set<Long> legacyMailboxIds = ticketMapper.selectObjs(legacyWrapper).stream()
                .filter(Objects::nonNull)
                .map(value -> value instanceof Number number ? number.longValue() : Long.valueOf(value.toString()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> targetMailboxIds = enterpriseMailboxAccessService.resolveReadableMailboxIds(principal);
        ShadowComparison comparison = compare(legacyMailboxIds, targetMailboxIds);
        log.info("工单权限影子对比 userId={} legacyCount={} targetCount={} legacyOnlyCount={} targetOnlyCount={}",
                principal == null ? null : principal.id(),
                comparison.legacyCount(),
                comparison.targetCount(),
                comparison.legacyOnlyCount(),
                comparison.targetOnlyCount());
        return comparison;
    }

    public void compareTicketScopeSafely(CurrentUserPrincipal principal) {
        if (!shadowEnabled) {
            return;
        }
        try {
            compareTicketScope(principal);
        } catch (RuntimeException ex) {
            log.warn("工单权限影子对比失败，不影响原请求 userId={} errorType={}",
                    principal == null ? null : principal.id(), ex.getClass().getSimpleName());
        }
    }

    ShadowComparison compare(Set<Long> legacyMailboxIds, Set<Long> targetMailboxIds) {
        Set<Long> legacy = legacyMailboxIds == null ? Set.of() : legacyMailboxIds;
        Set<Long> target = targetMailboxIds == null ? Set.of() : targetMailboxIds;
        Set<Long> legacyOnly = new LinkedHashSet<>(legacy);
        legacyOnly.removeAll(target);
        Set<Long> targetOnly = new LinkedHashSet<>(target);
        targetOnly.removeAll(legacy);
        return new ShadowComparison(legacy.size(), target.size(), legacyOnly.size(), targetOnly.size());
    }

    public record ShadowComparison(
            int legacyCount,
            int targetCount,
            int legacyOnlyCount,
            int targetOnlyCount
    ) {
        public boolean matches() {
            return legacyOnlyCount == 0 && targetOnlyCount == 0;
        }
    }
}
