package com.ntn.fziot.mailtrace.interfaces.api.department;

import com.ntn.fziot.mailtrace.application.bizservice.department.DepartmentService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.infrastructure.security.RequirePermission;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentCreateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentLeaderRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentMemberAddRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentMemberPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentMoveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentStatsVO;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentUpdateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentVO;
import com.ntn.fziot.mailtrace.interfaces.vo.user.UserVO;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "组织管理", description = "部门树、部门详情、新建、编辑和启停")
@RestController
@RequestMapping("/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "部门树")
    @GetMapping
    @RequirePermission(value = "department:read", message = "无权查看组织管理")
    public BasicResult<List<DepartmentVO>> listTree(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) Boolean enabled) {
        return BasicResult.ok(departmentService.listTree(principal, enabled));
    }

    @Operation(summary = "组织架构统计")
    @GetMapping("/stats")
    @RequirePermission(value = "department:read", message = "无权查看组织管理")
    public BasicResult<DepartmentStatsVO> stats(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return BasicResult.ok(departmentService.stats(principal));
    }

    @Operation(summary = "部门详情")
    @GetMapping("/{id}")
    @RequirePermission(value = "department:read", message = "无权查看组织管理")
    public BasicResult<DepartmentVO> getDepartment(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id) {
        return BasicResult.ok(departmentService.getDepartment(principal, id));
    }

    @Operation(summary = "新建部门")
    @PostMapping
    @RequirePermission(value = "department:create", message = "无权新建部门")
    public BasicResult<DepartmentVO> createDepartment(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody DepartmentCreateRequest request) {
        return BasicResult.ok(departmentService.createDepartment(principal, request));
    }

    @Operation(summary = "编辑部门")
    @PutMapping("/{id}")
    @RequirePermission(value = "department:update", message = "无权编辑部门")
    public BasicResult<DepartmentVO> updateDepartment(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody DepartmentUpdateRequest request) {
        return BasicResult.ok(departmentService.updateDepartment(principal, id, request));
    }

    @Operation(summary = "移动部门")
    @PatchMapping("/{id}/parent")
    @RequirePermission(value = "department:update", message = "无权移动部门")
    public BasicResult<DepartmentVO> moveDepartment(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody DepartmentMoveRequest request) {
        return BasicResult.ok(departmentService.moveDepartment(principal, id, request));
    }

    @Operation(summary = "设置部门负责人")
    @PatchMapping("/{id}/leader")
    @RequirePermission(value = "department:update", message = "无权设置部门负责人")
    public BasicResult<DepartmentVO> updateLeader(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody DepartmentLeaderRequest request) {
        return BasicResult.ok(departmentService.updateLeader(principal, id, request));
    }

    @Operation(summary = "启用或停用部门")
    @PatchMapping("/{id}/enabled")
    @RequirePermission(value = "department:enable", message = "无权启停部门")
    public BasicResult<DepartmentVO> updateEnabled(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody DepartmentEnabledRequest request) {
        return BasicResult.ok(departmentService.updateEnabled(principal, id, request));
    }

    @Operation(summary = "部门成员分页查询")
    @GetMapping("/{id}/members")
    @RequirePermission(value = "department:read", message = "无权查看组织管理")
    public BasicResult<DepartmentMemberPageResponse> pageMembers(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return BasicResult.ok(departmentService.pageMembers(principal, id, keyword, roleCode, page, size));
    }

    @Operation(summary = "可添加部门成员候选列表")
    @GetMapping("/{id}/member-candidates")
    @RequirePermission(value = "department:read", message = "无权查看组织管理")
    public BasicResult<DepartmentMemberPageResponse> pageMemberCandidates(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return BasicResult.ok(departmentService.pageMemberCandidates(principal, id, keyword, page, size));
    }

    @Operation(summary = "添加部门成员")
    @PostMapping("/{id}/members")
    @RequirePermission(value = "department:update", message = "无权添加部门成员")
    public BasicResult<List<UserVO>> addMembers(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody DepartmentMemberAddRequest request) {
        return BasicResult.ok(departmentService.addMembers(principal, id, request));
    }

    @Operation(summary = "移出部门成员")
    @DeleteMapping("/{id}/members/{userId}")
    @RequirePermission(value = "department:update", message = "无权移出部门成员")
    public BasicResult<Void> removeMember(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long userId) {
        departmentService.removeMember(principal, id, userId);
        return BasicResult.ok();
    }
}
