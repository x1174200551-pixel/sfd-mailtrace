package com.ntn.fziot.mailtrace.interfaces.vo.assignment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "分配规则组保存请求")
public class AssignmentRuleGroupSaveRequest {

    @NotNull(message = "请选择所属企业")
    @Min(value = 1, message = "企业ID需大于 0")
    @Schema(description = "所属企业ID")
    private Long enterpriseId;

    @NotBlank(message = "请输入规则组名称")
    @Size(max = 128, message = "规则组名称不能超过 128 个字符")
    @Schema(description = "规则组名称")
    private String groupName;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled = true;

    @Size(max = 512, message = "备注不能超过 512 个字符")
    @Schema(description = "备注")
    private String remark;
}
