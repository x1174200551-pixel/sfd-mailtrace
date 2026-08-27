package com.ntn.fziot.mailtrace.interfaces.api.assignment;

import com.ntn.fziot.mailtrace.application.bizservice.assignment.AssignmentRuleGroupService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.infrastructure.security.RequirePermission;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleGroupListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleGroupSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleGroupVO;
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

@Tag(name = "分配规则组", description = "企业分配规则组配置、启停和删除")
@RestController
@RequestMapping("/v1/assignment-rule-groups")
@RequiredArgsConstructor
public class AssignmentRuleGroupController {

    private final AssignmentRuleGroupService groupService;

    @Operation(summary = "分配规则组列表")
    @GetMapping
    @RequirePermission(value = "assignment_rule_group:read", message = "无权查看分配规则组")
    public BasicResult<AssignmentRuleGroupListResponse> listGroups(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return BasicResult.ok(groupService.listGroups(principal, enterpriseId, keyword, enabled));
    }

    @Operation(summary = "分配规则组选项")
    @GetMapping("/options")
    @RequirePermission(value = "assignment_rule_group:read", message = "无权查看分配规则组")
    public BasicResult<List<AssignmentRuleGroupVO>> listOptions(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) Boolean enabled) {
        return BasicResult.ok(groupService.listOptions(principal, enterpriseId, enabled));
    }

    @Operation(summary = "新建分配规则组")
    @PostMapping
    @RequirePermission(value = "assignment_rule_group:create", message = "无权新建分配规则组")
    public BasicResult<AssignmentRuleGroupVO> createGroup(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody AssignmentRuleGroupSaveRequest request) {
        return BasicResult.ok(groupService.createGroup(principal, request));
    }

    @Operation(summary = "编辑分配规则组")
    @PutMapping("/{id}")
    @RequirePermission(value = "assignment_rule_group:update", message = "无权编辑分配规则组")
    public BasicResult<AssignmentRuleGroupVO> updateGroup(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRuleGroupSaveRequest request) {
        return BasicResult.ok(groupService.updateGroup(principal, id, request));
    }

    @Operation(summary = "启用或停用分配规则组")
    @PatchMapping("/{id}/enabled")
    @RequirePermission(value = "assignment_rule_group:enable", message = "无权启停分配规则组")
    public BasicResult<AssignmentRuleGroupVO> updateEnabled(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRuleEnabledRequest request) {
        return BasicResult.ok(groupService.updateEnabled(principal, id, request));
    }

    @Operation(summary = "删除分配规则组")
    @DeleteMapping("/{id}")
    @RequirePermission(value = "assignment_rule_group:delete", message = "无权删除分配规则组")
    public BasicResult<Void> deleteGroup(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id) {
        groupService.deleteGroup(principal, id);
        return BasicResult.ok();
    }
}
