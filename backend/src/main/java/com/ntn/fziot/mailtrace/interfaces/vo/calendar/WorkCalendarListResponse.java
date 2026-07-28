package com.ntn.fziot.mailtrace.interfaces.vo.calendar;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "工作日历列表响应")
public record WorkCalendarListResponse(
        @Schema(description = "日历列表") List<WorkCalendarVO> records,
        @Schema(description = "统计摘要") WorkCalendarSummaryVO summary
) {
}
