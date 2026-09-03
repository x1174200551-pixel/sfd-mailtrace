package com.ntn.fziot.mailtrace.application.event;

import com.ntn.fziot.mailtrace.application.bizservice.notification.FeishuNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuImmediateDispatchListener {

    private final FeishuNotificationService feishuNotificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void dispatch(FeishuTaskCreatedEvent event) {
        if (event == null || event.sendLogId() == null) {
            return;
        }
        try {
            feishuNotificationService.processPending(event.sendLogId());
        } catch (RuntimeException exception) {
            // 任务已经持久化，异常时由定时重试任务继续补偿。
            log.error("飞书任务即时派发异常，等待定时重试 sendLogId={}", event.sendLogId(), exception);
        }
    }
}
