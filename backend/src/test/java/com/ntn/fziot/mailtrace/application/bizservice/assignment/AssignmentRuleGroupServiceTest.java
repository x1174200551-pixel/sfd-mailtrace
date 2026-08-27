package com.ntn.fziot.mailtrace.application.bizservice.assignment;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleGroupSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleGroupVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.AssignmentRuleGroupEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.AssignmentRuleGroupMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.AssignmentRuleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentRuleGroupServiceTest {

    @Mock
    private AssignmentRuleGroupMapper groupMapper;
    @Mock
    private AssignmentRuleMapper ruleMapper;
    @Mock
    private EnterpriseMapper enterpriseMapper;
    @Mock
    private MailboxMapper mailboxMapper;
    @Mock
    private OperationLogMapper operationLogMapper;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private AssignmentRuleGroupService groupService;

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "系统管理员", "admin@example.com", "ADMIN");

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "AssignmentRuleGroupServiceTest"),
                AssignmentRuleGroupEntity.class);
    }

    @BeforeEach
    void setUp() {
        lenient().when(groupMapper.selectCount(any())).thenReturn(0L);
        lenient().when(ruleMapper.selectCount(any())).thenReturn(0L);
        lenient().when(mailboxMapper.selectCount(any())).thenReturn(0L);
        lenient().when(enterpriseMapper.selectById(1L)).thenReturn(enterprise(1L, true));
    }

    @Test
    void createGroup_shouldPersistEnterpriseOwnership() {
        when(groupMapper.insert(any())).thenAnswer(invocation -> {
            AssignmentRuleGroupEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        });
        when(groupMapper.selectById(100L)).thenReturn(group(100L, 1L, "售后规则组", true));

        AssignmentRuleGroupVO result = groupService.createGroup(admin, request(1L, " 售后规则组 "));

        assertEquals(1L, result.enterpriseId());
        assertEquals("售后规则组", result.groupName());
        verify(groupMapper).insert(any());
    }

    @Test
    void createGroup_whenEnterpriseDisabled_shouldReject() {
        when(enterpriseMapper.selectById(2L)).thenReturn(enterprise(2L, false));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> groupService.createGroup(admin, request(2L, "停用企业规则组")));

        assertTrue(exception.getMessage().contains("所属企业已停用"));
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void deleteGroup_whenMailboxReferenced_shouldReject() {
        when(groupMapper.selectById(100L)).thenReturn(group(100L, 1L, "规则组", true));
        when(mailboxMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> groupService.deleteGroup(admin, 100L));

        assertTrue(exception.getMessage().contains("已被邮箱引用"));
        verify(groupMapper, never()).deleteById(100L);
    }

    private AssignmentRuleGroupSaveRequest request(Long enterpriseId, String name) {
        AssignmentRuleGroupSaveRequest request = new AssignmentRuleGroupSaveRequest();
        request.setEnterpriseId(enterpriseId);
        request.setGroupName(name);
        request.setEnabled(true);
        return request;
    }

    private AssignmentRuleGroupEntity group(Long id, Long enterpriseId, String name, boolean enabled) {
        AssignmentRuleGroupEntity entity = new AssignmentRuleGroupEntity();
        entity.setId(id);
        entity.setEnterpriseId(enterpriseId);
        entity.setGroupName(name);
        entity.setEnabled(enabled);
        return entity;
    }

    private EnterpriseEntity enterprise(Long id, boolean enabled) {
        EnterpriseEntity entity = new EnterpriseEntity();
        entity.setId(id);
        entity.setEnabled(enabled);
        return entity;
    }
}
