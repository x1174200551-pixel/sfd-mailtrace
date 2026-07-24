package com.ntn.fziot.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "工单备注更新请求")
public record TicketRemarkRequest(
        @Schema(description = "备注内容") String remark
) {
}
