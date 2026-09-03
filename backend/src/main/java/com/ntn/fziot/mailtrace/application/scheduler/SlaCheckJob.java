package com.ntn.fziot.mailtrace.application.scheduler;

import com.ntn.fziot.mailtrace.application.bizservice.sla.SlaCheckResult;
import com.ntn.fziot.mailtrace.application.bizservice.sla.SlaCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SLA 定时检查任务：扫描即将超时和已超时工单，并保证提醒状态不重复写入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlaCheckJob {

    private final SlaCheckService slaCheckService;

    /**
     * 固定延迟执行 SLA 检查。
     */
    @Scheduled(
            fixedDelayString = "${mailtrace.sla.check-fixed-delay-ms:60000}",
            initialDelayString = "${mailtrace.sla.check-initial-delay-ms:0}"
    )
    public void checkSlaTickets() {
        // 1、委托业务服务执行扫描和幂等写入。
        SlaCheckResult result = slaCheckService.checkDueTickets(null);

        // 2、输出本轮执行摘要。
        if (result.scannedCount() > 0 || result.warningCount() > 0
                || result.breachCount() > 0 || result.escalationCount() > 0) {
            log.info("SLA 检查完成 scanned={} warning={} breach={} escalation={}",
                    result.scannedCount(), result.warningCount(), result.breachCount(), result.escalationCount());
        }
    }
}
