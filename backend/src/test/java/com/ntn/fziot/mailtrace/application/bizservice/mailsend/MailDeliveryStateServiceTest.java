package com.ntn.fziot.mailtrace.application.bizservice.mailsend;

import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailSendLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMessageMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailDeliveryStateServiceTest {

    @Test
    void claimRetry_shouldAllowOnlySuccessfulDatabaseClaim() {
        MailSendLogMapper sendLogMapper = mock(MailSendLogMapper.class);
        TicketMessageMapper messageMapper = mock(TicketMessageMapper.class);
        MailDeliveryStateService service = new MailDeliveryStateService(sendLogMapper, messageMapper);
        when(sendLogMapper.claimFailedForRetry(10L)).thenReturn(1, 0);

        assertTrue(service.claimRetry(10L));
        assertFalse(service.claimRetry(10L));
        verify(sendLogMapper, org.mockito.Mockito.times(2)).claimFailedForRetry(10L);
    }
}
