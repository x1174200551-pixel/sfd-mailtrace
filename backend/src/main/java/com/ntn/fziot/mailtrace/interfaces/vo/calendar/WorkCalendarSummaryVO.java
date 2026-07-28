package com.ntn.fziot.mailtrace.interfaces.vo.calendar;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工作日历统计摘要")
public record WorkCalendarSummaryVO(
        @Schema(description = "总数") long totalCount,
        @Schema(description = "默认日历数") long defaultCount
) {
}
