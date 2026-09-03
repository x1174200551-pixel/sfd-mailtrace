package com.ntn.fziot.mailtrace.interfaces.vo.sla;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "SLA 策略详情")
public record SlaPolicyVO(
        @Schema(description = "策略ID") Long id,
        @Schema(description = "所属企业ID") Long enterpriseId,
        @Schema(description = "策略名称") String policyName,
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "是否默认策略") Boolean defaultPolicy,
        @Schema(description = "首次响应时限（工作小时）") Integer responseHours,
        @Schema(description = "解决时限（工作小时）") Integer resolveHours,
        @Schema(description = "即将超时阈值（剩余工作小时）") Integer warningRemainHours,
        @Schema(description = "超时后升级提醒的工作小时") Integer escalateAfterBreachHours,
        @Schema(description = "是否发送首次响应预警通知") Boolean responseWarningNotifyEnabled,
        @Schema(description = "是否发送首次响应超时通知") Boolean responseBreachNotifyEnabled,
        @Schema(description = "是否发送首次响应超时升级通知") Boolean responseEscalationNotifyEnabled,
        @Schema(description = "是否发送解决预警通知") Boolean resolveWarningNotifyEnabled,
        @Schema(description = "是否发送解决超时通知") Boolean resolveBreachNotifyEnabled,
        @Schema(description = "是否发送解决超时升级通知") Boolean resolveEscalationNotifyEnabled,
        @Schema(description = "工作日历ID") Long calendarId,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
