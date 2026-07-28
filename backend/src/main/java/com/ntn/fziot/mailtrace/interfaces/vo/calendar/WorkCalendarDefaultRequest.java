package com.ntn.fziot.mailtrace.interfaces.vo.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "默认工作日历请求")
public class WorkCalendarDefaultRequest {

    @NotNull(message = "请选择默认状态")
    @Schema(description = "是否默认日历", example = "true")
    private Boolean defaultCalendar;
}
