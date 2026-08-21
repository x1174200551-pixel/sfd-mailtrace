package com.ntn.fziot.mailtrace.interfaces.api.ticket;

import com.ntn.fziot.mailtrace.application.bizservice.ticket.CustomerTicketAccessService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.CustomerTicketDetailVO;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.CustomerTicketVerifyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "客户工单查询", description = "客户通过邮件链接和校验码查看工单进度")
@RestController
@RequestMapping("/v1/customer-tickets")
@RequiredArgsConstructor
public class CustomerTicketController {

    private final CustomerTicketAccessService customerTicketAccessService;

    @Operation(summary = "校验客户访问码并返回客户可见工单详情")
    @PostMapping("/{ticketNo}/verify")
    public BasicResult<CustomerTicketDetailVO> verifyAndGetDetail(
            @PathVariable String ticketNo,
            @Valid @RequestBody CustomerTicketVerifyRequest request,
            HttpServletRequest httpRequest) {
        return BasicResult.ok(customerTicketAccessService.verifyAndGetDetail(
                ticketNo, request.accessCode(), clientIp(httpRequest)));
    }

    @Operation(summary = "客户访问内联附件")
    @GetMapping("/{ticketNo}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadInlineAttachment(
            @PathVariable String ticketNo,
            @PathVariable Long attachmentId,
            @RequestParam String token,
            HttpServletRequest request) {
        var download = customerTicketAccessService.downloadInlineAttachment(
                ticketNo, attachmentId, token, clientIp(request));
        var vo = download.attachment();
        MediaType mediaType = vo.contentType() != null && !vo.contentType().isBlank()
                ? MediaType.parseMediaType(vo.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("Content-Disposition", "inline; filename=\"" + safeFileName(vo.fileName()) + "\"")
                .body(new InputStreamResource(download.inputStream()));
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "attachment";
        }
        return fileName.replaceAll("[\\r\\n\"]", "_");
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
