package com.ntn.fziot.mailtrace.interfaces.vo.dashboard;

import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketSummaryVO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "我的待办列表响应")
public record DashboardTodoListResponse(
        @Schema(description = "待办工单列表") List<TicketSummaryVO> records,
        @Schema(description = "待办总数") long totalCount,
        @Schema(description = "处理中数量") long processingCount,
        @Schema(description = "待客户回复数量") long waitingCustomerCount,
        @Schema(description = "SLA 超时数量") long slaOverdueCount,
        @Schema(description = "返回条数限制") int limit
) {
}
