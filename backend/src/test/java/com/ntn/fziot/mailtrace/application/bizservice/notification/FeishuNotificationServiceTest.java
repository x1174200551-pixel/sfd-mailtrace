package com.ntn.fziot.mailtrace.application.bizservice.notification;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.infrastructure.feishu.FeishuGroupBotClient;
import com.ntn.fziot.mailtrace.infrastructure.feishu.FeishuGroupBotProperties;
import com.ntn.fziot.mailtrace.infrastructure.feishu.FeishuGroupCardRenderer;
import com.ntn.fziot.mailtrace.application.event.FeishuTaskCreatedEvent;
import com.ntn.fziot.mailtrace.application.bizservice.sla.SlaNotificationPolicyService;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.FeishuSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.FeishuSendLogMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeishuNotificationServiceTest {

    @Mock
    private FeishuGroupBotProperties properties;
    @Mock
    private FeishuGroupCardRenderer cardRenderer;
    @Mock
    private FeishuGroupBotClient client;
    @Mock
    private EnterpriseMapper enterpriseMapper;
    @Mock
    private FeishuSendLogMapper sendLogMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SlaNotificationPolicyService slaNotificationPolicyService;

    @InjectMocks
    private FeishuNotificationService service;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                configuration, "FeishuNotificationServiceTest.EnterpriseEntity");
        TableInfoHelper.initTableInfo(assistant, EnterpriseEntity.class);
    }

    @BeforeEach
    void allowDeliveryByDefault() {
        lenient().when(slaNotificationPolicyService.isDeliveryEnabled(any(), any())).thenReturn(true);
    }

    @Test
    void enqueue_whenGlobalSwitchDisabled_shouldSkipWithoutQueryingEnterprise() {
        when(properties.isEnabled()).thenReturn(false);

        assertNull(service.enqueue(10L, ticket(), 2L, "处理人", "ASSIGN_NOTIFY", 20L, "标题", "正文"));

        verify(enterpriseMapper, never()).selectById(any());
        verify(sendLogMapper, never()).insert(any());
    }

    @Test
    void enqueue_whenAssigneeMissing_shouldStillPersistGroupTask() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getMaxRetry()).thenReturn(5);
        when(enterpriseMapper.selectById(1L)).thenReturn(enterprise(1));
        when(cardRenderer.render("SLA_BREACH", 100L, "TCK-100", "HIGH", "标题", "正文", "未分配"))
                .thenReturn(new FeishuGroupCardRenderer.RenderedCard("标题", "正文", "{\"elements\":[]}"));
        when(sendLogMapper.insert(any())).thenAnswer(invocation -> {
            FeishuSendLogEntity log = invocation.getArgument(0);
            log.setId(98L);
            return 1;
        });

        Long logId = service.enqueue(10L, ticket(), null, "未分配", "SLA_BREACH", 20L, "标题", "正文");

        assertEquals(98L, logId);
        verify(sendLogMapper).insert(any());
    }

    @Test
    void enqueue_whenEnterpriseConnectionStatusIsNotOk_shouldStillPersistTask() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getMaxRetry()).thenReturn(5);
        EnterpriseEntity enterprise = enterprise(1);
        enterprise.setFeishuConnectionStatus("ERROR");
        when(enterpriseMapper.selectById(1L)).thenReturn(enterprise);
        when(cardRenderer.render("ASSIGN_NOTIFY", 100L, "TCK-100", "HIGH", "标题", "正文", "处理人"))
                .thenReturn(new FeishuGroupCardRenderer.RenderedCard("标题", "正文", "{\"elements\":[]}"));
        when(sendLogMapper.insert(any())).thenAnswer(invocation -> {
            FeishuSendLogEntity log = invocation.getArgument(0);
            log.setId(99L);
            return 1;
        });

        Long logId = service.enqueue(10L, ticket(), 2L, "处理人", "ASSIGN_NOTIFY", 20L, "标题", "正文");

        assertEquals(99L, logId);
        verify(sendLogMapper).insert(any());
    }

    @Test
    void enqueue_shouldPersistTaskForTicketEnterpriseOnly() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getMaxRetry()).thenReturn(5);
        when(enterpriseMapper.selectById(1L)).thenReturn(enterprise(1));
        when(cardRenderer.render("ASSIGN_NOTIFY", 100L, "TCK-100", "HIGH", "标题", "正文", "处理人"))
                .thenReturn(new FeishuGroupCardRenderer.RenderedCard("标题", "正文", "{\"elements\":[]}"));
        when(sendLogMapper.insert(any())).thenAnswer(invocation -> {
            FeishuSendLogEntity log = invocation.getArgument(0);
            log.setId(99L);
            return 1;
        });

        Long logId = service.enqueue(10L, ticket(), 2L, "处理人", "ASSIGN_NOTIFY", 20L, "标题", "正文");

        ArgumentCaptor<FeishuSendLogEntity> captor = ArgumentCaptor.forClass(FeishuSendLogEntity.class);
        verify(sendLogMapper).insert(captor.capture());
        assertEquals(99L, logId);
        assertEquals(1L, captor.getValue().getEnterpriseId());
        assertEquals("PENDING", captor.getValue().getSendStatus());
        assertEquals(2L, captor.getValue().getAssigneeUserId());
        verify(eventPublisher).publishEvent(any(FeishuTaskCreatedEvent.class));
    }

    @Test
    void sendTest_shouldSendPlainGroupMessageWithoutUserSelection() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getMaxRetry()).thenReturn(5);
        when(enterpriseMapper.selectById(1L)).thenReturn(enterprise(1));
        when(cardRenderer.render("TEST", null, null, null,
                "MailTrace 飞书群通知测试", "这是一条企业通知群配置测试消息，请在群内确认是否正常显示。", null))
                .thenReturn(new FeishuGroupCardRenderer.RenderedCard("测试", "测试正文", "{\"elements\":[]}"));
        FeishuSendLogEntity[] persisted = new FeishuSendLogEntity[1];
        when(sendLogMapper.insert(any())).thenAnswer(invocation -> {
            persisted[0] = invocation.getArgument(0);
            persisted[0].setId(77L);
            return 1;
        });
        when(sendLogMapper.claimForSend(eq(77L), any(LocalDateTime.class))).thenReturn(1);
        when(sendLogMapper.selectById(77L)).thenAnswer(invocation -> persisted[0]);
        when(client.send(any(), any(), any())).thenReturn(FeishuGroupBotClient.SendResult.ok("0", "success"));

        var response = service.sendTest(1L);

        assertTrue(response.accepted());
        assertEquals(77L, response.sendLogId());
        assertNull(persisted[0].getAssigneeUserId());
        verify(client).send(any(), any(), any());
    }

    @Test
    void processPendingBatch_whenEnterpriseConfigVersionChanged_shouldNotSendOldTask() {
        when(properties.isEnabled()).thenReturn(true);
        FeishuSendLogEntity task = new FeishuSendLogEntity();
        task.setId(99L);
        task.setEnterpriseId(1L);
        task.setEnterpriseConfigVersion(1);
        task.setSendType("ASSIGN_NOTIFY");
        when(sendLogMapper.selectPendingForSend(any(LocalDateTime.class), anyInt())).thenReturn(List.of(task));
        when(sendLogMapper.claimForSend(eq(99L), any(LocalDateTime.class))).thenReturn(1);
        when(sendLogMapper.selectById(99L)).thenReturn(task);
        when(enterpriseMapper.selectById(1L)).thenReturn(enterprise(2));

        FeishuNotificationService.BatchResult result = service.processPendingBatch();

        assertEquals(1, result.failed());
        verify(client, never()).send(any(), any(), any());
        ArgumentCaptor<FeishuSendLogEntity> captor = ArgumentCaptor.forClass(FeishuSendLogEntity.class);
        verify(sendLogMapper).updateById(captor.capture());
        assertEquals("FINAL_FAILED", captor.getValue().getSendStatus());
        assertEquals("CONFIG_CHANGED", captor.getValue().getResponseCode());
        assertTrue(captor.getValue().getResponseMessage().contains("配置已变更"));
    }

    @Test
    void processPendingBatch_whenEnterpriseConnectionStatusIsNotOk_shouldStillSend() {
        when(properties.isEnabled()).thenReturn(true);
        FeishuSendLogEntity task = new FeishuSendLogEntity();
        task.setId(99L);
        task.setEnterpriseId(1L);
        task.setEnterpriseConfigVersion(1);
        task.setSendType("ASSIGN_NOTIFY");
        EnterpriseEntity enterprise = enterprise(1);
        enterprise.setFeishuConnectionStatus("ERROR");
        when(sendLogMapper.selectPendingForSend(any(LocalDateTime.class), anyInt())).thenReturn(List.of(task));
        when(sendLogMapper.claimForSend(eq(99L), any(LocalDateTime.class))).thenReturn(1);
        when(sendLogMapper.selectById(99L)).thenReturn(task);
        when(enterpriseMapper.selectById(1L)).thenReturn(enterprise);
        when(client.send(any(), any(), any())).thenReturn(FeishuGroupBotClient.SendResult.ok("0", "success"));

        FeishuNotificationService.BatchResult result = service.processPendingBatch();

        assertEquals(1, result.success());
        verify(client).send(any(), any(), any());
        ArgumentCaptor<FeishuSendLogEntity> captor = ArgumentCaptor.forClass(FeishuSendLogEntity.class);
        verify(sendLogMapper).updateById(captor.capture());
        assertEquals("SUCCESS", captor.getValue().getSendStatus());
        assertEquals("0", captor.getValue().getResponseCode());
    }

    @Test
    void processPendingBatch_whenSlaNodeDisabled_shouldCancelWithoutCallingFeishu() {
        when(properties.isEnabled()).thenReturn(true);
        FeishuSendLogEntity task = new FeishuSendLogEntity();
        task.setId(100L);
        task.setTicketId(10L);
        task.setEnterpriseId(1L);
        task.setEnterpriseConfigVersion(1);
        task.setSendType(SlaNotificationPolicyService.RESOLVE_ESCALATION);
        when(sendLogMapper.selectPendingForSend(any(LocalDateTime.class), anyInt())).thenReturn(List.of(task));
        when(sendLogMapper.claimForSend(eq(100L), any(LocalDateTime.class))).thenReturn(1);
        when(sendLogMapper.selectById(100L)).thenReturn(task);
        when(enterpriseMapper.selectById(1L)).thenReturn(enterprise(1));
        when(slaNotificationPolicyService.isDeliveryEnabled(10L, task.getSendType())).thenReturn(false);

        FeishuNotificationService.BatchResult result = service.processPendingBatch();

        assertEquals(1, result.failed());
        verify(client, never()).send(any(), any(), any());
        ArgumentCaptor<FeishuSendLogEntity> captor = ArgumentCaptor.forClass(FeishuSendLogEntity.class);
        verify(sendLogMapper).updateById(captor.capture());
        assertEquals("CANCELLED", captor.getValue().getSendStatus());
        assertEquals("SLA_POLICY_DISABLED", captor.getValue().getResponseCode());
    }

    @Test
    void processPendingBatch_shouldUseSameApplicationTimeForRecoveryAndDueQuery() {
        when(properties.isEnabled()).thenReturn(true);
        when(sendLogMapper.selectPendingForSend(any(LocalDateTime.class), eq(20))).thenReturn(List.of());

        FeishuNotificationService.BatchResult result = service.processPendingBatch();

        assertEquals(0, result.total());
        ArgumentCaptor<LocalDateTime> recoveryTime = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> queryTime = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(sendLogMapper).recoverStaleSending(recoveryTime.capture());
        verify(sendLogMapper).selectPendingForSend(queryTime.capture(), eq(20));
        assertEquals(recoveryTime.getValue(), queryTime.getValue());
    }

    private TicketEntity ticket() {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(100L);
        ticket.setTicketNo("TCK-100");
        ticket.setEnterpriseId(1L);
        ticket.setMailboxId(11L);
        ticket.setPriority("HIGH");
        return ticket;
    }

    private EnterpriseEntity enterprise(int configVersion) {
        EnterpriseEntity enterprise = new EnterpriseEntity();
        enterprise.setId(1L);
        enterprise.setEnabled(true);
        enterprise.setFeishuNotifyEnabled(true);
        enterprise.setFeishuGroupName("企业通知群");
        enterprise.setFeishuWebhookUrl("https://open.feishu.cn/open-apis/bot/v2/hook/test");
        enterprise.setFeishuSigningSecret("secret");
        enterprise.setFeishuConfigVersion(configVersion);
        enterprise.setFeishuConnectionStatus("OK");
        return enterprise;
    }
}
