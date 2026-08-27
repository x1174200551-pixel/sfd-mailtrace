package com.ntn.fziot.mailtrace.interfaces.api.enterprise;

import com.ntn.fziot.mailtrace.application.bizservice.enterprise.EnterpriseService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.infrastructure.security.RequirePermission;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseOptionVO;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

@Tag(name = "企业管理", description = "企业业务归属配置、启停和可见选项")
@RestController
@RequestMapping("/v1/enterprises")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    @Operation(summary = "企业配置列表")
    @GetMapping
    @RequirePermission(value = "enterprise:read", message = "无权查看企业配置")
    public BasicResult<EnterpriseListResponse> listEnterprises(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return BasicResult.ok(enterpriseService.listEnterprises(principal, keyword, enabled, page, size));
    }

    @Operation(summary = "当前用户可见企业选项")
    @GetMapping("/options")
    public BasicResult<List<EnterpriseOptionVO>> listVisibleOptions(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) Boolean enabled) {
        return BasicResult.ok(enterpriseService.listVisibleOptions(principal, enabled));
    }

    @Operation(summary = "企业详情")
    @GetMapping("/{id}")
    @RequirePermission(value = "enterprise:read", message = "无权查看企业配置")
    public BasicResult<EnterpriseVO> getEnterprise(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id) {
        return BasicResult.ok(enterpriseService.getEnterprise(principal, id));
    }

    @Operation(summary = "新建企业")
    @PostMapping
    @RequirePermission(value = "enterprise:create", message = "无权新建企业配置")
    public BasicResult<EnterpriseVO> createEnterprise(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody EnterpriseSaveRequest request) {
        return BasicResult.ok(enterpriseService.createEnterprise(principal, request));
    }

    @Operation(summary = "编辑企业")
    @PutMapping("/{id}")
    @RequirePermission(value = "enterprise:update", message = "无权编辑企业配置")
    public BasicResult<EnterpriseVO> updateEnterprise(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody EnterpriseSaveRequest request) {
        return BasicResult.ok(enterpriseService.updateEnterprise(principal, id, request));
    }

    @Operation(summary = "启用或停用企业")
    @PatchMapping("/{id}/enabled")
    @RequirePermission(value = "enterprise:enable", message = "无权启停企业配置")
    public BasicResult<EnterpriseVO> updateEnabled(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody EnterpriseEnabledRequest request) {
        return BasicResult.ok(enterpriseService.updateEnabled(principal, id, request));
    }
}
