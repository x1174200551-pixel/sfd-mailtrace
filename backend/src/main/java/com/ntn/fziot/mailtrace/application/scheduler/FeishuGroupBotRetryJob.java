package com.ntn.fziot.mailtrace.application.scheduler;

import com.ntn.fziot.mailtrace.application.bizservice.notification.FeishuNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuGroupBotRetryJob {

    private final FeishuNotificationService feishuNotificationService;

    @Scheduled(
            fixedDelayString = "${mailtrace.feishu.group-bot.retry-fixed-delay-ms:60000}",
            initialDelayString = "${mailtrace.feishu.group-bot.retry-initial-delay-ms:15000}"
    )
    public void sendPendingMessages() {
        FeishuNotificationService.BatchResult result = feishuNotificationService.processPendingBatch();
        if (result.total() > 0) {
            log.info("飞书群通知任务完成：本轮={}，成功={}，失败={}",
                    result.total(), result.success(), result.failed());
        }
    }
}
