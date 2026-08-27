package com.ntn.fziot.mailtrace.interfaces.api.calendar;

import com.ntn.fziot.mailtrace.application.bizservice.calendar.WorkCalendarService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.infrastructure.security.RequirePermission;
import com.ntn.fziot.mailtrace.interfaces.vo.calendar.WorkCalendarDefaultRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.calendar.WorkCalendarListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.calendar.WorkCalendarSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.calendar.WorkCalendarVO;
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

@Tag(name = "工作日历", description = "工作日历列表、新建、编辑、默认和删除")
@RestController
@RequestMapping("/v1/work-calendars")
@RequiredArgsConstructor
public class WorkCalendarController {

    private final WorkCalendarService workCalendarService;

    @Operation(summary = "工作日历列表")
    @GetMapping
    @RequirePermission(value = "work_calendar:read", message = "无权查看工作日历")
    public BasicResult<WorkCalendarListResponse> listCalendars(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean defaultCalendar) {
        return BasicResult.ok(workCalendarService.listCalendars(
                principal, enterpriseId, keyword, defaultCalendar));
    }

    @Operation(summary = "新建工作日历")
    @PostMapping
    @RequirePermission(value = "work_calendar:create", message = "无权新建工作日历")
    public BasicResult<WorkCalendarVO> createCalendar(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody WorkCalendarSaveRequest request) {
        return BasicResult.ok(workCalendarService.createCalendar(principal, request));
    }

    @Operation(summary = "编辑工作日历")
    @PutMapping("/{id}")
    @RequirePermission(value = "work_calendar:update", message = "无权编辑工作日历")
    public BasicResult<WorkCalendarVO> updateCalendar(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody WorkCalendarSaveRequest request) {
        return BasicResult.ok(workCalendarService.updateCalendar(principal, id, request));
    }

    @Operation(summary = "设置默认工作日历")
    @PatchMapping("/{id}/default")
    @RequirePermission(value = "work_calendar:default", message = "无权设置默认工作日历")
    public BasicResult<WorkCalendarVO> updateDefault(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody WorkCalendarDefaultRequest request) {
        return BasicResult.ok(workCalendarService.updateDefault(principal, id, request));
    }

    @Operation(summary = "删除工作日历")
    @DeleteMapping("/{id}")
    @RequirePermission(value = "work_calendar:delete", message = "无权删除工作日历")
    public BasicResult<Void> deleteCalendar(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id) {
        workCalendarService.deleteCalendar(principal, id);
        return BasicResult.ok();
    }
}
