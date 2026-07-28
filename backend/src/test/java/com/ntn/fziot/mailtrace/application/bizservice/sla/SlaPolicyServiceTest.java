package com.ntn.fziot.mailtrace.application.bizservice.sla;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicyDefaultRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicyEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicySaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicyVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.SlaPolicyEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.WorkCalendarEntity;
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
class SlaPolicyServiceTest {

    @Mock
    private SlaPolicyMapper slaPolicyMapper;
    @Mock
    private WorkCalendarMapper workCalendarMapper;
    @Mock
    private OperationLogMapper operationLogMapper;

    @InjectMocks
    private SlaPolicyService slaPolicyService;

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "系统管理员", "admin@example.com", "ADMIN");
    private final CurrentUserPrincipal agent = new CurrentUserPrincipal(
            2L, "agent", "处理人", "agent@example.com", "AGENT");

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        initTableInfo(configuration, "SlaPolicyServiceTest.SlaPolicyEntity", SlaPolicyEntity.class);
    }

    @BeforeEach
    void setUp() {
        lenient().when(slaPolicyMapper.selectCount(any())).thenReturn(0L);
        lenient().when(workCalendarMapper.selectById(1L)).thenReturn(workCalendar(1L));
    }

    @Test
    void createPolicy_shouldValidateAndPersistPolicy() {
        when(slaPolicyMapper.insert(any(SlaPolicyEntity.class))).thenAnswer(invocation -> {
            SlaPolicyEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        });
        when(slaPolicyMapper.selectById(100L)).thenReturn(policy(100L, "标准 SLA", true, false,
                4, 24, 1, 2, 1L));

        SlaPolicyVO vo = slaPolicyService.createPolicy(admin,
                saveRequest(" 标准 SLA ", true, false, 4, 24, 1, 2, 1L));

        ArgumentCaptor<SlaPolicyEntity> policyCaptor = ArgumentCaptor.forClass(SlaPolicyEntity.class);
        verify(slaPolicyMapper).insert(policyCaptor.capture());
        SlaPolicyEntity saved = policyCaptor.getValue();
        assertEquals("标准 SLA", saved.getPolicyName());
        assertEquals(4, saved.getResponseHours());
        assertEquals(24, saved.getResolveHours());
        assertEquals(1L, saved.getCalendarId());
        assertEquals("admin", saved.getCreatedBy());
        assertEquals("标准 SLA", vo.policyName());
    }

    @Test
    void createPolicy_whenDefaultAlreadyExists_shouldReject() {
        when(slaPolicyMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> slaPolicyService.createPolicy(admin,
                        saveRequest("默认 SLA", true, true, 4, 24, 1, null, 1L)));

        assertTrue(ex.getMessage().contains("默认 SLA 策略已存在"));
    }

    @Test
    void createPolicy_whenResolveLessThanResponse_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> slaPolicyService.createPolicy(admin,
                        saveRequest("错误 SLA", true, false, 8, 4, 1, null, 1L)));

        assertTrue(ex.getMessage().contains("解决时限不能小于首次响应时限"));
    }

    @Test
    void createPolicy_whenWarningNotLessThanResponse_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> slaPolicyService.createPolicy(admin,
                        saveRequest("错误 SLA", true, false, 4, 24, 4, null, 1L)));

        assertTrue(ex.getMessage().contains("预警阈值必须小于首次响应时限"));
    }

    @Test
    void createPolicy_whenCalendarMissing_shouldReject() {
        when(workCalendarMapper.selectById(9L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> slaPolicyService.createPolicy(admin,
                        saveRequest("错误 SLA", true, false, 4, 24, 1, null, 9L)));

        assertTrue(ex.getMessage().contains("工作日历不存在"));
    }

    @Test
    void updateEnabled_whenDefaultPolicyDisabled_shouldReject() {
        when(slaPolicyMapper.selectById(100L)).thenReturn(policy(100L, "默认 SLA", true, true,
                4, 24, 1, null, 1L));
        SlaPolicyEnabledRequest request = new SlaPolicyEnabledRequest();
        request.setEnabled(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> slaPolicyService.updateEnabled(admin, 100L, request));

        assertTrue(ex.getMessage().contains("默认 SLA 策略不能停用"));
    }

    @Test
    void updateDefault_shouldClearOtherDefaultsAndSetCurrent() {
        when(slaPolicyMapper.selectById(100L))
                .thenReturn(policy(100L, "标准 SLA", true, false, 4, 24, 1, null, 1L))
                .thenReturn(policy(100L, "标准 SLA", true, true, 4, 24, 1, null, 1L));
        SlaPolicyDefaultRequest request = new SlaPolicyDefaultRequest();
        request.setDefaultPolicy(true);

        SlaPolicyVO vo = slaPolicyService.updateDefault(admin, 100L, request);

        verify(slaPolicyMapper, org.mockito.Mockito.times(2)).update(eq(null), any());
        assertEquals(true, vo.defaultPolicy());
    }

    @Test
    void updateDefault_whenCancelDefault_shouldReject() {
        when(slaPolicyMapper.selectById(100L))
                .thenReturn(policy(100L, "默认 SLA", true, true, 4, 24, 1, null, 1L));
        SlaPolicyDefaultRequest request = new SlaPolicyDefaultRequest();
        request.setDefaultPolicy(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> slaPolicyService.updateDefault(admin, 100L, request));

        assertTrue(ex.getMessage().contains("系统必须保留一个默认 SLA 策略"));
    }

    @Test
    void deletePolicy_whenDefaultPolicy_shouldReject() {
        when(slaPolicyMapper.selectById(100L)).thenReturn(policy(100L, "默认 SLA", true, true,
                4, 24, 1, null, 1L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> slaPolicyService.deletePolicy(admin, 100L));

        assertTrue(ex.getMessage().contains("默认 SLA 策略不能删除"));
    }

    @Test
    void listPolicies_whenNotAdmin_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> slaPolicyService.listPolicies(agent, null, null, null));

        assertTrue(ex.getMessage().contains("仅管理员"));
    }

    private SlaPolicySaveRequest saveRequest(String policyName, Boolean enabled, Boolean defaultPolicy,
                                             Integer responseHours, Integer resolveHours,
                                             Integer warningRemainHours, Integer escalateAfterBreachHours,
                                             Long calendarId) {
        SlaPolicySaveRequest request = new SlaPolicySaveRequest();
        request.setPolicyName(policyName);
        request.setEnabled(enabled);
        request.setDefaultPolicy(defaultPolicy);
        request.setResponseHours(responseHours);
        request.setResolveHours(resolveHours);
        request.setWarningRemainHours(warningRemainHours);
        request.setEscalateAfterBreachHours(escalateAfterBreachHours);
        request.setCalendarId(calendarId);
        return request;
    }

    private SlaPolicyEntity policy(Long id, String policyName, Boolean enabled, Boolean defaultPolicy,
                                   Integer responseHours, Integer resolveHours,
                                   Integer warningRemainHours, Integer escalateAfterBreachHours,
                                   Long calendarId) {
        SlaPolicyEntity policy = new SlaPolicyEntity();
        policy.setId(id);
        policy.setPolicyName(policyName);
        policy.setEnabled(enabled);
        policy.setDefaultPolicy(defaultPolicy);
        policy.setResponseHours(responseHours);
        policy.setResolveHours(resolveHours);
        policy.setWarningRemainHours(warningRemainHours);
        policy.setEscalateAfterBreachHours(escalateAfterBreachHours);
        policy.setCalendarId(calendarId);
        policy.setCreatedAt(LocalDateTime.now());
        policy.setUpdatedAt(LocalDateTime.now());
        return policy;
    }

    private WorkCalendarEntity workCalendar(Long id) {
        WorkCalendarEntity calendar = new WorkCalendarEntity();
        calendar.setId(id);
        calendar.setCalendarName("标准工作日历");
        calendar.setTimezone("Asia/Shanghai");
        calendar.setWorkdays("1,2,3,4,5");
        return calendar;
    }

    private static void initTableInfo(MybatisConfiguration configuration, String namespace, Class<?> entityClass) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, namespace);
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
