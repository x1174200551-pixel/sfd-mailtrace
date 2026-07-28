package com.ntn.fziot.mailtrace.interfaces.vo.holiday;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "节假日统计摘要")
public record HolidaySummaryVO(
        @Schema(description = "总数") long totalCount
) {
}
