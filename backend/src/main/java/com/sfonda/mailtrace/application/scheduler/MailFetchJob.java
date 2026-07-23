package com.sfonda.mailtrace.application.scheduler;

import com.sfonda.mailtrace.application.bizservice.mailfetch.MailFetchBizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * IMAP 定时拉信任务：每分钟扫描到期邮箱并写入 fetch_log。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailFetchJob {

    private final MailFetchBizService mailFetchBizService;

    /**
     * 固定延迟 60 秒执行一轮到期邮箱拉取。
     */
    @Scheduled(fixedDelayString = "${mailtrace.mail.fetch-fixed-delay-ms:60000}")
    public void fetchDueMailboxes() {
        // 1、查询到期邮箱，经代理逐个拉取，保证事务生效
        List<Long> dueMailboxIds = mailFetchBizService.listDueMailboxIds();
        if (dueMailboxIds.isEmpty()) {
            return;
        }
        List<Long> logIds = new ArrayList<>();
        for (Long mailboxId : dueMailboxIds) {
            try {
                logIds.add(mailFetchBizService.fetchMailbox(mailboxId, MailFetchBizService.TRIGGER_SCHEDULED));
            } catch (Exception exception) {
                log.warn("IMAP 定时拉信跳过 mailboxId={} reason={}", mailboxId, exception.getMessage());
            }
        }
        // 2、输出本轮执行摘要
        log.info("IMAP 定时拉信完成，本轮邮箱数={} 日志数={} ids={}", dueMailboxIds.size(), logIds.size(), logIds);
    }
}
