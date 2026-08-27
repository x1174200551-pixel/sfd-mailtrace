package com.ntn.fziot.mailtrace.interfaces.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "用户企业或邮箱数据授权")
public record UserDataGrantVO(
        @Schema(description = "授权ID") Long id,
        @Schema(description = "用户ID") Long userId,
        @Schema(description = "授权类型：ENTERPRISE/MAILBOX") String grantType,
        @Schema(description = "企业ID") Long enterpriseId,
        @Schema(description = "邮箱ID") Long mailboxId,
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "备注") String remark,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
