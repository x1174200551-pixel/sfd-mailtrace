package com.ntn.fziot.mailtrace.interfaces.vo.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "企业详情")
public record EnterpriseVO(
        @Schema(description = "企业ID") Long id,
        @Schema(description = "企业名称") String enterpriseName,
        @Schema(description = "联系人") String contactName,
        @Schema(description = "联系邮箱") String contactEmail,
        @Schema(description = "联系电话") String contactPhone,
        @Schema(description = "企业下邮箱数") Long mailboxCount,
        @Schema(description = "企业下工单数") Long ticketCount,
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "备注") String remark,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
}
