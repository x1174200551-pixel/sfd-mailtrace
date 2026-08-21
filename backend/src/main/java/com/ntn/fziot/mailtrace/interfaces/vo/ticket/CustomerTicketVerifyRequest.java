package com.ntn.fziot.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "客户工单访问校验请求")
public record CustomerTicketVerifyRequest(
        @NotBlank(message = "请输入校验码")
        @Schema(description = "客户访问校验码")
        String accessCode
) {
}
