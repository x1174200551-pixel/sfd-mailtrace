package com.ntn.fziot.mailtrace.interfaces.api.mailsend;

import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendLogBizService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.log.MailSendLogPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "邮件发送", description = "SMTP 邮件发送与日志查询")
@RestController
@RequestMapping("/v1/mail-send")
@RequiredArgsConstructor
public class MailSendLogController {

    private final MailSendLogBizService mailSendLogBizService;

    @Operation(summary = "发送日志统计概览")
    @GetMapping("/logs/stats")
    public BasicResult<MailSendLogBizService.SendLogStats> stats() {
        return BasicResult.ok(mailSendLogBizService.stats());
    }

    @Operation(summary = "待处理数量统计（菜单角标）")
    @GetMapping("/logs/pending-count")
    public BasicResult<Long> pendingCount() {
        return BasicResult.ok(mailSendLogBizService.pendingCount());
    }

    @Operation(summary = "发送日志分页查询")
    @GetMapping("/logs")
    public BasicResult<MailSendLogPageResponse> pageSendLogs(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) Long mailboxId,
            @RequestParam(required = false) String sendType,
            @RequestParam(required = false) String sendStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return BasicResult.ok(mailSendLogBizService.pageSendLogs(
                principal, mailboxId, sendType, sendStatus, startFrom, startTo, page, size));
    }
}
