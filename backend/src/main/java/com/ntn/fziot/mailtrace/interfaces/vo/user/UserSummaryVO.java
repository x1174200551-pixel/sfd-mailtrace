package com.ntn.fziot.mailtrace.interfaces.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户管理统计")
public record UserSummaryVO(
        @Schema(description = "用户总数") long totalUsers,
        @Schema(description = "启用用户数") long enabledUsers,
        @Schema(description = "停用用户数") long disabledUsers,
        @Schema(description = "管理员数") long adminUsers,
        @Schema(description = "处理人数") long agentUsers
) {
}
