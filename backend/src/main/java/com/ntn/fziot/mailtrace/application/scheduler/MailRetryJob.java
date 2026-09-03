package com.ntn.fziot.mailtrace.application.scheduler;

import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailDeliveryStateService;
import com.ntn.fziot.mailtrace.application.bizservice.ticket.TicketReplyDeliveryService;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailSendLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.time.LocalDateTime;

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
    private final MailDeliveryStateService deliveryStateService;
    private final TicketReplyDeliveryService ticketReplyDeliveryService;

    /**
     * 每 5 分钟扫描一次失败记录，重试未超限的邮件。
     */
    @Scheduled(
            fixedDelayString = "${mailtrace.mail.retry-fixed-delay-ms:300000}",
            initialDelayString = "${mailtrace.mail.retry-initial-delay-ms:0}"
    )
    public void retryFailedMails() {
        int unknownCount = deliveryStateService.markStaleUnknown(LocalDateTime.now().minusMinutes(10));
        if (unknownCount > 0) {
            log.warn("发现 {} 条超时中的邮件任务，已标记为投递结果未知并停止自动重试", unknownCount);
        }
        reconcileReplyCompletion();
        dispatchCommittedPending();

        List<MailSendLogEntity> failedList = mailSendLogMapper.selectFailedForRetry(BATCH_SIZE);
        if (failedList.isEmpty()) {
            return;
        }

        log.info("发送重试任务：本轮待重试 {} 条", failedList.size());
        int successCount = 0;
        int failCount = 0;

        for (MailSendLogEntity logEntity : failedList) {
            try {
                MailSendService.SendResult result = mailSendService.retrySend(logEntity.getId());
                if (result.success()) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (RuntimeException exception) {
                failCount++;
                log.error("发送重试任务单条执行异常 sendLogId={}", logEntity.getId(), exception);
            }
        }

        log.info("发送重试任务完成：本轮 {} 条，成功={}，失败={}", failedList.size(), successCount, failCount);
    }

    private void dispatchCommittedPending() {
        List<MailSendLogEntity> pendingList = mailSendLogMapper.selectPendingForDispatch(
                LocalDateTime.now().minusMinutes(1), BATCH_SIZE);
        for (MailSendLogEntity sendLog : pendingList) {
            try {
                mailSendService.dispatchPending(sendLog.getId());
            } catch (RuntimeException exception) {
                log.error("事务提交后的待发送邮件补偿调度失败 sendLogId={}", sendLog.getId(), exception);
            }
        }
    }

    private void reconcileReplyCompletion() {
        List<MailSendLogEntity> pendingList = mailSendLogMapper.selectReplyCompletionPending(BATCH_SIZE);
        for (MailSendLogEntity sendLog : pendingList) {
            try {
                ticketReplyDeliveryService.completeBySendLog(sendLog.getId());
            } catch (RuntimeException exception) {
                log.error("客服回复工单状态补偿失败 sendLogId={} ticketId={}",
                        sendLog.getId(), sendLog.getTicketId(), exception);
            }
        }
    }
}
