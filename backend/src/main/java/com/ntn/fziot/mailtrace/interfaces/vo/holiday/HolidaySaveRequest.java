package com.ntn.fziot.mailtrace.interfaces.vo.holiday;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "节假日保存请求")
public class HolidaySaveRequest {

    @NotNull(message = "请选择工作日历")
    @Schema(description = "工作日历ID", example = "1")
    private Long calendarId;

    @NotNull(message = "请选择节假日日期")
    @Schema(description = "节假日日期", example = "2026-10-01")
    private LocalDate holidayDate;

    @NotBlank(message = "请输入节假日名称")
    @Size(max = 64, message = "节假日名称不能超过 64 个字符")
    @Schema(description = "节假日名称", example = "国庆节")
    private String holidayName;
}
