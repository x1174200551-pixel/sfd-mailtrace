package com.ntn.fziot.mailtrace.application.bizservice.mailfetch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.log.MailFetchLogPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.log.MailFetchLogStatsVO;
import com.ntn.fziot.mailtrace.interfaces.vo.log.MailFetchLogVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailFetchLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailFetchLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailFetchLogBizService {

    private final MailFetchLogMapper mailFetchLogMapper;
    private final MailboxMapper mailboxMapper;
    private final PermissionService permissionService;
    private final EnterpriseMailboxAccessService enterpriseMailboxAccessService;

    /**
     * 拉取日志统计概览（总量，不受筛选条件影响）。
     */
    public MailFetchLogStatsVO stats(CurrentUserPrincipal principal) {
        permissionService.assertPermission(principal, "mail_fetch_log:read", "无权访问拉取日志");
        Set<Long> readableMailboxIds = enterpriseMailboxAccessService.resolveReadableMailboxIds(principal);
        if (readableMailboxIds.isEmpty()) {
            return new MailFetchLogStatsVO(0, 0, 0, 0);
        }
        List<MailFetchLogEntity> logs = mailFetchLogMapper.selectList(
                new LambdaQueryWrapper<MailFetchLogEntity>()
                        .in(MailFetchLogEntity::getMailboxId, readableMailboxIds));
        long totalCount = logs.size();
        long successCount = logs.stream().filter(log -> Boolean.TRUE.equals(log.getSuccess())).count();
        long failCount = logs.stream().filter(log -> Boolean.FALSE.equals(log.getSuccess())).count();
        long totalCreatedTickets = logs.stream()
                .mapToLong(log -> log.getCreatedTicketCount() == null ? 0 : log.getCreatedTicketCount())
                .sum();
        return new MailFetchLogStatsVO(totalCount, successCount, failCount, totalCreatedTickets);
    }

    /**
     * 分页查询拉取日志。
     */
    public MailFetchLogPageResponse pageFetchLogs(CurrentUserPrincipal principal,
                                                  Long enterpriseId, Long mailboxId, Boolean success,
                                                  LocalDateTime startFrom, LocalDateTime startTo,
                                                  String keyword, Integer page, Integer size) {
        permissionService.assertPermission(principal, "mail_fetch_log:read", "无权访问拉取日志");

        long currentPage = normalizePage(page);
        long pageSize = normalizeSize(size);
        Set<Long> readableMailboxIds = enterpriseMailboxAccessService.resolveReadableMailboxIds(principal);

        LambdaQueryWrapper<MailFetchLogEntity> wrapper = buildQuery(enterpriseId, mailboxId, success, startFrom, startTo, keyword)
                .orderByDesc(MailFetchLogEntity::getStartedAt)
                .orderByDesc(MailFetchLogEntity::getId);
        applyMailboxScope(wrapper, readableMailboxIds);

        Page<MailFetchLogEntity> result = mailFetchLogMapper.selectPage(Page.of(currentPage, pageSize), wrapper);

        // 批量查询邮箱名称和地址（空列表跳过，避免 IN () 语法错误）
        List<Long> mailboxIds = result.getRecords().stream()
                .map(MailFetchLogEntity::getMailboxId)
                .distinct()
                .toList();
        Map<Long, MailboxEntity> mailboxMap = mailboxIds.isEmpty()
                ? new java.util.HashMap<>()
                : mailboxMapper.selectBatchIds(mailboxIds).stream()
                        .collect(Collectors.toMap(MailboxEntity::getId, java.util.function.Function.identity()));

        List<MailFetchLogVO> records = result.getRecords().stream()
                .map(log -> {
                    MailboxEntity mb = mailboxMap.get(log.getMailboxId());
                    return toVO(log, mb != null ? mb.getMailboxName() : null, mb != null ? mb.getEmailAddress() : null);
                })
                .toList();

        return new MailFetchLogPageResponse(
                records, result.getTotal(), result.getCurrent(),
                result.getSize(), result.getPages()
        );
    }

    private void applyMailboxScope(LambdaQueryWrapper<MailFetchLogEntity> wrapper, Set<Long> mailboxIds) {
        if (mailboxIds.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.in(MailFetchLogEntity::getMailboxId, mailboxIds);
    }

    private LambdaQueryWrapper<MailFetchLogEntity> buildQuery(Long enterpriseId, Long mailboxId, Boolean success,
                                                              LocalDateTime startFrom, LocalDateTime startTo,
                                                              String keyword) {
        LambdaQueryWrapper<MailFetchLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (enterpriseId != null) {
            wrapper.eq(MailFetchLogEntity::getEnterpriseId, enterpriseId);
        }
        if (mailboxId != null) {
            wrapper.eq(MailFetchLogEntity::getMailboxId, mailboxId);
        }
        if (success != null) {
            wrapper.eq(MailFetchLogEntity::getSuccess, success);
        }
        if (startFrom != null) {
            wrapper.ge(MailFetchLogEntity::getStartedAt, startFrom);
        }
        if (startTo != null) {
            wrapper.le(MailFetchLogEntity::getStartedAt, startTo);
        }
        String normalizedKeyword = normalize(keyword);
        if (!normalizedKeyword.isEmpty()) {
            wrapper.like(MailFetchLogEntity::getErrorMessage, normalizedKeyword);
        }
        return wrapper;
    }

    private MailFetchLogVO toVO(MailFetchLogEntity log, String mailboxName, String emailAddress) {
        return new MailFetchLogVO(
                log.getId(), log.getEnterpriseId(), log.getMailboxId(), mailboxName, emailAddress,
                log.getTriggerType(),
                log.getStartedAt(), log.getFinishedAt(),
                log.getSuccess(), log.getFetchedCount(),
                log.getCreatedTicketCount(), log.getLinkedCount(),
                log.getErrorMessage(), log.getCreatedAt()
        );
    }

    private long normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long normalizeSize(Integer size) {
        if (size == null || size < 1) return 20;
        return Math.min(size, 100);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
