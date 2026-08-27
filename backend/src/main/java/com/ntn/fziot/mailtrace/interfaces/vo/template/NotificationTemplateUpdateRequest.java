package com.ntn.fziot.mailtrace.interfaces.vo.template;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新通知模板请求")
public class NotificationTemplateUpdateRequest {

    @NotBlank(message = "请选择模板类型")
    @Pattern(regexp = "AUTO_REPLY|ASSIGN_NOTIFY|AGENT_REPLY|SLA_WARNING|SLA_BREACH|SYSTEM",
            message = "模板类型不支持")
    @Schema(description = "模板类型：AUTO_REPLY/ASSIGN_NOTIFY/AGENT_REPLY/SLA_WARNING/SLA_BREACH/SYSTEM")
    private String templateType;

    @NotBlank(message = "请输入模板名称")
    @Size(max = 64, message = "模板名称不能超过 64 个字符")
    @Schema(description = "模板名称")
    private String templateName;

    @NotBlank(message = "请输入邮件主题")
    @Size(max = 512, message = "邮件主题不能超过 512 个字符")
    @Schema(description = "主题模板")
    private String subjectTpl;

    @NotBlank(message = "请输入邮件正文")
    @Size(max = 10000, message = "邮件正文不能超过 10000 个字符")
    @Schema(description = "正文模板")
    private String contentTpl;

    @NotNull(message = "请选择启用状态")
    @Schema(description = "是否启用")
    private Boolean enabled;
}
