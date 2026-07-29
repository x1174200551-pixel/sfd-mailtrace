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
import java.io.InputStream;
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
    @RequirePermission(value = "ticket_attachment:download", message = "无权下载工单附件")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId) {
        // Simple proxy - in production use presigned URL directly
        var attachments = attachmentService.listByTicketId(ticketId, principal);
        var opt = attachments.stream().filter(a -> a.id().equals(attachmentId)).findFirst();
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var vo = opt.get();
        InputStream inputStream = attachmentService.downloadRaw(ticketId, attachmentId, principal);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + vo.fileName() + "\"")
                .body(new InputStreamResource(inputStream));
    }
}
