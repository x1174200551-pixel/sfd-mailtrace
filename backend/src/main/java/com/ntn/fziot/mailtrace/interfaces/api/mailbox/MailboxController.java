package com.ntn.fziot.mailtrace.interfaces.api.mailbox;

import com.ntn.fziot.mailtrace.application.bizservice.mailbox.MailboxService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.infrastructure.security.RequirePermission;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxConnectionTestRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxConnectionTestResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxOptionVO;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "邮箱配置", description = "企业客服邮箱 IMAP/SMTP 配置、启停和连接测试")
@RestController
@RequestMapping("/v1/mailboxes")
@RequiredArgsConstructor
public class MailboxController {

    private final MailboxService mailboxService;

    @Operation(summary = "邮箱分页查询")
    @GetMapping
    @RequirePermission(value = "mailbox:read", message = "无权查看邮箱配置")
    public BasicResult<MailboxPageResponse> pageMailboxes(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return BasicResult.ok(mailboxService.pageMailboxes(
                principal, enterpriseId, keyword, status, enabled, page, size));
    }

    @Operation(summary = "当前用户可见邮箱选项")
    @GetMapping("/options")
    public BasicResult<List<MailboxOptionVO>> listVisibleOptions(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) Boolean operationalOnly) {
        return BasicResult.ok(mailboxService.listVisibleOptions(principal, enterpriseId, operationalOnly));
    }

    @Operation(summary = "新建邮箱配置")
    @PostMapping
    @RequirePermission(value = "mailbox:create", message = "无权新建邮箱配置")
    public BasicResult<MailboxVO> createMailbox(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody MailboxSaveRequest request) {
        return BasicResult.ok(mailboxService.createMailbox(principal, request));
    }

    @Operation(summary = "编辑邮箱配置")
    @PutMapping("/{id}")
    @RequirePermission(value = "mailbox:update", message = "无权编辑邮箱配置")
    public BasicResult<MailboxVO> updateMailbox(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody MailboxSaveRequest request) {
        return BasicResult.ok(mailboxService.updateMailbox(principal, id, request));
    }

    @Operation(summary = "启用或停用邮箱配置")
    @PatchMapping("/{id}/enabled")
    @RequirePermission(value = "mailbox:enable", message = "无权启停邮箱配置")
    public BasicResult<MailboxVO> updateEnabled(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody MailboxEnabledRequest request) {
        return BasicResult.ok(mailboxService.updateEnabled(principal, id, request));
    }

    @Operation(summary = "删除邮箱配置")
    @DeleteMapping("/{id}")
    @RequirePermission(value = "mailbox:delete", message = "无权删除邮箱配置")
    public BasicResult<Void> deleteMailbox(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id) {
        mailboxService.deleteMailbox(principal, id);
        return BasicResult.ok();
    }

    @Operation(summary = "测试已保存邮箱连接")
    @PostMapping("/{id}/test-connection")
    @RequirePermission(value = "mailbox:test_connection", message = "无权测试邮箱连接")
    public BasicResult<MailboxConnectionTestResponse> testSavedMailbox(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) String testType) {
        return BasicResult.ok(mailboxService.testSavedMailbox(principal, id, testType));
    }

    @Operation(summary = "测试邮箱草稿连接")
    @PostMapping("/test-connection")
    @RequirePermission(value = "mailbox:test_connection", message = "无权测试邮箱连接")
    public BasicResult<MailboxConnectionTestResponse> testDraftMailbox(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody MailboxConnectionTestRequest request) {
        return BasicResult.ok(mailboxService.testDraftMailbox(principal, request));
    }
}
