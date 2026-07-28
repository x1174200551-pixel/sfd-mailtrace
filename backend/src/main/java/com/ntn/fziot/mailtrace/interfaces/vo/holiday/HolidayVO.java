package com.ntn.fziot.mailtrace.interfaces.vo.holiday;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "节假日详情")
public record HolidayVO(
        @Schema(description = "节假日ID") Long id,
        @Schema(description = "工作日历ID") Long calendarId,
        @Schema(description = "节假日日期") LocalDate holidayDate,
        @Schema(description = "节假日名称") String holidayName,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
