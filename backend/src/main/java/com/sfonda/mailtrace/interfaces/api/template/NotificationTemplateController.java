package com.sfonda.mailtrace.interfaces.api.template;

import com.sfonda.mailtrace.application.bizservice.template.NotificationTemplateService;
import com.sfonda.mailtrace.infrastructure.basic.BasicResult;
import com.sfonda.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.sfonda.mailtrace.interfaces.vo.template.NotificationTemplateCreateRequest;
import com.sfonda.mailtrace.interfaces.vo.template.NotificationTemplateListResponse;
import com.sfonda.mailtrace.interfaces.vo.template.NotificationTemplateUpdateRequest;
import com.sfonda.mailtrace.interfaces.vo.template.NotificationTemplateVO;
import com.sfonda.mailtrace.interfaces.vo.template.TemplatePreviewRequest;
import com.sfonda.mailtrace.interfaces.vo.template.TemplatePreviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "通知模板", description = "通知模板列表、保存与预览")
@RestController
@RequestMapping("/v1/notification-templates")
@RequiredArgsConstructor
public class NotificationTemplateController {

    private final NotificationTemplateService notificationTemplateService;

    @Operation(summary = "通知模板列表")
    @GetMapping
    public BasicResult<NotificationTemplateListResponse> listTemplates(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return BasicResult.ok(notificationTemplateService.listTemplates(principal, keyword, enabled));
    }

    @Operation(summary = "新建通知模板")
    @PostMapping
    public BasicResult<NotificationTemplateVO> createTemplate(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody NotificationTemplateCreateRequest request) {
        return BasicResult.ok(notificationTemplateService.createTemplate(principal, request));
    }

    @Operation(summary = "保存通知模板")
    @PutMapping("/{id}")
    public BasicResult<NotificationTemplateVO> updateTemplate(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody NotificationTemplateUpdateRequest request) {
        return BasicResult.ok(notificationTemplateService.updateTemplate(principal, id, request));
    }

    @Operation(summary = "通知模板预览")
    @PostMapping("/preview")
    public BasicResult<TemplatePreviewResponse> preview(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody TemplatePreviewRequest request) {
        return BasicResult.ok(notificationTemplateService.preview(principal, request));
    }
}
