package com.sfonda.mailtrace.interfaces.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "用户分页响应")
public record UserPageResponse(
        @Schema(description = "用户列表") List<UserVO> records,
        @Schema(description = "总条数") long total,
        @Schema(description = "当前页") long page,
        @Schema(description = "每页条数") long size,
        @Schema(description = "总页数") long pages,
        @Schema(description = "统计摘要") UserSummaryVO summary
) {
}
