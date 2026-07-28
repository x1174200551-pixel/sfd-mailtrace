package com.ntn.fziot.mailtrace.interfaces.vo.holiday;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "节假日列表响应")
public record HolidayListResponse(
        @Schema(description = "节假日列表") List<HolidayVO> records,
        @Schema(description = "统计摘要") HolidaySummaryVO summary
) {
}
