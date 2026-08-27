package com.ntn.fziot.mailtrace.application.scheduler;

import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailSendLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 发送重试定时任务：定期扫描失败邮件进行重试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailRetryJob {

    private static final int BATCH_SIZE = 20;

    private final MailSendLogMapper mailSendLogMapper;
    private final MailSendService mailSendService;

    /**
     * 每 5 分钟扫描一次失败记录，重试未超限的邮件。
     */
    @Scheduled(
            fixedDelayString = "${mailtrace.mail.retry-fixed-delay-ms:300000}",
            initialDelayString = "${mailtrace.mail.retry-initial-delay-ms:0}"
    )
    public void retryFailedMails() {
        List<MailSendLogEntity> failedList = mailSendLogMapper.selectFailedForRetry(BATCH_SIZE);
        if (failedList.isEmpty()) {
            return;
        }

        log.info("发送重试任务：本轮待重试 {} 条", failedList.size());
        int successCount = 0;
        int failCount = 0;

        for (MailSendLogEntity logEntity : failedList) {
            MailSendService.SendResult result = mailSendService.retrySend(logEntity.getId());
            if (result.success()) {
                successCount++;
            } else {
                failCount++;
            }
        }

        log.info("发送重试任务完成：本轮 {} 条，成功={}，失败={}", failedList.size(), successCount, failCount);
    }
}
