package com.ntn.fziot.mailtrace.application.bizservice.assignment;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleMatchResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleSortItem;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleSortRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleTestRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.AssignmentRuleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.AssignmentRuleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentRuleServiceTest {

    @Mock
    private AssignmentRuleMapper assignmentRuleMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private OperationLogMapper operationLogMapper;

    @InjectMocks
    private AssignmentRuleService assignmentRuleService;

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "系统管理员", "admin@example.com", "ADMIN");
    private final CurrentUserPrincipal agent = new CurrentUserPrincipal(
            2L, "agent", "处理人", "agent@example.com", "AGENT");

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        initTableInfo(configuration, "AssignmentRuleServiceTest.AssignmentRuleEntity", AssignmentRuleEntity.class);
    }

    @BeforeEach
    void setUp() {
        lenient().when(assignmentRuleMapper.selectCount(any())).thenReturn(0L);
        lenient().when(userMapper.selectById(2L)).thenReturn(agentUser(2L, "验收客服", "AGENT", true));
        lenient().when(userMapper.selectById(3L)).thenReturn(agentUser(3L, "停用客服", "AGENT", false));
        lenient().when(userMapper.selectById(4L)).thenReturn(agentUser(4L, "管理员", "ADMIN", true));
    }

    @Test
    void createRule_shouldNormalizeAndPersistRule() {
        when(assignmentRuleMapper.insert(any(AssignmentRuleEntity.class))).thenAnswer(invocation -> {
            AssignmentRuleEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        });
        when(assignmentRuleMapper.selectById(100L)).thenReturn(rule(100L, "VIP", true, 10,
                false, "FROM_EMAIL", "vip@example.com", 2L, true));

        AssignmentRuleVO vo = assignmentRuleService.createRule(admin, saveRequest(
                "VIP", "FROM_EMAIL", " VIP@EXAMPLE.COM ", 2L, 10, true, false));

        ArgumentCaptor<AssignmentRuleEntity> ruleCaptor = ArgumentCaptor.forClass(AssignmentRuleEntity.class);
        verify(assignmentRuleMapper).insert(ruleCaptor.capture());
        AssignmentRuleEntity saved = ruleCaptor.getValue();
        assertEquals("VIP", saved.getRuleName());
        assertEquals("FROM_EMAIL", saved.getMatchType());
        assertEquals("vip@example.com", saved.getMatchValue());
        assertEquals(2L, saved.getAssigneeId());
        assertEquals("admin", saved.getCreatedBy());
        assertEquals("验收客服", vo.assigneeName());
    }

    @Test
    void createRule_whenDefaultAlreadyExists_shouldReject() {
        when(assignmentRuleMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> assignmentRuleService.createRule(admin, saveRequest(
                        "默认规则", "DEFAULT", null, 2L, 100, true, true)));

        assertTrue(ex.getMessage().contains("默认分配规则已存在"));
    }

    @Test
    void createRule_whenDisabledOrAdminAssignee_shouldReject() {
        BusinessException disabled = assertThrows(BusinessException.class,
                () -> assignmentRuleService.createRule(admin, saveRequest(
                        "停用客服", "SUBJECT_KEYWORD", "vip", 3L, 10, true, false)));
        assertTrue(disabled.getMessage().contains("启用的处理人"));

        BusinessException adminTarget = assertThrows(BusinessException.class,
                () -> assignmentRuleService.createRule(admin, saveRequest(
                        "管理员", "SUBJECT_KEYWORD", "vip", 4L, 10, true, false)));
        assertTrue(adminTarget.getMessage().contains("启用的处理人"));
    }

    @Test
    void updateEnabled_shouldPatchStatusAndReturnLatestRule() {
        when(assignmentRuleMapper.selectById(100L))
                .thenReturn(rule(100L, "VIP", true, 10, false, "SUBJECT_KEYWORD", "vip", 2L, true))
                .thenReturn(rule(100L, "VIP", false, 10, false, "SUBJECT_KEYWORD", "vip", 2L, true));
        AssignmentRuleEnabledRequest request = new AssignmentRuleEnabledRequest();
        request.setEnabled(false);

        AssignmentRuleVO vo = assignmentRuleService.updateEnabled(admin, 100L, request);

        verify(assignmentRuleMapper).update(eq(null), any());
        assertEquals(false, vo.enabled());
    }

    @Test
    void sortRules_shouldRejectDuplicateRuleId() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> assignmentRuleService.sortRules(admin, new AssignmentRuleSortRequest(List.of(
                        new AssignmentRuleSortItem(100L, 10),
                        new AssignmentRuleSortItem(100L, 20)
                ))));

        assertTrue(ex.getMessage().contains("排序规则ID重复"));
    }

    @Test
    void sortRules_shouldUpdatePriorityOrder() {
        when(assignmentRuleMapper.selectById(100L)).thenReturn(rule(100L, "VIP", true, 10,
                false, "SUBJECT_KEYWORD", "vip", 2L, true));
        when(assignmentRuleMapper.selectById(101L)).thenReturn(rule(101L, "默认", true, 100,
                true, "DEFAULT", null, 2L, true));
        when(assignmentRuleMapper.selectList(any())).thenReturn(List.of(
                rule(101L, "默认", true, 20, true, "DEFAULT", null, 2L, true),
                rule(100L, "VIP", true, 30, false, "SUBJECT_KEYWORD", "vip", 2L, true)
        ));

        List<AssignmentRuleVO> result = assignmentRuleService.sortRules(admin, new AssignmentRuleSortRequest(List.of(
                new AssignmentRuleSortItem(100L, 30),
                new AssignmentRuleSortItem(101L, 20)
        )));

        verify(assignmentRuleMapper, org.mockito.Mockito.times(2)).update(eq(null), any());
        assertEquals(2, result.size());
        assertEquals(101L, result.get(0).id());
    }

    @Test
    void deleteRule_shouldDeleteAndWriteLog() {
        when(assignmentRuleMapper.selectById(100L)).thenReturn(rule(100L, "VIP", true, 10,
                false, "SUBJECT_KEYWORD", "vip", 2L, true));

        assignmentRuleService.deleteRule(admin, 100L);

        verify(assignmentRuleMapper).deleteById(100L);
        verify(operationLogMapper).insert(any());
    }

    @Test
    void listRules_whenNotAdmin_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> assignmentRuleService.listRules(agent, null, null, null));

        assertTrue(ex.getMessage().contains("仅管理员"));
    }

    @Test
    void matchForTicket_shouldHitSubjectKeywordBeforeDefault() {
        when(assignmentRuleMapper.selectList(any())).thenReturn(List.of(
                rule(200L, "VIP 关键词", true, 10, false, "SUBJECT_KEYWORD", "VIP", 2L, true),
                rule(201L, "默认规则", true, 100, true, "DEFAULT", null, 2L, true)
        ));

        AssignmentRuleMatchResult result = assignmentRuleService.matchForTicket(
                11L, "support@example.com", "关于 VIP 订单的咨询", "customer@example.com");

        assertNotNull(result);
        assertEquals(200L, result.ruleId());
        assertEquals("SUBJECT_KEYWORD", result.matchType());
        assertEquals(2L, result.assigneeId());
    }

    @Test
    void matchForTicket_shouldMatchMailboxByIdOrAddress() {
        when(assignmentRuleMapper.selectList(any())).thenReturn(List.of(
                rule(202L, "邮箱规则", true, 10, false, "MAILBOX", "support@example.com", 2L, true)
        ));

        AssignmentRuleMatchResult result = assignmentRuleService.matchForTicket(
                11L, "SUPPORT@example.com", "普通咨询", "customer@example.com");

        assertNotNull(result);
        assertEquals(202L, result.ruleId());
    }

    @Test
    void matchForTicket_shouldMatchFromEmailIgnoringCase() {
        when(assignmentRuleMapper.selectList(any())).thenReturn(List.of(
                rule(203L, "来源邮箱规则", true, 10, false, "FROM_EMAIL", "vip@example.com", 2L, false)
        ));

        AssignmentRuleMatchResult result = assignmentRuleService.matchForTicket(
                11L, "support@example.com", "普通咨询", "VIP@EXAMPLE.COM");

        assertNotNull(result);
        assertEquals(203L, result.ruleId());
        assertEquals(false, result.notifyEnabled());
    }

    @Test
    void matchForTicket_shouldSkipInvalidAssigneeAndFallbackDefault() {
        when(assignmentRuleMapper.selectList(any())).thenReturn(List.of(
                rule(204L, "停用处理人规则", true, 10, false, "SUBJECT_KEYWORD", "VIP", 3L, true),
                rule(205L, "默认规则", true, 100, true, "DEFAULT", null, 2L, true)
        ));

        AssignmentRuleMatchResult result = assignmentRuleService.matchForTicket(
                11L, "support@example.com", "VIP 咨询", "customer@example.com");

        assertNotNull(result);
        assertEquals(205L, result.ruleId());
    }

    @Test
    void matchForTicket_whenNoRuleMatches_shouldReturnNull() {
        when(assignmentRuleMapper.selectList(any())).thenReturn(List.of(
                rule(206L, "关键词规则", true, 10, false, "SUBJECT_KEYWORD", "VIP", 2L, true)
        ));

        AssignmentRuleMatchResult result = assignmentRuleService.matchForTicket(
                11L, "support@example.com", "普通咨询", "customer@example.com");

        assertNull(result);
    }

    @Test
    void testMatch_shouldReturnExplainableResultForAdmin() {
        when(assignmentRuleMapper.selectList(any())).thenReturn(List.of(
                rule(207L, "邮箱ID规则", true, 10, false, "MAILBOX", "11", 2L, true)
        ));
        AssignmentRuleTestRequest request = new AssignmentRuleTestRequest();
        request.setMailboxId(11L);
        request.setSubject("普通咨询");
        request.setFromEmail("customer@example.com");

        AssignmentRuleMatchResponse response = assignmentRuleService.testMatch(admin, request);

        assertEquals(true, response.matched());
        assertEquals(207L, response.ruleId());
        assertEquals("邮箱ID规则", response.ruleName());
    }

    private AssignmentRuleSaveRequest saveRequest(String ruleName, String matchType, String matchValue,
                                                  Long assigneeId, Integer priorityOrder,
                                                  Boolean notifyEnabled, Boolean defaultRule) {
        AssignmentRuleSaveRequest request = new AssignmentRuleSaveRequest();
        request.setRuleName(ruleName);
        request.setMatchType(matchType);
        request.setMatchValue(matchValue);
        request.setAssigneeId(assigneeId);
        request.setPriorityOrder(priorityOrder);
        request.setNotifyEnabled(notifyEnabled);
        request.setDefaultRule(defaultRule);
        return request;
    }

    private AssignmentRuleEntity rule(Long id, String ruleName, Boolean enabled, Integer priorityOrder,
                                      Boolean defaultRule, String matchType, String matchValue,
                                      Long assigneeId, Boolean notifyEnabled) {
        AssignmentRuleEntity rule = new AssignmentRuleEntity();
        rule.setId(id);
        rule.setRuleName(ruleName);
        rule.setEnabled(enabled);
        rule.setPriorityOrder(priorityOrder);
        rule.setDefaultRule(defaultRule);
        rule.setMatchType(matchType);
        rule.setMatchValue(matchValue);
        rule.setAssigneeId(assigneeId);
        rule.setNotifyEnabled(notifyEnabled);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        return rule;
    }

    private UserEntity agentUser(Long id, String displayName, String roleCode, boolean enabled) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setAccount("user" + id);
        user.setDisplayName(displayName);
        user.setEmail("user" + id + "@example.com");
        user.setRoleCode(roleCode);
        user.setEnabled(enabled);
        return user;
    }

    private static void initTableInfo(MybatisConfiguration configuration, String namespace, Class<?> entityClass) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, namespace);
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
