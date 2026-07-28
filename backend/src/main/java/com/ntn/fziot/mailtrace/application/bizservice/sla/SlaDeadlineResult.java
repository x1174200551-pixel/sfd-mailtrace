package com.ntn.fziot.mailtrace.application.bizservice.sla;

import java.time.LocalDateTime;

public record SlaDeadlineResult(
        Long policyId,
        LocalDateTime responseDeadline,
        LocalDateTime resolveDeadline
) {

    public static SlaDeadlineResult none() {
        return new SlaDeadlineResult(null, null, null);
    }
}
