package com.ntn.fziot.mailtrace.application.bizservice.assignment;

public record AssignmentRuleMatchResult(
        Long groupId,
        Long ruleId,
        String ruleName,
        String matchType,
        String matchValue,
        Long assigneeId,
        String assigneeName,
        String assigneeEmail,
        Boolean notifyEnabled
) {
}
