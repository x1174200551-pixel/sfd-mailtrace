package com.ntn.fziot.mailtrace.interfaces.vo.log;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "发送日志分页响应")
public record MailSendLogPageResponse(
        @Schema(description = "日志列表") java.util.List<MailSendLogVO> records,
        @Schema(description = "总记录数") Long total,
        @Schema(description = "当前页码") Long page,
        @Schema(description = "每页大小") Long size,
        @Schema(description = "总页数") Long pages
) {
}
