package com.ntn.fziot.mailtrace.interfaces.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "客户只读详情")
public record CustomerVO(
        @Schema(description = "客户ID，来自客户档案；仅工单聚合客户可能为空") Long id,
        @Schema(description = "客户邮箱") String email,
        @Schema(description = "客户显示名") String displayName,
        @Schema(description = "最近来信时间") LocalDateTime lastMailAt,
        @Schema(description = "关联工单数") Long ticketCount,
        @Schema(description = "备注") String remark,
        @Schema(description = "创建时间") LocalDateTime createdAt
) {
}
