package com.ntn.fziot.mailtrace.interfaces.vo.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "工作日历保存请求")
public class WorkCalendarSaveRequest {

    @NotBlank(message = "请输入日历名称")
    @Size(max = 64, message = "日历名称不能超过 64 个字符")
    @Schema(description = "日历名称", example = "标准工作日历")
    private String calendarName;

    @NotBlank(message = "请输入时区")
    @Size(max = 64, message = "时区不能超过 64 个字符")
    @Schema(description = "时区", example = "Asia/Shanghai")
    private String timezone = "Asia/Shanghai";

    @NotEmpty(message = "请选择工作日")
    @Schema(description = "工作日，1=周一...7=周日", example = "[1,2,3,4,5]")
    private List<Integer> workdays;

    @NotBlank(message = "请输入工作开始时间")
    @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$", message = "工作开始时间格式应为 HH:mm 或 HH:mm:ss")
    @Schema(description = "每日工作开始时间", example = "09:00")
    private String workStartTime;

    @NotBlank(message = "请输入工作结束时间")
    @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$", message = "工作结束时间格式应为 HH:mm 或 HH:mm:ss")
    @Schema(description = "每日工作结束时间", example = "18:00")
    private String workEndTime;

    @Schema(description = "是否默认日历", example = "false")
    private Boolean defaultCalendar = false;
}
