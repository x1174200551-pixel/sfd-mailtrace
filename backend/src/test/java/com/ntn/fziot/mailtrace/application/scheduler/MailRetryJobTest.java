package com.ntn.fziot.mailtrace.application.scheduler;

import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailDeliveryStateService;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.application.bizservice.ticket.TicketReplyDeliveryService;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailSendLogMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailRetryJobTest {

    @Test
    void retryFailedMails_shouldContinueAfterSingleTaskException() {
        MailSendLogMapper sendLogMapper = mock(MailSendLogMapper.class);
        MailSendService sendService = mock(MailSendService.class);
        MailDeliveryStateService stateService = mock(MailDeliveryStateService.class);
        TicketReplyDeliveryService replyDeliveryService = mock(TicketReplyDeliveryService.class);
        MailRetryJob job = new MailRetryJob(
                sendLogMapper, sendService, stateService, replyDeliveryService);

        MailSendLogEntity first = new MailSendLogEntity();
        first.setId(1L);
        MailSendLogEntity second = new MailSendLogEntity();
        second.setId(2L);
        when(sendLogMapper.selectReplyCompletionPending(20)).thenReturn(List.of());
        when(sendLogMapper.selectPendingForDispatch(any(), org.mockito.ArgumentMatchers.eq(20))).thenReturn(List.of());
        when(sendLogMapper.selectFailedForRetry(20)).thenReturn(List.of(first, second));
        when(sendService.retrySend(1L)).thenThrow(new IllegalStateException("mailbox disabled"));
        when(sendService.retrySend(2L)).thenReturn(MailSendService.SendResult.ok("ok"));

        job.retryFailedMails();

        verify(stateService).markStaleUnknown(any());
        verify(sendService).retrySend(1L);
        verify(sendService).retrySend(2L);
    }
}
