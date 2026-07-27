package com.ntn.fziot.mailtrace.interfaces.api.assignment;

import com.ntn.fziot.mailtrace.application.bizservice.assignment.AssignmentRuleService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleMatchResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleSortRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleTestRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleVO;
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

@Tag(name = "分配规则", description = "分配规则列表、新建、编辑、启停、排序和删除")
@RestController
@RequestMapping("/v1/assignment-rules")
@RequiredArgsConstructor
public class AssignmentRuleController {

    private final AssignmentRuleService assignmentRuleService;

    @Operation(summary = "分配规则列表")
    @GetMapping
    public BasicResult<AssignmentRuleListResponse> listRules(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String matchType) {
        return BasicResult.ok(assignmentRuleService.listRules(principal, keyword, enabled, matchType));
    }

    @Operation(summary = "新建分配规则")
    @PostMapping
    public BasicResult<AssignmentRuleVO> createRule(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody AssignmentRuleSaveRequest request) {
        return BasicResult.ok(assignmentRuleService.createRule(principal, request));
    }

    @Operation(summary = "编辑分配规则")
    @PutMapping("/{id}")
    public BasicResult<AssignmentRuleVO> updateRule(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRuleSaveRequest request) {
        return BasicResult.ok(assignmentRuleService.updateRule(principal, id, request));
    }

    @Operation(summary = "启用或停用分配规则")
    @PatchMapping("/{id}/enabled")
    public BasicResult<AssignmentRuleVO> updateEnabled(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRuleEnabledRequest request) {
        return BasicResult.ok(assignmentRuleService.updateEnabled(principal, id, request));
    }

    @Operation(summary = "调整分配规则排序")
    @PutMapping("/sort")
    public BasicResult<List<AssignmentRuleVO>> sortRules(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody AssignmentRuleSortRequest request) {
        return BasicResult.ok(assignmentRuleService.sortRules(principal, request));
    }

    @Operation(summary = "测试分配规则匹配")
    @PostMapping("/test-match")
    public BasicResult<AssignmentRuleMatchResponse> testMatch(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody AssignmentRuleTestRequest request) {
        return BasicResult.ok(assignmentRuleService.testMatch(principal, request));
    }

    @Operation(summary = "删除分配规则")
    @DeleteMapping("/{id}")
    public BasicResult<Void> deleteRule(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id) {
        assignmentRuleService.deleteRule(principal, id);
        return BasicResult.ok();
    }
}
