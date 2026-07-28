package com.ntn.fziot.mailtrace.application.bizservice.holiday;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.holiday.HolidaySaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.holiday.HolidayVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.HolidayEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.WorkCalendarEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.HolidayMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.WorkCalendarMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayMapper holidayMapper;
    @Mock
    private WorkCalendarMapper workCalendarMapper;
    @Mock
    private OperationLogMapper operationLogMapper;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private HolidayService holidayService;

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "系统管理员", "admin@example.com", "ADMIN");
    private final CurrentUserPrincipal agent = new CurrentUserPrincipal(
            2L, "agent", "处理人", "agent@example.com", "AGENT");

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        initTableInfo(configuration, "HolidayServiceTest.HolidayEntity", HolidayEntity.class);
    }

    @BeforeEach
    void setUp() {
        allowAdminAndAgentOperationalPermissions();
        lenient().when(holidayMapper.selectCount(any())).thenReturn(0L);
        lenient().when(workCalendarMapper.selectById(10L)).thenReturn(calendar(10L));
    }

    private void allowAdminAndAgentOperationalPermissions() {
        lenient().doAnswer(invocation -> {
            CurrentUserPrincipal principal = invocation.getArgument(0);
            String permissionCode = invocation.getArgument(1);
            String message = invocation.getArgument(2);
            if (principal == null) {
                throw new BusinessException(40302, "未登录");
            }
            if ("ADMIN".equals(principal.roleCode()) || isAgentOperationalPermission(principal, permissionCode)) {
                return null;
            }
            throw new BusinessException(40302, message);
        }).when(permissionService).assertPermission(any(), any(), any());
    }

    private boolean isAgentOperationalPermission(CurrentUserPrincipal principal, String permissionCode) {
        return "AGENT".equals(principal.roleCode())
                && (permissionCode.startsWith("ticket:")
                || permissionCode.startsWith("ticket_attachment:")
                || "customer:read".equals(permissionCode)
                || "dashboard:read".equals(permissionCode));
    }

    @Test
    void createHoliday_shouldNormalizeNameAndPersist() {
        when(holidayMapper.insert(any(HolidayEntity.class))).thenAnswer(invocation -> {
            HolidayEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        });
        when(holidayMapper.selectById(100L)).thenReturn(holiday(100L, 10L, "2026-10-01", "国庆节"));

        HolidayVO vo = holidayService.createHoliday(admin, saveRequest(10L, "2026-10-01", " 国庆节 "));

        ArgumentCaptor<HolidayEntity> holidayCaptor = ArgumentCaptor.forClass(HolidayEntity.class);
        verify(holidayMapper).insert(holidayCaptor.capture());
        HolidayEntity saved = holidayCaptor.getValue();
        assertEquals(10L, saved.getCalendarId());
        assertEquals(LocalDate.parse("2026-10-01"), saved.getHolidayDate());
        assertEquals("国庆节", saved.getHolidayName());
        assertEquals("国庆节", vo.holidayName());
    }

    @Test
    void createHoliday_whenCalendarNotExists_shouldReject() {
        when(workCalendarMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> holidayService.createHoliday(admin, saveRequest(999L, "2026-10-01", "国庆节")));

        assertTrue(ex.getMessage().contains("工作日历不存在"));
    }

    @Test
    void createHoliday_whenDateDuplicate_shouldReject() {
        when(holidayMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> holidayService.createHoliday(admin, saveRequest(10L, "2026-10-01", "国庆节")));

        assertTrue(ex.getMessage().contains("该节假日日期已存在"));
    }

    @Test
    void updateHoliday_whenDateDuplicate_shouldReject() {
        when(holidayMapper.selectById(100L)).thenReturn(holiday(100L, 10L, "2026-10-01", "国庆节"));
        when(holidayMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> holidayService.updateHoliday(admin, 100L, saveRequest(10L, "2026-10-02", "调休日")));

        assertTrue(ex.getMessage().contains("该节假日日期已存在"));
    }

    @Test
    void updateHoliday_shouldPersistEditableFields() {
        when(holidayMapper.selectById(100L))
                .thenReturn(holiday(100L, 10L, "2026-10-01", "国庆节"))
                .thenReturn(holiday(100L, 10L, "2026-10-02", "调休日"));

        HolidayVO vo = holidayService.updateHoliday(admin, 100L, saveRequest(10L, "2026-10-02", "调休日"));

        ArgumentCaptor<HolidayEntity> holidayCaptor = ArgumentCaptor.forClass(HolidayEntity.class);
        verify(holidayMapper).updateById(holidayCaptor.capture());
        HolidayEntity saved = holidayCaptor.getValue();
        assertEquals(100L, saved.getId());
        assertEquals(LocalDate.parse("2026-10-02"), saved.getHolidayDate());
        assertEquals("调休日", vo.holidayName());
    }

    @Test
    void deleteHoliday_shouldDeleteAndRecordLog() {
        when(holidayMapper.selectById(100L)).thenReturn(holiday(100L, 10L, "2026-10-01", "国庆节"));

        holidayService.deleteHoliday(admin, 100L);

        verify(holidayMapper).deleteById(100L);
        verify(operationLogMapper).insert(any());
    }

    @Test
    void listHolidays_whenNotAdmin_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> holidayService.listHolidays(agent, null, null, null, null));

        assertTrue(ex.getMessage().contains("无权查看节假日"));
    }

    @Test
    void listHolidays_whenDateRangeInvalid_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> holidayService.listHolidays(admin, 10L, null,
                        LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-01")));

        assertTrue(ex.getMessage().contains("开始日期不能晚于结束日期"));
    }

    @Test
    void listHolidays_shouldReturnRecordsAndSummary() {
        when(holidayMapper.selectList(any())).thenReturn(List.of(
                holiday(100L, 10L, "2026-10-01", "国庆节"),
                holiday(101L, 10L, "2026-10-02", "国庆调休")
        ));
        when(holidayMapper.selectCount(any())).thenReturn(2L);

        var response = holidayService.listHolidays(admin, 10L, "国庆",
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-07"));

        assertEquals(2, response.records().size());
        assertEquals(2L, response.summary().totalCount());
    }

    private HolidaySaveRequest saveRequest(Long calendarId, String holidayDate, String holidayName) {
        HolidaySaveRequest request = new HolidaySaveRequest();
        request.setCalendarId(calendarId);
        request.setHolidayDate(LocalDate.parse(holidayDate));
        request.setHolidayName(holidayName);
        return request;
    }

    private HolidayEntity holiday(Long id, Long calendarId, String holidayDate, String holidayName) {
        HolidayEntity holiday = new HolidayEntity();
        holiday.setId(id);
        holiday.setCalendarId(calendarId);
        holiday.setHolidayDate(LocalDate.parse(holidayDate));
        holiday.setHolidayName(holidayName);
        holiday.setCreatedAt(LocalDateTime.now());
        holiday.setUpdatedAt(LocalDateTime.now());
        return holiday;
    }

    private WorkCalendarEntity calendar(Long id) {
        WorkCalendarEntity calendar = new WorkCalendarEntity();
        calendar.setId(id);
        calendar.setCalendarName("标准日历");
        return calendar;
    }

    private static void initTableInfo(MybatisConfiguration configuration, String namespace, Class<?> entityClass) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, namespace);
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
