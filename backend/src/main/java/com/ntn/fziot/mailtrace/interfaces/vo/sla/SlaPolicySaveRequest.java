package com.ntn.fziot.mailtrace.interfaces.vo.sla;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "SLA 策略保存请求")
public class SlaPolicySaveRequest {

    @NotNull(message = "请选择所属企业")
    @Min(value = 1, message = "企业ID需大于 0")
    @Schema(description = "所属企业ID")
    private Long enterpriseId;

    @NotBlank(message = "请输入策略名称")
    @Size(max = 64, message = "策略名称不能超过 64 个字符")
    @Schema(description = "策略名称", example = "标准客服 SLA")
    private String policyName;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled = true;

    @Schema(description = "是否默认策略", example = "false")
    private Boolean defaultPolicy = false;

    @NotNull(message = "请输入首次响应时限")
    @Min(value = 1, message = "首次响应时限需大于 0")
    @Max(value = 9999, message = "首次响应时限不能超过 9999 小时")
    @Schema(description = "首次响应时限（工作小时）", example = "4")
    private Integer responseHours;

    @Min(value = 1, message = "解决时限需大于 0")
    @Max(value = 9999, message = "解决时限不能超过 9999 小时")
    @Schema(description = "解决时限（工作小时，可空）", example = "24")
    private Integer resolveHours;

    @NotNull(message = "请输入预警阈值")
    @Min(value = 1, message = "预警阈值需大于 0")
    @Max(value = 9999, message = "预警阈值不能超过 9999 小时")
    @Schema(description = "即将超时阈值（剩余工作小时）", example = "1")
    private Integer warningRemainHours = 1;

    @Min(value = 1, message = "升级阈值需大于 0")
    @Max(value = 9999, message = "升级阈值不能超过 9999 小时")
    @Schema(description = "超时后升级提醒的工作小时（可空）", example = "2")
    private Integer escalateAfterBreachHours;

    @Schema(description = "是否发送首次响应预警通知", example = "true")
    private Boolean responseWarningNotifyEnabled = true;

    @Schema(description = "是否发送首次响应超时通知", example = "true")
    private Boolean responseBreachNotifyEnabled = true;

    @Schema(description = "是否发送首次响应超时升级通知", example = "false")
    private Boolean responseEscalationNotifyEnabled = false;

    @Schema(description = "是否发送解决预警通知", example = "true")
    private Boolean resolveWarningNotifyEnabled = true;

    @Schema(description = "是否发送解决超时通知", example = "true")
    private Boolean resolveBreachNotifyEnabled = true;

    @Schema(description = "是否发送解决超时升级通知", example = "false")
    private Boolean resolveEscalationNotifyEnabled = false;

    @NotNull(message = "请选择工作日历")
    @Min(value = 1, message = "工作日历ID需大于 0")
    @Schema(description = "工作日历ID", example = "1")
    private Long calendarId;
}
