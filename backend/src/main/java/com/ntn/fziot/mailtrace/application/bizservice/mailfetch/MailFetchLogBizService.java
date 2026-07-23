package com.ntn.fziot.mailtrace.application.bizservice.mailfetch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailFetchLogBizService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_AGENT = "AGENT";

    private final MailFetchLogMapper mailFetchLogMapper;
    private final MailboxMapper mailboxMapper;

    /**
     * 拉取日志统计概览（总量，不受筛选条件影响）。
     */
    public MailFetchLogStatsVO stats() {
        long totalCount = mailFetchLogMapper.countTotal();
        long successCount = mailFetchLogMapper.countSuccess();
        long failCount = mailFetchLogMapper.countFail();
        long totalCreatedTickets = mailFetchLogMapper.sumCreatedTicketCount();
        return new MailFetchLogStatsVO(totalCount, successCount, failCount, totalCreatedTickets);
    }

    /**
     * 分页查询拉取日志。
     */
    public MailFetchLogPageResponse pageFetchLogs(CurrentUserPrincipal principal,
                                                  Long mailboxId, Boolean success,
                                                  LocalDateTime startFrom, LocalDateTime startTo,
                                                  String keyword, Integer page, Integer size) {
        // principal 由 @AuthenticationPrincipal 保证非 null，Security 层已做鉴权
        String role = principal.roleCode();
        if (!ROLE_ADMIN.equals(role) && !ROLE_AGENT.equals(role)) {
            throw new com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException(
                    40302, "无权访问拉取日志");
        }

        long currentPage = normalizePage(page);
        long pageSize = normalizeSize(size);

        LambdaQueryWrapper<MailFetchLogEntity> wrapper = buildQuery(mailboxId, success, startFrom, startTo, keyword)
                .orderByDesc(MailFetchLogEntity::getStartedAt)
                .orderByDesc(MailFetchLogEntity::getId);

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

    private LambdaQueryWrapper<MailFetchLogEntity> buildQuery(Long mailboxId, Boolean success,
                                                              LocalDateTime startFrom, LocalDateTime startTo,
                                                              String keyword) {
        LambdaQueryWrapper<MailFetchLogEntity> wrapper = new LambdaQueryWrapper<>();
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
                log.getId(), log.getMailboxId(), mailboxName, emailAddress,
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
