package com.ntn.fziot.mailtrace.interfaces.vo.assignment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "分配规则保存请求")
public class AssignmentRuleSaveRequest {

    @NotBlank(message = "请输入规则名称")
    @Size(max = 64, message = "规则名称不能超过 64 个字符")
    @Schema(description = "规则名称", example = "VIP 客户优先分配")
    private String ruleName;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled = true;

    @Min(value = 1, message = "优先级需大于 0")
    @Max(value = 9999, message = "优先级不能超过 9999")
    @Schema(description = "匹配优先级，数字越小越优先", example = "10")
    private Integer priorityOrder = 100;

    @Schema(description = "是否默认规则", example = "false")
    private Boolean defaultRule = false;

    @NotBlank(message = "请选择匹配类型")
    @Schema(description = "匹配类型：DEFAULT/SUBJECT_KEYWORD/MAILBOX/FROM_EMAIL", example = "SUBJECT_KEYWORD")
    private String matchType;

    @Size(max = 256, message = "匹配值不能超过 256 个字符")
    @Schema(description = "匹配值；默认规则可为空")
    private String matchValue;

    @NotNull(message = "请选择分配处理人")
    @Schema(description = "分配目标处理人用户ID")
    private Long assigneeId;

    @Schema(description = "匹配后是否通知处理人", example = "true")
    private Boolean notifyEnabled = true;
}
