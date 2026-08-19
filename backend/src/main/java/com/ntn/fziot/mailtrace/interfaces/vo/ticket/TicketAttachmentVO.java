package com.ntn.fziot.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "工单附件")
public record TicketAttachmentVO(
        @Schema(description = "附件ID") Long id,
        @Schema(description = "关联消息ID") Long messageId,
        @Schema(description = "文件名") String fileName,
        @Schema(description = "文件大小(字节)") Long fileSize,
        @Schema(description = "MIME 类型") String contentType,
        @Schema(description = "下载URL") String downloadUrl,
        @Schema(description = "是否内嵌资源") Boolean isInline,
        @Schema(description = "Content-ID") String contentId,
        @Schema(description = "上传人") String uploadedBy,
        @Schema(description = "上传时间") LocalDateTime createdAt
) {
}
