package com.ntn.fziot.mailtrace.interfaces.vo.mailbox;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "邮箱分页响应")
public record MailboxPageResponse(
        @Schema(description = "邮箱列表") List<MailboxVO> records,
        @Schema(description = "总条数") long total,
        @Schema(description = "当前页") long page,
        @Schema(description = "每页条数") long size,
        @Schema(description = "总页数") long pages,
        @Schema(description = "统计摘要") MailboxSummaryVO summary
) {
}
