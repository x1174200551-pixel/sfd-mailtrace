package com.ntn.fziot.mailtrace.application.bizservice.sla;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.HolidayEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.SlaPolicyEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.WorkCalendarEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.HolidayMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaDeadlineService {

    private static final int MAX_CALCULATION_DAYS = 20000;
    private static final int DEFAULT_WARNING_REMAIN_HOURS = 1;
    private static final ZoneId STORAGE_ZONE = ZoneId.of("Asia/Shanghai");

    private final SlaPolicyMapper slaPolicyMapper;
    private final MailboxMapper mailboxMapper;
    private final WorkCalendarMapper workCalendarMapper;
    private final HolidayMapper holidayMapper;

    /**
     * 计算新建工单的 SLA 截止时间。
     */
    public SlaDeadlineResult calculateForNewTicket(Long mailboxId, LocalDateTime customerMailAt) {
        // 1、建单只读取来源邮箱显式绑定的 SLA；未绑定或停用时不计算。
        MailboxEntity mailbox = mailboxId == null ? null : mailboxMapper.selectById(mailboxId);
        if (mailbox == null || mailbox.getSlaPolicyId() == null) {
            return SlaDeadlineResult.none();
        }
        SlaPolicyEntity policy = slaPolicyMapper.selectById(mailbox.getSlaPolicyId());
        if (policy == null || !Boolean.TRUE.equals(policy.getEnabled())
                || !Objects.equals(policy.getEnterpriseId(), mailbox.getEnterpriseId())) {
            log.warn("邮箱绑定的 SLA 不可用，跳过截止时间计算 mailboxId={} policyId={}",
                    mailboxId, mailbox.getSlaPolicyId());
            return SlaDeadlineResult.none();
        }

        // 2、读取同企业工作日历；配置损坏时不阻断建单，只跳过本次 SLA 写入。
        WorkCalendarEntity calendar = workCalendarMapper.selectById(policy.getCalendarId());
        if (calendar == null || !Objects.equals(calendar.getEnterpriseId(), mailbox.getEnterpriseId())) {
            log.warn("SLA 策略绑定的工作日历不存在，跳过截止时间计算 policyId={} calendarId={}",
                    policy.getId(), policy.getCalendarId());
            return SlaDeadlineResult.none();
        }

        // 3、从客户来信时间起算；统一在工作日历时区内计算，再转换回系统存储时区。
        LocalDateTime startAt = customerMailAt != null ? customerMailAt : LocalDateTime.now();
        CalendarSchedule schedule = toSchedule(calendar, loadHolidays(calendar.getId()));
        LocalDateTime calendarStartAt = toCalendarTime(startAt, schedule.zoneId());
        LocalDateTime responseDeadlineAtCalendar = addWorkingHours(
                calendarStartAt, policy.getResponseHours(), schedule);
        LocalDateTime resolveDeadlineAtCalendar = policy.getResolveHours() == null
                ? null
                : addWorkingHours(calendarStartAt, policy.getResolveHours(), schedule);

        // 4、预警和升级触发时间与截止时间同时固化，避免后续策略或日历修改影响历史工单。
        return buildResult(policy, schedule, responseDeadlineAtCalendar, resolveDeadlineAtCalendar);
    }

    /**
     * 为迁移前已经存在的工单补齐通知调度快照；两个既有截止时间保持不变。
     */
    public SlaDeadlineResult calculateForStoredDeadlines(Long policyId,
                                                          LocalDateTime responseDeadline,
                                                          LocalDateTime resolveDeadline) {
        if (policyId == null || (responseDeadline == null && resolveDeadline == null)) {
            return SlaDeadlineResult.none();
        }
        SlaPolicyEntity policy = slaPolicyMapper.selectById(policyId);
        if (policy == null || policy.getCalendarId() == null) {
            return new SlaDeadlineResult(policyId, null, null, responseDeadline, resolveDeadline,
                    null, null, null, null);
        }
        WorkCalendarEntity calendar = workCalendarMapper.selectById(policy.getCalendarId());
        if (calendar == null) {
            return new SlaDeadlineResult(policyId, null, null, responseDeadline, resolveDeadline,
                    null, null, null, null);
        }
        CalendarSchedule schedule = toSchedule(calendar, loadHolidays(calendar.getId()));
        LocalDateTime responseAtCalendar = toCalendarTime(responseDeadline, schedule.zoneId());
        LocalDateTime resolveAtCalendar = toCalendarTime(resolveDeadline, schedule.zoneId());
        return buildResult(policy, schedule, responseAtCalendar, resolveAtCalendar);
    }

    private SlaDeadlineResult buildResult(SlaPolicyEntity policy, CalendarSchedule schedule,
                                          LocalDateTime responseDeadlineAtCalendar,
                                          LocalDateTime resolveDeadlineAtCalendar) {
        int warningHours = positiveOrDefault(policy.getWarningRemainHours(), DEFAULT_WARNING_REMAIN_HOURS);
        Integer escalationHours = positiveOrNull(policy.getEscalateAfterBreachHours());
        LocalDateTime responseWarningAt = subtractWorkingHours(
                responseDeadlineAtCalendar, warningHours, schedule);
        LocalDateTime responseEscalationAt = escalationHours == null
                ? null : addWorkingHours(responseDeadlineAtCalendar, escalationHours, schedule);
        LocalDateTime resolveWarningAt = resolveDeadlineAtCalendar == null
                ? null : subtractWorkingHours(resolveDeadlineAtCalendar, warningHours, schedule);
        LocalDateTime resolveEscalationAt = resolveDeadlineAtCalendar == null || escalationHours == null
                ? null : addWorkingHours(resolveDeadlineAtCalendar, escalationHours, schedule);

        return new SlaDeadlineResult(
                policy.getId(),
                warningHours,
                escalationHours,
                toStorageTime(responseDeadlineAtCalendar, schedule.zoneId()),
                toStorageTime(resolveDeadlineAtCalendar, schedule.zoneId()),
                toStorageTime(responseWarningAt, schedule.zoneId()),
                toStorageTime(responseEscalationAt, schedule.zoneId()),
                toStorageTime(resolveWarningAt, schedule.zoneId()),
                toStorageTime(resolveEscalationAt, schedule.zoneId())
        );
    }

    private Set<LocalDate> loadHolidays(Long calendarId) {
        return holidayMapper.selectList(new LambdaQueryWrapper<HolidayEntity>()
                        .eq(HolidayEntity::getCalendarId, calendarId))
                .stream()
                .map(HolidayEntity::getHolidayDate)
                .collect(Collectors.toSet());
    }

    private CalendarSchedule toSchedule(WorkCalendarEntity calendar, Set<LocalDate> holidays) {
        Set<Integer> workdays = Arrays.stream(calendar.getWorkdays().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Integer::valueOf)
                .collect(Collectors.toSet());
        return new CalendarSchedule(workdays, calendar.getWorkStartTime(), calendar.getWorkEndTime(), holidays,
                ZoneId.of(calendar.getTimezone()));
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

    private LocalDateTime subtractWorkingHours(LocalDateTime endAt, int hours, CalendarSchedule schedule) {
        long remainingMinutes = Duration.ofHours(hours).toMinutes();
        LocalDateTime cursor = normalizeToPreviousWorkingTime(endAt, schedule);

        for (int i = 0; i < MAX_CALCULATION_DAYS; i++) {
            cursor = normalizeToPreviousWorkingTime(cursor, schedule);
            LocalDateTime dayStart = LocalDateTime.of(cursor.toLocalDate(), schedule.workStartTime());
            long availableMinutes = Duration.between(dayStart, cursor).toMinutes();
            if (remainingMinutes <= availableMinutes) {
                return cursor.minusMinutes(remainingMinutes);
            }
            remainingMinutes -= availableMinutes;
            cursor = LocalDateTime.of(cursor.toLocalDate().minusDays(1), schedule.workEndTime());
        }

        throw new IllegalStateException("SLA 预警时间计算超过最大跨度，请检查工作日历配置");
    }

    private LocalDateTime normalizeToPreviousWorkingTime(LocalDateTime dateTime, CalendarSchedule schedule) {
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();

        for (int i = 0; i < MAX_CALCULATION_DAYS; i++) {
            if (!isWorkingDate(date, schedule)) {
                date = date.minusDays(1);
                time = schedule.workEndTime();
                continue;
            }
            if (time.isAfter(schedule.workEndTime())) {
                return LocalDateTime.of(date, schedule.workEndTime());
            }
            if (!time.isAfter(schedule.workStartTime())) {
                date = date.minusDays(1);
                time = schedule.workEndTime();
                continue;
            }
            return LocalDateTime.of(date, time);
        }

        throw new IllegalStateException("找不到可用的历史工作时段，请检查工作日历配置");
    }

    private LocalDateTime toCalendarTime(LocalDateTime storageTime, ZoneId calendarZone) {
        if (storageTime == null) {
            return null;
        }
        return storageTime.atZone(STORAGE_ZONE).withZoneSameInstant(calendarZone).toLocalDateTime();
    }

    private LocalDateTime toStorageTime(LocalDateTime calendarTime, ZoneId calendarZone) {
        if (calendarTime == null) {
            return null;
        }
        ZonedDateTime zoned = calendarTime.atZone(calendarZone);
        return zoned.withZoneSameInstant(STORAGE_ZONE).toLocalDateTime();
    }

    private int positiveOrDefault(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private Integer positiveOrNull(Integer value) {
        return value == null || value <= 0 ? null : value;
    }

    private boolean isWorkingDate(LocalDate date, CalendarSchedule schedule) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return schedule.workdays().contains(dayOfWeek.getValue()) && !schedule.holidays().contains(date);
    }

    private record CalendarSchedule(
            Set<Integer> workdays,
            LocalTime workStartTime,
            LocalTime workEndTime,
            Set<LocalDate> holidays,
            ZoneId zoneId
    ) {
    }
}
