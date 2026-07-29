package com.ntn.fziot.mailtrace.interfaces.api.department;

import com.ntn.fziot.mailtrace.application.bizservice.department.DepartmentService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentCreateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentUpdateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.department.DepartmentVO;
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

@Tag(name = "组织管理", description = "部门树、部门详情、新建、编辑和启停")
@RestController
@RequestMapping("/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "部门树")
    @GetMapping
    public BasicResult<List<DepartmentVO>> listTree(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) Boolean enabled) {
        return BasicResult.ok(departmentService.listTree(principal, enabled));
    }

    @Operation(summary = "部门详情")
    @GetMapping("/{id}")
    public BasicResult<DepartmentVO> getDepartment(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id) {
        return BasicResult.ok(departmentService.getDepartment(principal, id));
    }

    @Operation(summary = "新建部门")
    @PostMapping
    public BasicResult<DepartmentVO> createDepartment(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody DepartmentCreateRequest request) {
        return BasicResult.ok(departmentService.createDepartment(principal, request));
    }

    @Operation(summary = "编辑部门")
    @PutMapping("/{id}")
    public BasicResult<DepartmentVO> updateDepartment(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody DepartmentUpdateRequest request) {
        return BasicResult.ok(departmentService.updateDepartment(principal, id, request));
    }

    @Operation(summary = "启用或停用部门")
    @PatchMapping("/{id}/enabled")
    public BasicResult<DepartmentVO> updateEnabled(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody DepartmentEnabledRequest request) {
        return BasicResult.ok(departmentService.updateEnabled(principal, id, request));
    }
}
