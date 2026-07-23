package com.ntn.fziot.mailtrace.interfaces.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "当前用户信息")
public class CurrentUserVO {

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "账号")
    private String account;

    @Schema(description = "显示名称")
    private String displayName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "角色码")
    private String roleCode;
}
