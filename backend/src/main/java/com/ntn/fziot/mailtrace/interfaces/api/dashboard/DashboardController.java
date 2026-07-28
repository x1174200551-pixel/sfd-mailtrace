package com.ntn.fziot.mailtrace.interfaces.api.dashboard;

import com.ntn.fziot.mailtrace.application.bizservice.dashboard.DashboardService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.dashboard.DashboardSummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.dashboard.DashboardTodoListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "工作台", description = "工作台统计与待办")
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "工作台统计摘要")
    @GetMapping("/summary")
    public BasicResult<DashboardSummaryVO> summary(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return BasicResult.ok(dashboardService.summary(principal));
    }

    @Operation(summary = "我的待办")
    @GetMapping("/my-todos")
    public BasicResult<DashboardTodoListResponse> myTodos(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) Integer limit) {
        return BasicResult.ok(dashboardService.myTodos(principal, limit));
    }
}
