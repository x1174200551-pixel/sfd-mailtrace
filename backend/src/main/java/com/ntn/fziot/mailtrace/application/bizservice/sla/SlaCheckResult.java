package com.ntn.fziot.mailtrace.application.bizservice.sla;

public record SlaCheckResult(
        int scannedCount,
        int warningCount,
        int breachCount
) {
}
