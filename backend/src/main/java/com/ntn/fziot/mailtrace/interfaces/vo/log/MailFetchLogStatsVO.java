package com.ntn.fziot.mailtrace.interfaces.vo.log;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "拉取日志统计概览")
public record MailFetchLogStatsVO(
        @Schema(description = "总拉取次数") long totalCount,
        @Schema(description = "成功次数") long successCount,
        @Schema(description = "失败次数") long failCount,
        @Schema(description = "新建工单总数") long totalCreatedTickets
) {
}
