package com.sfonda.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "工单分页响应")
public record TicketPageResponse(
        @Schema(description = "工单列表") List<TicketSummaryVO> records,
        @Schema(description = "总记录数") Long total,
        @Schema(description = "当前页码") Long page,
        @Schema(description = "每页大小") Long size,
        @Schema(description = "总页数") Long pages
) {
}
