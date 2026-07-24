package com.ntn.fziot.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "工单回复/备注请求")
public record TicketReplyRequest(
        @Schema(description = "回复内容（纯文本）") String content,
        @Schema(description = "回复内容（HTML 富文本）") String htmlContent,
        @Schema(description = "是否为内部备注（true=内部备注，false=对外回复客户）") Boolean internal,
        @Schema(description = "附件列表") List<AttachmentInfo> attachments
) {
    public record AttachmentInfo(
            @Schema(description = "MinIO objectKey") String objectKey,
            @Schema(description = "文件名") String fileName,
            @Schema(description = "文件大小(字节)") Long fileSize,
            @Schema(description = "MIME 类型") String contentType
    ) {}
}
