package com.ntn.fziot.mailtrace.interfaces.vo.assignment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "分配规则测试匹配请求")
public class AssignmentRuleTestRequest {

    @NotNull(message = "请选择来源邮箱")
    @Schema(description = "来源邮箱ID")
    private Long mailboxId;

    @Size(max = 256, message = "来源邮箱地址不能超过 256 个字符")
    @Schema(description = "来源邮箱地址；未传时仅按邮箱ID匹配")
    private String mailboxAddress;

    @Size(max = 256, message = "邮件主题不能超过 256 个字符")
    @Schema(description = "邮件主题")
    private String subject;

    @Size(max = 256, message = "发件人邮箱不能超过 256 个字符")
    @Schema(description = "发件人邮箱")
    private String fromEmail;
}
