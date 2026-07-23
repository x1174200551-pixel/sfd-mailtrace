package com.ntn.fziot.mailtrace.interfaces.vo.sysparam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "工单编号规则保存或预览请求")
public class TicketNumberRuleRequest {

    @NotNull(message = "请选择启用状态")
    @Schema(description = "是否启用")
    private Boolean enabled;

    @NotBlank(message = "请输入工单前缀")
    @Size(min = 2, max = 8, message = "工单前缀长度需为 2-8 位")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "工单前缀仅支持大写英文和数字")
    @Schema(description = "工单前缀", example = "TCK")
    private String prefix;

    @NotBlank(message = "请选择日期格式")
    @Pattern(regexp = "^(yyyyMMdd|yyyyMM|yyyy)$", message = "日期格式仅支持 yyyyMMdd、yyyyMM 或 yyyy")
    @Schema(description = "日期格式", example = "yyyyMMdd")
    private String dateFormat;

    @NotNull(message = "请输入流水位数")
    @Min(value = 3, message = "流水位数最少 3 位")
    @Max(value = 8, message = "流水位数最多 8 位")
    @Schema(description = "流水位数", example = "4")
    private Integer seqLength;

    @Pattern(regexp = "^[-_]?$", message = "分隔符仅支持短横线、下划线或不设置")
    @Schema(description = "分隔符", example = "-")
    private String separator;

    @Size(max = 256, message = "参数说明不能超过 256 个字符")
    @Schema(description = "业务说明")
    private String description;
}
