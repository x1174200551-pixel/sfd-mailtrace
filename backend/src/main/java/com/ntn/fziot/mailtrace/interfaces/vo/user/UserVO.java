package com.ntn.fziot.mailtrace.interfaces.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "用户管理列表项")
public record UserVO(
        @Schema(description = "用户ID") Long id,
        @Schema(description = "账号") String account,
        @Schema(description = "显示名称") String displayName,
        @Schema(description = "邮箱") String email,
        @Schema(description = "角色编码") String roleCode,
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "最近登录时间") LocalDateTime lastLoginAt,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
