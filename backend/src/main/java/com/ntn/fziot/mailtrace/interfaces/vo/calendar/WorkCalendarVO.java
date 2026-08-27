package com.ntn.fziot.mailtrace.interfaces.vo.calendar;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "工作日历详情")
public record WorkCalendarVO(
        @Schema(description = "日历ID") Long id,
        @Schema(description = "所属企业ID") Long enterpriseId,
        @Schema(description = "日历名称") String calendarName,
        @Schema(description = "时区") String timezone,
        @Schema(description = "工作日，1=周一...7=周日") List<Integer> workdays,
        @Schema(description = "每日工作开始时间") String workStartTime,
        @Schema(description = "每日工作结束时间") String workEndTime,
        @Schema(description = "是否默认日历") Boolean defaultCalendar,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
