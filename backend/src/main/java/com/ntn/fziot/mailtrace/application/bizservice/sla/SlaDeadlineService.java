package com.ntn.fziot.mailtrace.application.bizservice.sla;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.HolidayEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.SlaPolicyEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.WorkCalendarEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.HolidayMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.SlaPolicyMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.WorkCalendarMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaDeadlineService {

    private static final int MAX_CALCULATION_DAYS = 20000;

    private final SlaPolicyMapper slaPolicyMapper;
    private final WorkCalendarMapper workCalendarMapper;
    private final HolidayMapper holidayMapper;

    /**
     * 计算新建工单的 SLA 截止时间。
     */
    public SlaDeadlineResult calculateForNewTicket(LocalDateTime customerMailAt) {
        // 1、选择启用的默认 SLA 策略；若暂无默认策略，则使用最早创建的启用策略兜底。
        SlaPolicyEntity policy = selectEffectivePolicy();
        if (policy == null) {
            return SlaDeadlineResult.none();
        }

        // 2、读取策略绑定的工作日历；日历不存在时不阻断建单，只跳过本次 SLA 写入。
        WorkCalendarEntity calendar = workCalendarMapper.selectById(policy.getCalendarId());
        if (calendar == null) {
            log.warn("SLA 策略绑定的工作日历不存在，跳过截止时间计算 policyId={} calendarId={}",
                    policy.getId(), policy.getCalendarId());
            return SlaDeadlineResult.none();
        }

        // 3、加载该日历下配置的节假日，用于工作日计算时跳过。
        Set<LocalDate> holidays = holidayMapper.selectList(new LambdaQueryWrapper<HolidayEntity>()
                        .eq(HolidayEntity::getCalendarId, calendar.getId()))
                .stream()
                .map(HolidayEntity::getHolidayDate)
                .collect(Collectors.toSet());

        // 4、从客户来信时间起算；非工作时间会自动推进到下一工作时段起点。
        LocalDateTime startAt = customerMailAt != null ? customerMailAt : LocalDateTime.now();
        CalendarSchedule schedule = toSchedule(calendar, holidays);
        LocalDateTime responseDeadline = addWorkingHours(startAt, policy.getResponseHours(), schedule);
        LocalDateTime resolveDeadline = policy.getResolveHours() == null
                ? null
                : addWorkingHours(startAt, policy.getResolveHours(), schedule);

        // 5、返回策略 ID 和两个截止时间，供工单创建流程一次性落库。
        return new SlaDeadlineResult(policy.getId(), responseDeadline, resolveDeadline);
    }

    private SlaPolicyEntity selectEffectivePolicy() {
        return slaPolicyMapper.selectOne(new LambdaQueryWrapper<SlaPolicyEntity>()
                .eq(SlaPolicyEntity::getEnabled, true)
                .orderByDesc(SlaPolicyEntity::getDefaultPolicy)
                .orderByAsc(SlaPolicyEntity::getId)
                .last("LIMIT 1"));
    }

    private CalendarSchedule toSchedule(WorkCalendarEntity calendar, Set<LocalDate> holidays) {
        Set<Integer> workdays = Arrays.stream(calendar.getWorkdays().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Integer::valueOf)
                .collect(Collectors.toSet());
        return new CalendarSchedule(workdays, calendar.getWorkStartTime(), calendar.getWorkEndTime(), holidays);
    }

    private LocalDateTime addWorkingHours(LocalDateTime startAt, int hours, CalendarSchedule schedule) {
        long remainingMinutes = Duration.ofHours(hours).toMinutes();
        LocalDateTime cursor = normalizeToWorkingTime(startAt, schedule);

        for (int i = 0; i < MAX_CALCULATION_DAYS; i++) {
            cursor = normalizeToWorkingTime(cursor, schedule);
            LocalDateTime dayEnd = LocalDateTime.of(cursor.toLocalDate(), schedule.workEndTime());
            long availableMinutes = Duration.between(cursor, dayEnd).toMinutes();
            if (remainingMinutes <= availableMinutes) {
                return cursor.plusMinutes(remainingMinutes);
            }
            remainingMinutes -= availableMinutes;
            cursor = LocalDateTime.of(cursor.toLocalDate().plusDays(1), schedule.workStartTime());
        }

        throw new IllegalStateException("SLA 截止时间计算超过最大跨度，请检查工作日历配置");
    }

    private LocalDateTime normalizeToWorkingTime(LocalDateTime dateTime, CalendarSchedule schedule) {
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();

        for (int i = 0; i < MAX_CALCULATION_DAYS; i++) {
            if (!isWorkingDate(date, schedule)) {
                date = date.plusDays(1);
                time = schedule.workStartTime();
                continue;
            }
            if (time.isBefore(schedule.workStartTime())) {
                return LocalDateTime.of(date, schedule.workStartTime());
            }
            if (!time.isBefore(schedule.workEndTime())) {
                date = date.plusDays(1);
                time = schedule.workStartTime();
                continue;
            }
            return LocalDateTime.of(date, time);
        }

        throw new IllegalStateException("找不到可用工作时段，请检查工作日历配置");
    }

    private boolean isWorkingDate(LocalDate date, CalendarSchedule schedule) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return schedule.workdays().contains(dayOfWeek.getValue()) && !schedule.holidays().contains(date);
    }

    private record CalendarSchedule(
            Set<Integer> workdays,
            LocalTime workStartTime,
            LocalTime workEndTime,
            Set<LocalDate> holidays
    ) {
    }
}
