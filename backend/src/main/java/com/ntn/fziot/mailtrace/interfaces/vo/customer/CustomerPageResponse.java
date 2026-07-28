package com.ntn.fziot.mailtrace.interfaces.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "客户分页响应")
public record CustomerPageResponse(
        @Schema(description = "客户列表") List<CustomerVO> records,
        @Schema(description = "总记录数") Long total,
        @Schema(description = "当前页码") Long page,
        @Schema(description = "每页大小") Long size,
        @Schema(description = "总页数") Long pages
) {
}
