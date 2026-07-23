package com.ntn.fziot.mailtrace.application.bizservice.mailfetch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.infrastructure.crypto.MailPasswordCipher;
import com.ntn.fziot.mailtrace.infrastructure.mail.ImapFetchClient;
import com.ntn.fziot.mailtrace.infrastructure.mail.ImapFetchConfig;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailFetchLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailFetchLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * IMAP 拉信业务服务：按启用邮箱执行拉取，并写入 fetch_log。
 * 本阶段只统计未读邮件数并记录日志，不解析建单。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailFetchBizService {

    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    public static final String TRIGGER_MANUAL = "MANUAL";
    private static final String STATUS_OK = "OK";
    private static final String STATUS_ERROR = "ERROR";
    private static final String OPERATOR_SYSTEM = "system";
    private static final int DEFAULT_FETCH_INTERVAL_SEC = 120;
    private static final int MAX_ERROR_LENGTH = 2000;

    private final MailboxMapper mailboxMapper;
    private final MailFetchLogMapper mailFetchLogMapper;
    private final MailPasswordCipher mailPasswordCipher;
    private final ImapFetchClient imapFetchClient;

    /**
     * 查询当前到期、需要执行拉取的启用邮箱 ID。
     */
    public List<Long> listDueMailboxIds() {
        // 1、查询启用中的邮箱
        LocalDateTime now = LocalDateTime.now();
        List<MailboxEntity> enabledMailboxes = mailboxMapper.selectList(new LambdaQueryWrapper<MailboxEntity>()
                .eq(MailboxEntity::getEnabled, true)
                .orderByAsc(MailboxEntity::getId));
        // 2、按拉取间隔过滤出到期邮箱
        return enabledMailboxes.stream()
                .filter(mailbox -> isDue(mailbox, now))
                .map(MailboxEntity::getId)
                .toList();
    }

    /**
     * 对指定邮箱执行一次拉取。
     *
     * @param mailboxId   邮箱 ID
     * @param triggerType SCHEDULED / MANUAL
     * @return 拉取日志 ID
     */
    @Transactional
    public Long fetchMailbox(Long mailboxId, String triggerType) {
        // 1、加载邮箱配置，准备拉取日志起始时间
        MailboxEntity mailbox = mailboxMapper.selectById(mailboxId);
        if (mailbox == null) {
            throw new IllegalArgumentException("邮箱配置不存在：" + mailboxId);
        }
        if (!Boolean.TRUE.equals(mailbox.getEnabled())) {
            throw new IllegalStateException("邮箱已停用，跳过拉取：" + mailbox.getEmailAddress());
        }

        String normalizedTrigger = TRIGGER_MANUAL.equals(triggerType) ? TRIGGER_MANUAL : TRIGGER_SCHEDULED;
        LocalDateTime startedAt = LocalDateTime.now();
        MailFetchLogEntity logEntity = new MailFetchLogEntity();
        logEntity.setMailboxId(mailbox.getId());
        logEntity.setTriggerType(normalizedTrigger);
        logEntity.setStartedAt(startedAt);
        logEntity.setFetchedCount(0);
        logEntity.setCreatedTicketCount(0);
        logEntity.setLinkedCount(0);
        logEntity.setCreatedBy(OPERATOR_SYSTEM);
        logEntity.setUpdatedBy(OPERATOR_SYSTEM);

        long start = System.currentTimeMillis();
        try {
            // 2、解密 IMAP 授权码并连接统计未读邮件数
            String password = mailPasswordCipher.decrypt(mailbox.getImapPasswordEnc());
            ImapFetchConfig config = new ImapFetchConfig(
                    mailbox.getImapHost(),
                    mailbox.getImapPort() == null ? 993 : mailbox.getImapPort(),
                    mailbox.getImapSslEnabled() == null || mailbox.getImapSslEnabled(),
                    mailbox.getImapUsername(),
                    password,
                    mailbox.getImapFolder() == null || mailbox.getImapFolder().isBlank() ? "INBOX" : mailbox.getImapFolder()
            );
            int fetchedCount = imapFetchClient.countUnseenMessages(config);

            // 3、写成功日志，并回写邮箱最近拉取时间与连接状态
            LocalDateTime finishedAt = LocalDateTime.now();
            long elapsed = System.currentTimeMillis() - start;
            log.info("IMAP 拉取成功 mailboxId={} email={} host={} 未读数={} trigger={} 耗时={}ms",
                    mailbox.getId(), mailbox.getEmailAddress(), mailbox.getImapHost(),
                    fetchedCount, normalizedTrigger, elapsed);
            logEntity.setFinishedAt(finishedAt);
            logEntity.setSuccess(true);
            logEntity.setFetchedCount(fetchedCount);
            logEntity.setErrorMessage(null);
            mailFetchLogMapper.insert(logEntity);

            mailbox.setLastFetchAt(finishedAt);
            mailbox.setConnectionStatus(STATUS_OK);
            mailbox.setUpdatedBy(OPERATOR_SYSTEM);
            mailbox.setUpdatedAt(finishedAt);
            mailboxMapper.updateById(mailbox);
            return logEntity.getId();
        } catch (Exception exception) {
            // 4、连接或拉取失败时记录错误，并标记邮箱连接异常
            LocalDateTime finishedAt = LocalDateTime.now();
            long elapsed = System.currentTimeMillis() - start;
            String errorMessage = truncateError(exception);
            log.warn("IMAP 拉取失败 mailboxId={} email={} host={} trigger={} 耗时={}ms reason={}",
                    mailbox.getId(), mailbox.getEmailAddress(), mailbox.getImapHost(),
                    normalizedTrigger, elapsed, errorMessage);

            logEntity.setFinishedAt(finishedAt);
            logEntity.setSuccess(false);
            logEntity.setFetchedCount(0);
            logEntity.setCreatedTicketCount(0);
            logEntity.setLinkedCount(0);
            logEntity.setErrorMessage(errorMessage);
            mailFetchLogMapper.insert(logEntity);

            mailbox.setLastFetchAt(finishedAt);
            mailbox.setConnectionStatus(STATUS_ERROR);
            mailbox.setUpdatedBy(OPERATOR_SYSTEM);
            mailbox.setUpdatedAt(finishedAt);
            mailboxMapper.updateById(mailbox);
            return logEntity.getId();
        }
    }

    /**
     * 判断邮箱是否到达拉取周期。
     */
    boolean isDue(MailboxEntity mailbox, LocalDateTime now) {
        // 1、未拉取过的启用邮箱立即到期
        if (mailbox.getLastFetchAt() == null) {
            return true;
        }
        // 2、按配置的拉取间隔（秒）判断是否超过下次可拉时间
        int intervalSec = mailbox.getFetchIntervalSec() == null || mailbox.getFetchIntervalSec() <= 0
                ? DEFAULT_FETCH_INTERVAL_SEC
                : mailbox.getFetchIntervalSec();
        return !mailbox.getLastFetchAt().plusSeconds(intervalSec).isAfter(now);
    }

    private String truncateError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        String normalized = "IMAP 拉取失败：" + message.trim();
        if (normalized.length() <= MAX_ERROR_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_ERROR_LENGTH);
    }
}
