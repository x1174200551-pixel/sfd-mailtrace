package com.ntn.fziot.mailtrace.interfaces.api.holiday;

import com.ntn.fziot.mailtrace.application.bizservice.holiday.HolidayService;
import com.ntn.fziot.mailtrace.application.bizservice.holiday.NationalHolidayPresetService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.infrastructure.security.RequirePermission;
import com.ntn.fziot.mailtrace.interfaces.vo.holiday.HolidayListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.holiday.HolidaySaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.holiday.HolidayVO;
import com.ntn.fziot.mailtrace.interfaces.vo.holiday.NationalHolidayPresetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "节假日", description = "按工作日历维护节假日")
@RestController
@RequestMapping("/v1/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;
    private final NationalHolidayPresetService nationalHolidayPresetService;

    @Operation(summary = "节假日列表")
    @GetMapping
    @RequirePermission(value = "holiday:read", message = "无权查看节假日")
    public BasicResult<HolidayListResponse> listHolidays(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) Long calendarId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return BasicResult.ok(holidayService.listHolidays(principal, calendarId, keyword, dateFrom, dateTo));
    }

    @Operation(summary = "国家法定节假日模板")
    @GetMapping("/national-presets")
    @RequirePermission(value = "holiday:import", message = "无权导入法定节假日模板")
    public BasicResult<NationalHolidayPresetResponse> nationalPresets(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam Integer year) {
        return BasicResult.ok(nationalHolidayPresetService.getPreset(principal, year));
    }

    @Operation(summary = "新建节假日")
    @PostMapping
    @RequirePermission(value = "holiday:create", message = "无权新建节假日")
    public BasicResult<HolidayVO> createHoliday(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody HolidaySaveRequest request) {
        return BasicResult.ok(holidayService.createHoliday(principal, request));
    }

    @Operation(summary = "编辑节假日")
    @PutMapping("/{id}")
    @RequirePermission(value = "holiday:update", message = "无权编辑节假日")
    public BasicResult<HolidayVO> updateHoliday(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody HolidaySaveRequest request) {
        return BasicResult.ok(holidayService.updateHoliday(principal, id, request));
    }

    @Operation(summary = "删除节假日")
    @DeleteMapping("/{id}")
    @RequirePermission(value = "holiday:delete", message = "无权删除节假日")
    public BasicResult<Void> deleteHoliday(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id) {
        holidayService.deleteHoliday(principal, id);
        return BasicResult.ok();
    }
}
