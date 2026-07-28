package com.ntn.fziot.mailtrace.interfaces.vo.holiday;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "国家法定节假日模板响应")
public record NationalHolidayPresetResponse(
        @Schema(description = "模板年份") Integer year,
        @Schema(description = "数据来源名称") String sourceName,
        @Schema(description = "数据来源链接") String sourceUrl,
        @Schema(description = "已维护模板年份") List<Integer> supportedYears,
        @Schema(description = "放假日期列表") List<NationalHolidayPresetItemVO> records,
        @Schema(description = "补班日期列表，当前节假日导入不写入") List<LocalDate> makeupWorkdayDates
) {
}
