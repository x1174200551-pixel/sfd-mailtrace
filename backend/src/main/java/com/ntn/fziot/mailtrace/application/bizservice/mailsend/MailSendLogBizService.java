package com.ntn.fziot.mailtrace.application.bizservice.mailsend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.log.MailSendLogPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.log.MailSendLogVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailSendLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailSendLogBizService {

    private final MailSendLogMapper mailSendLogMapper;
    private final PermissionService permissionService;
    private final EnterpriseMailboxAccessService enterpriseMailboxAccessService;

    /**
     * 发送日志统计概览（总量，不受筛选影响）。
     */
    public record SendLogStats(long totalCount, long successCount, long failCount) {
        public static SendLogStats empty() {
            return new SendLogStats(0, 0, 0);
        }
    }

    public SendLogStats stats(CurrentUserPrincipal principal) {
        permissionService.assertPermission(principal, "mail_send_log:read", "无权访问发送日志");
        Set<Long> readableMailboxIds = enterpriseMailboxAccessService.resolveReadableMailboxIds(principal);
        if (readableMailboxIds.isEmpty()) {
            return SendLogStats.empty();
        }
        List<MailSendLogEntity> logs = mailSendLogMapper.selectList(
                new LambdaQueryWrapper<MailSendLogEntity>()
                        .in(MailSendLogEntity::getMailboxId, readableMailboxIds));
        return new SendLogStats(
                logs.size(),
                logs.stream().filter(log -> "SUCCESS".equals(log.getSendStatus())).count(),
                logs.stream().filter(log -> "FAILED".equals(log.getSendStatus())).count()
        );
    }

    /**
     * 待处理数量（失败+待发+重试中），用于菜单角标。
     */
    public long pendingCount(CurrentUserPrincipal principal) {
        permissionService.assertPermission(principal, "mail_send_log:read", "无权访问发送日志");
        Set<Long> readableMailboxIds = enterpriseMailboxAccessService.resolveReadableMailboxIds(principal);
        if (readableMailboxIds.isEmpty()) {
            return 0;
        }
        return mailSendLogMapper.selectCount(new LambdaQueryWrapper<MailSendLogEntity>()
                .in(MailSendLogEntity::getMailboxId, readableMailboxIds)
                .in(MailSendLogEntity::getSendStatus, List.of("PENDING", "FAILED", "RETRYING")));
    }

    public MailSendLogPageResponse pageSendLogs(CurrentUserPrincipal principal,
                                                Long enterpriseId, Long mailboxId, String sendType, String sendStatus,
                                                LocalDateTime startFrom, LocalDateTime startTo,
                                                Integer page, Integer size) {
        permissionService.assertPermission(principal, "mail_send_log:read", "无权访问发送日志");

        long currentPage = normalizePage(page);
        long pageSize = normalizeSize(size);
        Set<Long> readableMailboxIds = enterpriseMailboxAccessService.resolveReadableMailboxIds(principal);

        LambdaQueryWrapper<MailSendLogEntity> wrapper = buildQuery(enterpriseId, mailboxId, sendType, sendStatus, startFrom, startTo)
                .orderByDesc(MailSendLogEntity::getId);
        applyMailboxScope(wrapper, readableMailboxIds);

        Page<MailSendLogEntity> result = mailSendLogMapper.selectPage(Page.of(currentPage, pageSize), wrapper);

        List<MailSendLogVO> records = result.getRecords().stream()
                .map(this::toVO)
                .toList();

        return new MailSendLogPageResponse(
                records, result.getTotal(), result.getCurrent(),
                result.getSize(), result.getPages()
        );
    }

    private void applyMailboxScope(LambdaQueryWrapper<MailSendLogEntity> wrapper, Set<Long> mailboxIds) {
        if (mailboxIds.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.in(MailSendLogEntity::getMailboxId, mailboxIds);
    }

    private LambdaQueryWrapper<MailSendLogEntity> buildQuery(Long enterpriseId, Long mailboxId, String sendType, String sendStatus,
                                                             LocalDateTime startFrom, LocalDateTime startTo) {
        LambdaQueryWrapper<MailSendLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (enterpriseId != null) {
            wrapper.eq(MailSendLogEntity::getEnterpriseId, enterpriseId);
        }
        if (mailboxId != null) {
            wrapper.eq(MailSendLogEntity::getMailboxId, mailboxId);
        }
        if (sendType != null && !sendType.isBlank()) {
            wrapper.eq(MailSendLogEntity::getSendType, sendType);
        }
        if (sendStatus != null && !sendStatus.isBlank()) {
            wrapper.eq(MailSendLogEntity::getSendStatus, sendStatus);
        }
        if (startFrom != null) {
            wrapper.ge(MailSendLogEntity::getCreatedAt, startFrom);
        }
        if (startTo != null) {
            wrapper.le(MailSendLogEntity::getCreatedAt, startTo);
        }
        return wrapper;
    }

    private MailSendLogVO toVO(MailSendLogEntity log) {
        return new MailSendLogVO(
                log.getId(), log.getTicketId(), log.getEnterpriseId(), log.getMailboxId(),
                log.getSendType(), log.getTemplateId(), log.getTemplateType(), log.getToAddress(), log.getSubject(),
                log.getContentBody(),
                log.getSendStatus(), log.getRetryCount(), log.getMaxRetry(),
                log.getErrorMessage(), log.getSentAt(), log.getCreatedAt()
        );
    }

    private long normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long normalizeSize(Integer size) {
        if (size == null || size < 1) return 20;
        return Math.min(size, 100);
    }
}
