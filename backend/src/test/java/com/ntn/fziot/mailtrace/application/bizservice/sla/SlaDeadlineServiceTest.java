package com.ntn.fziot.mailtrace.application.bizservice.sla;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.HolidayEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.SlaPolicyEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.WorkCalendarEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.HolidayMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.SlaPolicyMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.WorkCalendarMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlaDeadlineServiceTest {

    @Mock
    private SlaPolicyMapper slaPolicyMapper;
    @Mock
    private WorkCalendarMapper workCalendarMapper;
    @Mock
    private HolidayMapper holidayMapper;

    @InjectMocks
    private SlaDeadlineService slaDeadlineService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        initTableInfo(configuration, "SlaDeadlineServiceTest.SlaPolicyEntity", SlaPolicyEntity.class);
        initTableInfo(configuration, "SlaDeadlineServiceTest.WorkCalendarEntity", WorkCalendarEntity.class);
        initTableInfo(configuration, "SlaDeadlineServiceTest.HolidayEntity", HolidayEntity.class);
    }

    @BeforeEach
    void setUp() {
        lenient().when(slaPolicyMapper.selectOne(any())).thenReturn(policy(20L, 10L, 4, 10));
        lenient().when(workCalendarMapper.selectById(10L)).thenReturn(calendar(10L));
        lenient().when(holidayMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void calculateForNewTicket_whenWithinWorkingHours_shouldAddWorkingHoursSameDayAndNextDay() {
        SlaDeadlineResult result = slaDeadlineService.calculateForNewTicket(
                LocalDateTime.parse("2026-07-27T10:00:00"));

        assertEquals(20L, result.policyId());
        assertEquals(LocalDateTime.parse("2026-07-27T14:00:00"), result.responseDeadline());
        assertEquals(LocalDateTime.parse("2026-07-28T11:00:00"), result.resolveDeadline());
    }

    @Test
    void calculateForNewTicket_whenBeforeWorkTime_shouldStartAtWorkStart() {
        when(slaPolicyMapper.selectOne(any())).thenReturn(policy(20L, 10L, 1, null));

        SlaDeadlineResult result = slaDeadlineService.calculateForNewTicket(
                LocalDateTime.parse("2026-07-27T08:00:00"));

        assertEquals(LocalDateTime.parse("2026-07-27T10:00:00"), result.responseDeadline());
        assertNull(result.resolveDeadline());
    }

    @Test
    void calculateForNewTicket_whenAfterWorkTimeAndHoliday_shouldSkipToNextWorkingDay() {
        when(slaPolicyMapper.selectOne(any())).thenReturn(policy(20L, 10L, 2, null));
        when(holidayMapper.selectList(any())).thenReturn(List.of(holiday(10L, "2026-08-03")));

        SlaDeadlineResult result = slaDeadlineService.calculateForNewTicket(
                LocalDateTime.parse("2026-07-31T17:00:00"));

        assertEquals(LocalDateTime.parse("2026-08-04T10:00:00"), result.responseDeadline());
    }

    @Test
    void calculateForNewTicket_whenNoEnabledPolicy_shouldReturnNone() {
        when(slaPolicyMapper.selectOne(any())).thenReturn(null);

        SlaDeadlineResult result = slaDeadlineService.calculateForNewTicket(
                LocalDateTime.parse("2026-07-27T10:00:00"));

        assertNull(result.policyId());
        assertNull(result.responseDeadline());
        assertNull(result.resolveDeadline());
    }

    @Test
    void calculateForNewTicket_whenPolicyCalendarMissing_shouldReturnNone() {
        when(workCalendarMapper.selectById(10L)).thenReturn(null);

        SlaDeadlineResult result = slaDeadlineService.calculateForNewTicket(
                LocalDateTime.parse("2026-07-27T10:00:00"));

        assertNull(result.policyId());
        assertNull(result.responseDeadline());
        assertNull(result.resolveDeadline());
    }

    private SlaPolicyEntity policy(Long id, Long calendarId, Integer responseHours, Integer resolveHours) {
        SlaPolicyEntity policy = new SlaPolicyEntity();
        policy.setId(id);
        policy.setPolicyName("标准 SLA");
        policy.setEnabled(true);
        policy.setDefaultPolicy(true);
        policy.setCalendarId(calendarId);
        policy.setResponseHours(responseHours);
        policy.setResolveHours(resolveHours);
        policy.setWarningRemainHours(1);
        return policy;
    }

    private WorkCalendarEntity calendar(Long id) {
        WorkCalendarEntity calendar = new WorkCalendarEntity();
        calendar.setId(id);
        calendar.setCalendarName("标准日历");
        calendar.setTimezone("Asia/Shanghai");
        calendar.setWorkdays("1,2,3,4,5");
        calendar.setWorkStartTime(LocalTime.parse("09:00"));
        calendar.setWorkEndTime(LocalTime.parse("18:00"));
        return calendar;
    }

    private HolidayEntity holiday(Long calendarId, String holidayDate) {
        HolidayEntity holiday = new HolidayEntity();
        holiday.setCalendarId(calendarId);
        holiday.setHolidayDate(LocalDate.parse(holidayDate));
        holiday.setHolidayName("节假日");
        return holiday;
    }

    private static void initTableInfo(MybatisConfiguration configuration, String namespace, Class<?> entityClass) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, namespace);
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
