package com.ntn.fziot.mailtrace.application.bizservice.calendar;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.calendar.WorkCalendarDefaultRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.calendar.WorkCalendarSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.calendar.WorkCalendarVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.WorkCalendarEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.HolidayMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.SlaPolicyMapper;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkCalendarServiceTest {

    @Mock
    private WorkCalendarMapper workCalendarMapper;
    @Mock
    private EnterpriseMapper enterpriseMapper;
    @Mock
    private SlaPolicyMapper slaPolicyMapper;
    @Mock
    private HolidayMapper holidayMapper;
    @Mock
    private OperationLogMapper operationLogMapper;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private WorkCalendarService workCalendarService;

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "系统管理员", "admin@example.com", "ADMIN");
    private final CurrentUserPrincipal agent = new CurrentUserPrincipal(
            2L, "agent", "处理人", "agent@example.com", "AGENT");

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        initTableInfo(configuration, "WorkCalendarServiceTest.WorkCalendarEntity", WorkCalendarEntity.class);
    }

    @BeforeEach
    void setUp() {
        allowAdminAndAgentOperationalPermissions();
        lenient().when(workCalendarMapper.selectCount(any())).thenReturn(0L);
        lenient().when(slaPolicyMapper.selectCount(any())).thenReturn(0L);
        lenient().when(holidayMapper.selectCount(any())).thenReturn(0L);
        lenient().when(enterpriseMapper.selectById(1L)).thenReturn(enterprise(1L, true));
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
    void createCalendar_shouldNormalizeWorkdaysAndPersist() {
        when(workCalendarMapper.insert(any(WorkCalendarEntity.class))).thenAnswer(invocation -> {
            WorkCalendarEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        });
        when(workCalendarMapper.selectById(100L)).thenReturn(calendar(100L, "标准日历", "Asia/Shanghai",
                "1,2,3,4,5", "09:00", "18:00", false));

        WorkCalendarVO vo = workCalendarService.createCalendar(admin, saveRequest(
                " 标准日历 ", "Asia/Shanghai", List.of(5, 1, 3, 2, 4), "09:00", "18:00", false));

        ArgumentCaptor<WorkCalendarEntity> calendarCaptor = ArgumentCaptor.forClass(WorkCalendarEntity.class);
        verify(workCalendarMapper).insert(calendarCaptor.capture());
        WorkCalendarEntity saved = calendarCaptor.getValue();
        assertEquals("标准日历", saved.getCalendarName());
        assertEquals("1,2,3,4,5", saved.getWorkdays());
        assertEquals(LocalTime.of(9, 0), saved.getWorkStartTime());
        assertEquals(LocalTime.of(18, 0), saved.getWorkEndTime());
        assertEquals(List.of(1, 2, 3, 4, 5), vo.workdays());
    }

    @Test
    void createCalendar_whenDefaultAlreadyExists_shouldReject() {
        when(workCalendarMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workCalendarService.createCalendar(admin, saveRequest(
                        "默认日历", "Asia/Shanghai", List.of(1, 2, 3, 4, 5), "09:00", "18:00", true)));

        assertTrue(ex.getMessage().contains("默认工作日历已存在"));
    }

    @Test
    void createCalendar_whenWorkdayDuplicate_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> workCalendarService.createCalendar(admin, saveRequest(
                        "错误日历", "Asia/Shanghai", List.of(1, 1), "09:00", "18:00", false)));

        assertTrue(ex.getMessage().contains("工作日不能重复"));
    }

    @Test
    void createCalendar_whenTimeRangeInvalid_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> workCalendarService.createCalendar(admin, saveRequest(
                        "错误日历", "Asia/Shanghai", List.of(1, 2, 3), "18:00", "09:00", false)));

        assertTrue(ex.getMessage().contains("工作开始时间必须早于工作结束时间"));
    }

    @Test
    void createCalendar_whenTimezoneInvalid_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> workCalendarService.createCalendar(admin, saveRequest(
                        "错误日历", "Asia/NotExists", List.of(1, 2, 3), "09:00", "18:00", false)));

        assertTrue(ex.getMessage().contains("时区不合法"));
    }

    @Test
    void updateDefault_shouldClearOtherDefaultsAndSetCurrent() {
        when(workCalendarMapper.selectById(100L))
                .thenReturn(calendar(100L, "标准日历", "Asia/Shanghai", "1,2,3,4,5", "09:00", "18:00", false))
                .thenReturn(calendar(100L, "标准日历", "Asia/Shanghai", "1,2,3,4,5", "09:00", "18:00", true));
        WorkCalendarDefaultRequest request = new WorkCalendarDefaultRequest();
        request.setDefaultCalendar(true);

        WorkCalendarVO vo = workCalendarService.updateDefault(admin, 100L, request);

        verify(workCalendarMapper, org.mockito.Mockito.times(2)).update(eq(null), any());
        assertEquals(true, vo.defaultCalendar());
    }

    @Test
    void updateDefault_whenCancelDefault_shouldReject() {
        when(workCalendarMapper.selectById(100L))
                .thenReturn(calendar(100L, "默认日历", "Asia/Shanghai", "1,2,3,4,5", "09:00", "18:00", true));
        WorkCalendarDefaultRequest request = new WorkCalendarDefaultRequest();
        request.setDefaultCalendar(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workCalendarService.updateDefault(admin, 100L, request));

        assertTrue(ex.getMessage().contains("系统必须保留一个默认工作日历"));
    }

    @Test
    void deleteCalendar_whenReferencedBySlaPolicy_shouldReject() {
        when(workCalendarMapper.selectById(100L))
                .thenReturn(calendar(100L, "标准日历", "Asia/Shanghai", "1,2,3,4,5", "09:00", "18:00", false));
        when(slaPolicyMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workCalendarService.deleteCalendar(admin, 100L));

        assertTrue(ex.getMessage().contains("已被 SLA 策略引用"));
    }

    @Test
    void deleteCalendar_whenHolidayConfigured_shouldReject() {
        when(workCalendarMapper.selectById(100L))
                .thenReturn(calendar(100L, "标准日历", "Asia/Shanghai", "1,2,3,4,5", "09:00", "18:00", false));
        when(holidayMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> workCalendarService.deleteCalendar(admin, 100L));

        assertTrue(ex.getMessage().contains("已配置节假日"));
    }

    @Test
    void listCalendars_whenNotAdmin_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> workCalendarService.listCalendars(agent, null, null, null));

        assertTrue(ex.getMessage().contains("无权查看工作日历"));
    }

    private WorkCalendarSaveRequest saveRequest(String calendarName, String timezone, List<Integer> workdays,
                                                String startTime, String endTime, Boolean defaultCalendar) {
        WorkCalendarSaveRequest request = new WorkCalendarSaveRequest();
        request.setEnterpriseId(1L);
        request.setCalendarName(calendarName);
        request.setTimezone(timezone);
        request.setWorkdays(workdays);
        request.setWorkStartTime(startTime);
        request.setWorkEndTime(endTime);
        request.setDefaultCalendar(defaultCalendar);
        return request;
    }

    private WorkCalendarEntity calendar(Long id, String calendarName, String timezone, String workdays,
                                        String startTime, String endTime, Boolean defaultCalendar) {
        WorkCalendarEntity calendar = new WorkCalendarEntity();
        calendar.setId(id);
        calendar.setEnterpriseId(1L);
        calendar.setCalendarName(calendarName);
        calendar.setTimezone(timezone);
        calendar.setWorkdays(workdays);
        calendar.setWorkStartTime(LocalTime.parse(startTime));
        calendar.setWorkEndTime(LocalTime.parse(endTime));
        calendar.setDefaultCalendar(defaultCalendar);
        calendar.setCreatedAt(LocalDateTime.now());
        calendar.setUpdatedAt(LocalDateTime.now());
        return calendar;
    }

    private EnterpriseEntity enterprise(Long id, boolean enabled) {
        EnterpriseEntity enterprise = new EnterpriseEntity();
        enterprise.setId(id);
        enterprise.setEnabled(enabled);
        return enterprise;
    }

    private static void initTableInfo(MybatisConfiguration configuration, String namespace, Class<?> entityClass) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, namespace);
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
