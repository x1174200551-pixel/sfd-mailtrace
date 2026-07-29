package com.ntn.fziot.mailtrace.interfaces.api.log;

import com.ntn.fziot.mailtrace.application.bizservice.mailfetch.MailFetchLogBizService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.infrastructure.security.RequirePermission;
import com.ntn.fziot.mailtrace.interfaces.vo.log.MailFetchLogPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.log.MailFetchLogStatsVO;
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

@Tag(name = "拉取日志", description = "邮件拉取任务日志查询")
@RestController
@RequestMapping("/v1/mail-fetch-logs")
@RequiredArgsConstructor
public class MailFetchLogController {

    private final MailFetchLogBizService mailFetchLogBizService;

    @Operation(summary = "拉取日志分页查询")
    @GetMapping
    @RequirePermission(value = "mail_fetch_log:read", message = "无权访问拉取日志")
    public BasicResult<MailFetchLogPageResponse> pageFetchLogs(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) Long mailboxId,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTo,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return BasicResult.ok(mailFetchLogBizService.pageFetchLogs(
                principal, mailboxId, success, startFrom, startTo, keyword, page, size));
    }

    @Operation(summary = "拉取日志统计概览")
    @GetMapping("/stats")
    @RequirePermission(value = "mail_fetch_log:read", message = "无权访问拉取日志")
    public BasicResult<MailFetchLogStatsVO> stats() {
        return BasicResult.ok(mailFetchLogBizService.stats());
    }
}
