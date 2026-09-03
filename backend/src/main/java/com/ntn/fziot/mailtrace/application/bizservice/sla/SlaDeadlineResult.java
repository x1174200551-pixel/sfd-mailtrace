package com.ntn.fziot.mailtrace.application.bizservice.sla;

import java.time.LocalDateTime;

public record SlaDeadlineResult(
        Long policyId,
        Integer warningRemainHours,
        Integer escalateAfterBreachHours,
        LocalDateTime responseDeadline,
        LocalDateTime resolveDeadline,
        LocalDateTime responseWarningAt,
        LocalDateTime responseEscalationAt,
        LocalDateTime resolveWarningAt,
        LocalDateTime resolveEscalationAt
) {

    public static SlaDeadlineResult none() {
        return new SlaDeadlineResult(null, null, null, null, null, null, null, null, null);
    }
}
