package com.ntn.fziot.mailtrace.application.event;

import java.time.LocalDateTime;

public record SlaMilestoneCompletedEvent(Long ticketId, LocalDateTime completedAt) {
}
