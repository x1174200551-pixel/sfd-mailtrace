package com.ntn.fziot.mailtrace.interfaces.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "重置密码请求")
public class UserResetPasswordRequest {

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 128, message = "密码长度需为 6-128 个字符")
    @Schema(description = "新密码", example = "newpass123")
    private String password;
}
