package com.ntn.fziot.mailtrace.application.bizservice.template;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.template.NotificationTemplateCreateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.template.NotificationTemplateVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.NotificationTemplateEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class NotificationTemplateServiceTest {

    @Mock
    private NotificationTemplateMapper templateMapper;
    @Mock
    private MailboxMapper mailboxMapper;
    @Mock
    private OperationLogMapper operationLogMapper;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private NotificationTemplateService templateService;

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "系统管理员", "admin@example.com", "ADMIN");

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "NotificationTemplateServiceTest.Template"),
                NotificationTemplateEntity.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "NotificationTemplateServiceTest.Mailbox"),
                MailboxEntity.class);
    }

    @BeforeEach
    void setUp() {
        lenient().when(templateMapper.selectCount(any())).thenReturn(0L);
        lenient().when(mailboxMapper.selectCount(any())).thenReturn(0L);
    }

    @Test
    void createTemplate_shouldPersistGlobalTemplateAndType() {
        when(templateMapper.insert(any())).thenAnswer(invocation -> {
            NotificationTemplateEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        });
        when(templateMapper.selectById(100L)).thenReturn(template(100L, "AUTO_REPLY_STD", "AUTO_REPLY"));

        NotificationTemplateVO result = templateService.createTemplate(admin, request());

        ArgumentCaptor<NotificationTemplateEntity> captor = ArgumentCaptor.forClass(NotificationTemplateEntity.class);
        verify(templateMapper).insert(captor.capture());
        assertEquals("AUTO_REPLY", captor.getValue().getTemplateType());
        assertEquals("AUTO_REPLY_STD", result.templateCode());
    }

    @Test
    void createTemplate_whenCodeAlreadyExists_shouldReject() {
        when(templateMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> templateService.createTemplate(admin, request()));

        assertTrue(exception.getMessage().contains("模板编码已存在"));
        verify(templateMapper, never()).insert(any());
    }

    @Test
    void createAgentReplyTemplate_withoutReplyContentVariable_shouldReject() {
        NotificationTemplateCreateRequest request = request();
        request.setTemplateType("AGENT_REPLY");
        request.setTemplateCode("AGENT_REPLY_CUSTOM");
        request.setContentTpl("您好，您的问题已经处理完成。");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> templateService.createTemplate(admin, request));

        assertTrue(exception.getMessage().contains("必须包含 {reply_content}"));
        verify(templateMapper, never()).insert(any());
    }

    @Test
    void deleteTemplate_whenMailboxReferenced_shouldReject() {
        when(templateMapper.selectById(100L)).thenReturn(template(100L, "AUTO_REPLY_STD", "AUTO_REPLY"));
        when(mailboxMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> templateService.deleteTemplate(admin, 100L));

        assertTrue(exception.getMessage().contains("已被邮箱引用"));
        verify(templateMapper, never()).deleteById(100L);
    }

    @Test
    void createSlaTemplate_shouldAcceptStageAndOverdueHourVariables() {
        NotificationTemplateCreateRequest request = request();
        request.setTemplateType("SLA_BREACH");
        request.setTemplateCode("SLA_BREACH_STAGE");
        request.setContentTpl("{sla_stage}{sla_action}，当前截止：{sla_deadline}，响应：{sla_response_deadline}，"
                + "解决：{sla_resolve_deadline}，触发：{sla_triggered_at}，已超时{sla_overdue_hours}个工作小时");
        when(templateMapper.insert(any())).thenAnswer(invocation -> {
            NotificationTemplateEntity entity = invocation.getArgument(0);
            entity.setId(101L);
            return 1;
        });
        when(templateMapper.selectById(101L)).thenReturn(template(101L, "SLA_BREACH_STAGE", "SLA_BREACH"));

        NotificationTemplateVO result = templateService.createTemplate(admin, request);

        assertEquals("SLA_BREACH", result.templateType());
        verify(templateMapper).insert(any());
    }

    private NotificationTemplateCreateRequest request() {
        NotificationTemplateCreateRequest request = new NotificationTemplateCreateRequest();
        request.setTemplateType("AUTO_REPLY");
        request.setTemplateCode("AUTO_REPLY_STD");
        request.setTemplateName("标准自动回复");
        request.setSubjectTpl("工单 {ticket_no} 已受理");
        request.setContentTpl("您好，{subject} 已受理");
        request.setEnabled(true);
        return request;
    }

    private NotificationTemplateEntity template(Long id, String code, String type) {
        NotificationTemplateEntity entity = new NotificationTemplateEntity();
        entity.setId(id);
        entity.setTemplateCode(code);
        entity.setTemplateType(type);
        entity.setTemplateName("模板");
        entity.setSubjectTpl("主题");
        entity.setContentTpl("正文");
        entity.setEnabled(true);
        return entity;
    }

}
