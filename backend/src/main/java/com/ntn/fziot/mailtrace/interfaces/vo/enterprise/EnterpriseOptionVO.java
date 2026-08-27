package com.ntn.fziot.mailtrace.interfaces.vo.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "企业选项")
public record EnterpriseOptionVO(
        @Schema(description = "企业ID") Long id,
        @Schema(description = "企业名称") String enterpriseName,
        @Schema(description = "是否启用") Boolean enabled
) {
}
