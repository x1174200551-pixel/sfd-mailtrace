package com.sfonda.mailtrace.interfaces.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @NotBlank(message = "请输入账号")
    @Size(max = 64, message = "账号不能超过 64 个字符")
    @Schema(description = "账号或邮箱", example = "admin")
    private String account;

    @NotBlank(message = "请输入密码")
    @Size(max = 128, message = "密码不能超过 128 个字符")
    @Schema(description = "密码", example = "admin123")
    private String password;

    @Schema(description = "是否记住登录", example = "true")
    private Boolean rememberMe = false;
}
