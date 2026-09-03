package com.ntn.fziot.mailtrace.interfaces.vo.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "企业保存请求")
public class EnterpriseSaveRequest {

    @NotBlank(message = "请输入企业名称")
    @Size(max = 128, message = "企业名称不能超过 128 个字符")
    @Schema(description = "企业名称", example = "示例科技有限公司")
    private String enterpriseName;

    @Size(max = 64, message = "联系人不能超过 64 个字符")
    @Schema(description = "联系人")
    private String contactName;

    @Email(message = "联系邮箱格式不正确")
    @Size(max = 128, message = "联系邮箱不能超过 128 个字符")
    @Schema(description = "联系邮箱")
    private String contactEmail;

    @Size(max = 32, message = "联系电话不能超过 32 个字符")
    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled = true;

    @Schema(description = "是否启用企业飞书群通知；首次配置需测试成功后才能启用")
    private Boolean feishuNotifyEnabled;

    @Size(max = 128, message = "飞书通知群名称不能超过 128 个字符")
    @Schema(description = "飞书通知群名称")
    private String feishuGroupName;

    @Size(max = 1024, message = "飞书 Webhook 不能超过 1024 个字符")
    @Schema(description = "飞书群机器人 Webhook；编辑时留空表示保持原值", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String feishuWebhookUrl;

    @Size(max = 512, message = "飞书签名密钥不能超过 512 个字符")
    @Schema(description = "飞书群机器人签名密钥；编辑时留空表示保持原值", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String feishuSigningSecret;

    @Schema(description = "是否清除企业飞书群机器人配置")
    private Boolean clearFeishuConfig = false;

    @Size(max = 512, message = "备注不能超过 512 个字符")
    @Schema(description = "备注")
    private String remark;
}
