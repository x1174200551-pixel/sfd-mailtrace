package com.ntn.fziot.mailtrace.application.bizservice.mailbox;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.crypto.MailPasswordCipher;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxSaveRequest;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailFetchLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.NotificationTemplateEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.AssignmentRuleGroupMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailFetchLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.SlaPolicyMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailboxServiceTest {

    @Mock
    private MailboxMapper mailboxMapper;
    @Mock
    private MailFetchLogMapper mailFetchLogMapper;
    @Mock
    private EnterpriseMapper enterpriseMapper;
    @Mock
    private NotificationTemplateMapper notificationTemplateMapper;
    @Mock
    private SlaPolicyMapper slaPolicyMapper;
    @Mock
    private AssignmentRuleGroupMapper assignmentRuleGroupMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private OperationLogMapper operationLogMapper;
    @Mock
    private MailPasswordCipher mailPasswordCipher;
    @Mock
    private PermissionService permissionService;
    @Mock
    private EnterpriseMailboxAccessService enterpriseMailboxAccessService;

    private MailboxService mailboxService;

    private final CurrentUserPrincipal operator = new CurrentUserPrincipal(
            8L, "mail_user", "邮箱用户", "mail_user@example.com", "CUSTOM");

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "MailboxServiceTest.MailboxEntity");
        TableInfoHelper.initTableInfo(assistant, MailboxEntity.class);
        mailboxService = new MailboxService(
                mailboxMapper,
                mailFetchLogMapper,
                enterpriseMapper,
                notificationTemplateMapper,
                slaPolicyMapper,
                assignmentRuleGroupMapper,
                userMapper,
                operationLogMapper,
                mailPasswordCipher,
                permissionService,
                enterpriseMailboxAccessService
        );
    }

    @Test
    void pageMailboxes_whenOnlyMailboxMenuPermission_shouldRejectRead() {
        doThrow(new BusinessException(40302, "无权查看邮箱配置"))
                .when(permissionService).assertPermission(operator, "mailbox:read", "无权查看邮箱配置");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> mailboxService.pageMailboxes(operator, null, null, "ALL", null, 1, 10));

        assertEquals(40302, ex.getCode());
        verify(mailboxMapper, never()).selectPage(any(), any());
    }

    @Test
    void pageMailboxes_whenGrantEmpty_shouldUseExplicitDenyAllAndZeroSummary() {
        when(enterpriseMailboxAccessService.resolveReadableMailboxIds(operator)).thenReturn(Set.of());
        Page<MailboxEntity> page = Page.of(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(mailboxMapper.selectPage(any(), any())).thenReturn(page);

        MailboxPageResponse response = mailboxService.pageMailboxes(operator, null, null, "ALL", null, 1, 10);

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MailboxEntity>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(mailboxMapper).selectPage(any(), captor.capture());
        assertEquals(true, captor.getValue().getSqlSegment().contains("1 = 0"));
        assertEquals(0, response.summary().totalMailboxes());
        verify(mailboxMapper, never()).selectCount(any());
    }

    @Test
    void pageMailboxes_shouldReturnActualTodayMailAndTicketCounts() {
        when(enterpriseMailboxAccessService.resolveReadableMailboxIds(operator)).thenReturn(Set.of(11L));
        Page<MailboxEntity> page = Page.of(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(mailboxMapper.selectPage(any(), any())).thenReturn(page);
        when(mailboxMapper.selectCount(any())).thenReturn(1L);

        MailFetchLogEntity firstLog = new MailFetchLogEntity();
        firstLog.setFetchedCount(12);
        firstLog.setCreatedTicketCount(10);
        MailFetchLogEntity secondLog = new MailFetchLogEntity();
        secondLog.setFetchedCount(8);
        secondLog.setCreatedTicketCount(7);
        when(mailFetchLogMapper.selectList(any())).thenReturn(List.of(firstLog, secondLog));

        MailboxPageResponse response = mailboxService.pageMailboxes(
                operator, null, null, "ALL", null, 1, 10);

        assertEquals(20, response.summary().todayReceivedMailCount());
        assertEquals(17, response.summary().todayCreatedTicketCount());
    }

    @Test
    void createMailbox_whenTemplateTypeDoesNotMatch_shouldReject() {
        when(mailboxMapper.selectCount(any())).thenReturn(0L);
        EnterpriseEntity enterprise = new EnterpriseEntity();
        enterprise.setId(1L);
        enterprise.setEnabled(true);
        when(enterpriseMapper.selectById(1L)).thenReturn(enterprise);
        NotificationTemplateEntity template = new NotificationTemplateEntity();
        template.setId(20L);
        template.setTemplateType("SLA_WARNING");
        template.setEnabled(true);
        when(notificationTemplateMapper.selectById(20L)).thenReturn(template);

        MailboxSaveRequest request = new MailboxSaveRequest();
        request.setEnterpriseId(1L);
        request.setEmailAddress("support@example.com");
        request.setAutoReplyTemplateId(20L);
        request.setAssignmentNotifyTemplateId(21L);
        request.setAgentReplyTemplateId(22L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> mailboxService.createMailbox(operator, request));

        assertTrue(exception.getMessage().contains("自动回复模板类型不匹配"));
        verify(mailboxMapper, never()).insert(any());
    }

    @Test
    void createMailbox_whenAutoReplyEnabledWithoutTemplate_shouldReject() {
        when(mailboxMapper.selectCount(any())).thenReturn(0L);
        EnterpriseEntity enterprise = new EnterpriseEntity();
        enterprise.setId(1L);
        enterprise.setEnabled(true);
        when(enterpriseMapper.selectById(1L)).thenReturn(enterprise);

        MailboxSaveRequest request = new MailboxSaveRequest();
        request.setEnterpriseId(1L);
        request.setEmailAddress("support@example.com");
        request.setAutoReplyEnabled(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> mailboxService.createMailbox(operator, request));

        assertTrue(exception.getMessage().contains("必须选择自动回复模板"));
        verify(mailboxMapper, never()).insert(any());
    }

    @Test
    void createMailbox_shouldPersistEnterpriseStrategyFields() {
        when(mailboxMapper.selectCount(any())).thenReturn(0L);
        EnterpriseEntity enterprise = new EnterpriseEntity();
        enterprise.setId(1L);
        enterprise.setEnterpriseName("示例企业");
        enterprise.setEnabled(true);
        when(enterpriseMapper.selectById(1L)).thenReturn(enterprise);
        when(mailPasswordCipher.encrypt(any())).thenReturn("encrypted");
        when(mailboxMapper.insert(any())).thenAnswer(invocation -> {
            MailboxEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        });
        MailboxEntity saved = new MailboxEntity();
        saved.setId(100L);
        saved.setEnterpriseId(1L);
        saved.setMailboxName("客服邮箱");
        saved.setEmailAddress("support@example.com");
        saved.setEnabled(true);
        saved.setSlaPolicyId(30L);
        saved.setAssignmentRuleGroupId(40L);
        saved.setAssignmentFallbackType("NONE");
        when(mailboxMapper.selectById(100L)).thenReturn(saved);
        com.ntn.fziot.mailtrace.repox.mysql.entity.SlaPolicyEntity policy =
                new com.ntn.fziot.mailtrace.repox.mysql.entity.SlaPolicyEntity();
        policy.setId(30L);
        policy.setEnterpriseId(1L);
        policy.setEnabled(true);
        when(slaPolicyMapper.selectById(30L)).thenReturn(policy);
        com.ntn.fziot.mailtrace.repox.mysql.entity.AssignmentRuleGroupEntity group =
                new com.ntn.fziot.mailtrace.repox.mysql.entity.AssignmentRuleGroupEntity();
        group.setId(40L);
        group.setEnterpriseId(1L);
        group.setEnabled(true);
        when(assignmentRuleGroupMapper.selectById(40L)).thenReturn(group);
        when(notificationTemplateMapper.selectById(21L)).thenReturn(template(21L, "ASSIGN_NOTIFY"));
        when(notificationTemplateMapper.selectById(22L)).thenReturn(template(22L, "AGENT_REPLY"));
        when(notificationTemplateMapper.selectById(23L)).thenReturn(template(23L, "SLA_WARNING"));
        when(notificationTemplateMapper.selectById(24L)).thenReturn(template(24L, "SLA_BREACH"));

        MailboxSaveRequest request = new MailboxSaveRequest();
        request.setEnterpriseId(1L);
        request.setMailboxName("客服邮箱");
        request.setEmailAddress("support@example.com");
        request.setImapPassword("imap-secret");
        request.setSmtpPassword("smtp-secret");
        request.setSlaPolicyId(30L);
        request.setAssignmentNotifyTemplateId(21L);
        request.setAgentReplyTemplateId(22L);
        request.setSlaWarningTemplateId(23L);
        request.setSlaBreachTemplateId(24L);
        request.setAssignmentRuleGroupId(40L);
        request.setAssignmentFallbackType("NONE");

        mailboxService.createMailbox(operator, request);

        ArgumentCaptor<MailboxEntity> captor = ArgumentCaptor.forClass(MailboxEntity.class);
        verify(mailboxMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getEnterpriseId());
        assertEquals(30L, captor.getValue().getSlaPolicyId());
        assertEquals(40L, captor.getValue().getAssignmentRuleGroupId());
        assertEquals("NONE", captor.getValue().getAssignmentFallbackType());
    }

    private NotificationTemplateEntity template(Long id, String type) {
        NotificationTemplateEntity template = new NotificationTemplateEntity();
        template.setId(id);
        template.setTemplateType(type);
        template.setEnabled(true);
        return template;
    }
}
