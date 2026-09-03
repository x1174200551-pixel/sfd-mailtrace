package com.ntn.fziot.mailtrace.interfaces.vo.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "企业飞书通知群测试结果")
public record FeishuGroupTestResponse(
        @Schema(description = "飞书是否接受消息") boolean accepted,
        @Schema(description = "结果提示") String message,
        @Schema(description = "发送日志ID") Long sendLogId
) {
}
