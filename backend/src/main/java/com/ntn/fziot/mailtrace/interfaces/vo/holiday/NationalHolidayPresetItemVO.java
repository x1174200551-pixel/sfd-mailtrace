package com.ntn.fziot.mailtrace.interfaces.vo.holiday;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "国家节假日模板明细")
public record NationalHolidayPresetItemVO(
        @Schema(description = "节假日日期") LocalDate holidayDate,
        @Schema(description = "节假日名称") String holidayName
) {
}
