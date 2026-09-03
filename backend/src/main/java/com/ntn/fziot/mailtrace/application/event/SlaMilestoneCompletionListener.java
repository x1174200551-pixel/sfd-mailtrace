package com.ntn.fziot.mailtrace.application.event;

import com.ntn.fziot.mailtrace.application.bizservice.sla.SlaCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlaMilestoneCompletionListener {

    private final SlaCheckService slaCheckService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void compensate(SlaMilestoneCompletedEvent event) {
        if (event == null || event.ticketId() == null) {
            return;
        }
        try {
            slaCheckService.checkTicket(event.ticketId(), event.completedAt());
        } catch (RuntimeException exception) {
            // 定时 SLA 扫描仍会继续补偿，不能反向影响已经完成的回复或关单事务。
            log.error("SLA 里程碑补偿异常，等待定时扫描 ticketId={}", event.ticketId(), exception);
        }
    }
}
