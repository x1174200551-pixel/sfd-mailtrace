package com.ntn.fziot.mailtrace.interfaces.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工作台统计摘要")
public record DashboardSummaryVO(
        @Schema(description = "工单总数") long totalCount,
        @Schema(description = "待分配工单数") long pendingAssignCount,
        @Schema(description = "处理中工单数") long processingCount,
        @Schema(description = "待客户回复工单数") long waitingCustomerCount,
        @Schema(description = "SLA 超时工单数") long slaOverdueCount,
        @Schema(description = "今日关闭工单数") long closedTodayCount,
        @Schema(description = "活跃工单数，待分配+处理中+待客户回复") long activeCount
) {
}
