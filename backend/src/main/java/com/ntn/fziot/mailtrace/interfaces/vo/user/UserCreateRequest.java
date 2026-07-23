package com.ntn.fziot.mailtrace.interfaces.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "新建用户请求")
public class UserCreateRequest {

    @NotBlank(message = "请输入账号")
    @Pattern(regexp = "^[A-Za-z0-9._-]{2,64}$", message = "账号仅支持 2-64 位字母、数字、点、下划线或中划线")
    @Schema(description = "登录账号", example = "agent01")
    private String account;

    @NotBlank(message = "请输入姓名")
    @Size(max = 64, message = "姓名不能超过 64 个字符")
    @Schema(description = "显示名称", example = "客服一号")
    private String displayName;

    @NotBlank(message = "请输入邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱不能超过 128 个字符")
    @Schema(description = "邮箱", example = "agent01@ntn.fziot")
    private String email;

    @NotBlank(message = "请选择角色")
    @Schema(description = "角色编码：ADMIN/AGENT", example = "AGENT")
    private String roleCode;

    @NotBlank(message = "请输入初始密码")
    @Size(min = 6, max = 128, message = "密码长度需为 6-128 个字符")
    @Schema(description = "初始密码", example = "agent123")
    private String password;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled = true;
}
