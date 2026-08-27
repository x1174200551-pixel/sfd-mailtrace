package com.ntn.fziot.mailtrace.interfaces.vo.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "企业列表响应")
public record EnterpriseListResponse(
        @Schema(description = "企业列表") List<EnterpriseVO> records,
        @Schema(description = "当前筛选条件下的总条数") long total,
        @Schema(description = "当前页码") long page,
        @Schema(description = "每页条数") long size,
        @Schema(description = "总页数") long pages,
        @Schema(description = "企业总数") long totalCount,
        @Schema(description = "启用企业数") long enabledCount,
        @Schema(description = "停用企业数") long disabledCount
) {
}
