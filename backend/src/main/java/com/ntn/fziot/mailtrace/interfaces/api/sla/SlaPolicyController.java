package com.ntn.fziot.mailtrace.interfaces.api.sla;

import com.ntn.fziot.mailtrace.application.bizservice.sla.SlaPolicyService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.infrastructure.security.RequirePermission;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicyDefaultRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicyEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicyListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicySaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicyVO;
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

@Tag(name = "SLA 策略", description = "SLA 策略列表、新建、编辑、启停、默认和删除")
@RestController
@RequestMapping("/v1/sla-policies")
@RequiredArgsConstructor
public class SlaPolicyController {

    private final SlaPolicyService slaPolicyService;

    @Operation(summary = "SLA 策略列表")
    @GetMapping
    @RequirePermission(value = "sla_policy:read", message = "无权查看 SLA 策略")
    public BasicResult<SlaPolicyListResponse> listPolicies(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean defaultPolicy) {
        return BasicResult.ok(slaPolicyService.listPolicies(
                principal, enterpriseId, keyword, enabled, defaultPolicy));
    }

    @Operation(summary = "新建 SLA 策略")
    @PostMapping
    @RequirePermission(value = "sla_policy:create", message = "无权新建 SLA 策略")
    public BasicResult<SlaPolicyVO> createPolicy(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody SlaPolicySaveRequest request) {
        return BasicResult.ok(slaPolicyService.createPolicy(principal, request));
    }

    @Operation(summary = "编辑 SLA 策略")
    @PutMapping("/{id}")
    @RequirePermission(value = "sla_policy:update", message = "无权编辑 SLA 策略")
    public BasicResult<SlaPolicyVO> updatePolicy(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody SlaPolicySaveRequest request) {
        return BasicResult.ok(slaPolicyService.updatePolicy(principal, id, request));
    }

    @Operation(summary = "启用或停用 SLA 策略")
    @PatchMapping("/{id}/enabled")
    @RequirePermission(value = "sla_policy:enable", message = "无权启停 SLA 策略")
    public BasicResult<SlaPolicyVO> updateEnabled(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody SlaPolicyEnabledRequest request) {
        return BasicResult.ok(slaPolicyService.updateEnabled(principal, id, request));
    }

    @Operation(summary = "设置默认 SLA 策略")
    @PatchMapping("/{id}/default")
    @RequirePermission(value = "sla_policy:default", message = "无权设置默认 SLA 策略")
    public BasicResult<SlaPolicyVO> updateDefault(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody SlaPolicyDefaultRequest request) {
        return BasicResult.ok(slaPolicyService.updateDefault(principal, id, request));
    }

    @Operation(summary = "删除 SLA 策略")
    @DeleteMapping("/{id}")
    @RequirePermission(value = "sla_policy:delete", message = "无权删除 SLA 策略")
    public BasicResult<Void> deletePolicy(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id) {
        slaPolicyService.deletePolicy(principal, id);
        return BasicResult.ok();
    }
}
