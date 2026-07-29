package com.ntn.fziot.mailtrace.interfaces.api.mailsend;

import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.infrastructure.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "邮件发送", description = "SMTP 邮件发送与测试")
@RestController
@RequestMapping("/v1/mail-send")
@RequiredArgsConstructor
public class MailSendController {

    private final MailSendService mailSendService;

    @Operation(summary = "发送测试邮件")
    @PostMapping("/test")
    @RequirePermission(value = "mail_send:test", message = "无权发送测试邮件")
    public BasicResult<MailSendService.SendResult> sendTestMail(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam Long mailboxId,
            @RequestParam String toAddress) {
        MailSendService.SendResult result = mailSendService.sendTestMail(principal, mailboxId, toAddress);
        if (result.success()) {
            return BasicResult.ok(result);
        }
        return BasicResult.fail(50001, result.message());
    }

    @Operation(summary = "重试发送失败邮件")
    @PostMapping("/retry")
    @RequirePermission(value = "mail_send:retry", message = "无权重试发送邮件")
    public BasicResult<MailSendService.SendResult> retrySend(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam Long id) {
        MailSendService.SendResult result = mailSendService.retrySend(principal, id);
        if (result.success()) {
            return BasicResult.ok(result);
        }
        return BasicResult.fail(50001, result.message());
    }
}
