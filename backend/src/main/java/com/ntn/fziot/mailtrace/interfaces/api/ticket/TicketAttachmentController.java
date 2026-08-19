package com.ntn.fziot.mailtrace.interfaces.api.ticket;

import com.ntn.fziot.mailtrace.application.bizservice.attachment.TicketAttachmentService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.infrastructure.security.RequirePermission;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketAttachmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "工单附件")
@RestController
@RequestMapping("/v1/tickets/{ticketId}/attachments")
@RequiredArgsConstructor
public class TicketAttachmentController {

    private final TicketAttachmentService attachmentService;

    @Operation(summary = "上传附件")
    @PostMapping
    @RequirePermission(value = "ticket_attachment:upload", message = "无权上传工单附件")
    public BasicResult<TicketAttachmentVO> upload(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "messageId", required = false) Long messageId) throws IOException {
        return BasicResult.ok(attachmentService.upload(ticketId, messageId, file, principal));
    }

    @Operation(summary = "附件列表")
    @GetMapping
    @RequirePermission(value = "ticket_attachment:read", message = "无权查看工单附件")
    public BasicResult<List<TicketAttachmentVO>> list(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long ticketId) {
        return BasicResult.ok(attachmentService.listByTicketId(ticketId, principal));
    }

    @Operation(summary = "删除附件")
    @DeleteMapping("/{attachmentId}")
    @RequirePermission(value = "ticket_attachment:delete", message = "无权删除工单附件")
    public BasicResult<Void> delete(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId) {
        attachmentService.delete(ticketId, attachmentId, principal);
        return BasicResult.ok(null);
    }

    @Operation(summary = "下载附件")
    @GetMapping("/{attachmentId}/download")
    @RequirePermission(value = "ticket:read", message = "无权查看工单")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId) {
        var download = attachmentService.download(ticketId, attachmentId, principal);
        var vo = download.attachment();
        MediaType mediaType = vo.contentType() != null && !vo.contentType().isBlank()
                ? MediaType.parseMediaType(vo.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        String disposition = Boolean.TRUE.equals(vo.isInline()) ? "inline" : "attachment";
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("Content-Disposition", disposition + "; filename=\"" + safeFileName(vo.fileName()) + "\"")
                .body(new InputStreamResource(download.inputStream()));
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "attachment";
        }
        return fileName.replaceAll("[\\r\\n\"]", "_");
    }
}
