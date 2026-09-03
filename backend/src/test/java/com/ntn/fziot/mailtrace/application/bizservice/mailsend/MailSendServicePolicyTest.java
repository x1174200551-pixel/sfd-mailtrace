package com.ntn.fziot.mailtrace.application.bizservice.mailsend;

import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.application.bizservice.sla.SlaNotificationPolicyService;
import com.ntn.fziot.mailtrace.application.bizservice.ticket.TicketReplyDeliveryService;
import com.ntn.fziot.mailtrace.infrastructure.crypto.MailPasswordCipher;
import com.ntn.fziot.mailtrace.infrastructure.storage.FileStorageService;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailSendLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketAttachmentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailSendServicePolicyTest {

    @Mock private MailboxMapper mailboxMapper;
    @Mock private MailSendLogMapper mailSendLogMapper;
    @Mock private TicketAttachmentMapper ticketAttachmentMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private MailPasswordCipher mailPasswordCipher;
    @Mock private PermissionService permissionService;
    @Mock private EnterpriseMailboxAccessService enterpriseMailboxAccessService;
    @Mock private MailDeliveryStateService deliveryStateService;
    @Mock private TicketReplyDeliveryService ticketReplyDeliveryService;
    @Mock private SlaNotificationPolicyService slaNotificationPolicyService;

    @InjectMocks private MailSendService service;

    @Test
    void dispatchPending_whenSlaNodeDisabled_shouldCancelBeforeSmtpSetup() {
        MailSendLogEntity log = new MailSendLogEntity();
        log.setId(1L);
        log.setTicketId(10L);
        log.setMailboxId(11L);
        log.setSendType(SlaNotificationPolicyService.RESOLVE_ESCALATION);
        log.setMessageId("message@example.com");
        when(deliveryStateService.claimInitial(1L)).thenReturn(true);
        when(deliveryStateService.reload(1L)).thenReturn(log);
        when(slaNotificationPolicyService.isDeliveryEnabled(10L, log.getSendType())).thenReturn(false);
        when(deliveryStateService.markCancelled(1L, "SLA通知节点已关闭或工单禁止通知")).thenReturn(true);

        MailSendService.SendResult result = service.dispatchPending(1L);

        assertFalse(result.success());
        assertEquals(MailSendService.DeliveryStatus.FAILED, result.deliveryStatus());
        verify(mailboxMapper, never()).selectById(11L);
    }
}
