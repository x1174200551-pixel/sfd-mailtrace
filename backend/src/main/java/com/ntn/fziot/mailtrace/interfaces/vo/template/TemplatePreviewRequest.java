package com.ntn.fziot.mailtrace.interfaces.vo.template;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "模板预览请求")
public class TemplatePreviewRequest {

    @NotBlank(message = "请输入邮件主题")
    @Size(max = 512, message = "邮件主题不能超过 512 个字符")
    private String subjectTpl;

    @NotBlank(message = "请输入邮件正文")
    @Size(max = 10000, message = "邮件正文不能超过 10000 个字符")
    private String contentTpl;

    @Schema(description = "示例数据")
    private Map<String, String> sampleData;
}
