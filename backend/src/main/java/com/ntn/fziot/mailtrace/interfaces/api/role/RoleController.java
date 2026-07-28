package com.ntn.fziot.mailtrace.interfaces.api.role;

import com.ntn.fziot.mailtrace.application.bizservice.role.RoleManagementService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.role.PermissionTreeNodeVO;
import com.ntn.fziot.mailtrace.interfaces.vo.role.RoleEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.role.RoleListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.role.RolePermissionSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.role.RoleSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.role.RoleVO;
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

@Tag(name = "角色管理", description = "角色列表、新建、编辑、启停、权限配置和默认数据范围")
@RestController
@RequestMapping("/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleManagementService roleManagementService;

    @Operation(summary = "角色列表")
    @GetMapping
    public BasicResult<RoleListResponse> listRoles(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return BasicResult.ok(roleManagementService.listRoles(principal, keyword, enabled));
    }

    @Operation(summary = "权限树")
    @GetMapping("/permissions")
    public BasicResult<List<PermissionTreeNodeVO>> listPermissionTree(
            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return BasicResult.ok(roleManagementService.listPermissionTree(principal));
    }

    @Operation(summary = "新建角色")
    @PostMapping
    public BasicResult<RoleVO> createRole(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody RoleSaveRequest request) {
        return BasicResult.ok(roleManagementService.createRole(principal, request));
    }

    @Operation(summary = "编辑角色")
    @PutMapping("/{id}")
    public BasicResult<RoleVO> updateRole(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody RoleSaveRequest request) {
        return BasicResult.ok(roleManagementService.updateRole(principal, id, request));
    }

    @Operation(summary = "启用或停用角色")
    @PatchMapping("/{id}/enabled")
    public BasicResult<RoleVO> updateEnabled(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody RoleEnabledRequest request) {
        return BasicResult.ok(roleManagementService.updateEnabled(principal, id, request));
    }

    @Operation(summary = "保存角色权限和默认数据范围")
    @PutMapping("/{id}/permissions")
    public BasicResult<RoleVO> saveRolePermissions(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody RolePermissionSaveRequest request) {
        return BasicResult.ok(roleManagementService.saveRolePermissions(principal, id, request));
    }
}
