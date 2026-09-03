package com.ntn.fziot.mailtrace.application.bizservice.sla;

import com.ntn.fziot.mailtrace.repox.mysql.entity.SlaPolicyEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.SlaPolicyMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SlaNotificationPolicyServiceTest {

    private final TicketMapper ticketMapper = mock(TicketMapper.class);
    private final SlaPolicyMapper slaPolicyMapper = mock(SlaPolicyMapper.class);
    private final SlaNotificationPolicyService service =
            new SlaNotificationPolicyService(ticketMapper, slaPolicyMapper);

    @Test
    void resolve_whenPolicyMissing_shouldEnableOnlyFourDefaultNodes() {
        SlaNotificationPolicyService.NotificationSettings settings = service.resolve(null);

        assertTrue(settings.responseWarning());
        assertTrue(settings.responseBreach());
        assertFalse(settings.responseEscalation());
        assertTrue(settings.resolveWarning());
        assertTrue(settings.resolveBreach());
        assertFalse(settings.resolveEscalation());
    }

    @Test
    void isDeliveryEnabled_shouldUseCurrentPolicyAndRespectHistorySuppression() {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(10L);
        ticket.setSlaPolicyId(20L);
        ticket.setSlaNotificationSuppressed(false);
        SlaPolicyEntity policy = new SlaPolicyEntity();
        policy.setResponseWarningNotifyEnabled(false);
        policy.setResponseBreachNotifyEnabled(true);
        when(ticketMapper.selectById(10L)).thenReturn(ticket);
        when(slaPolicyMapper.selectById(20L)).thenReturn(policy);

        assertFalse(service.isDeliveryEnabled(10L, SlaNotificationPolicyService.RESPONSE_WARNING));
        assertTrue(service.isDeliveryEnabled(10L, SlaNotificationPolicyService.RESPONSE_BREACH));

        ticket.setSlaNotificationSuppressed(true);
        assertFalse(service.isDeliveryEnabled(10L, SlaNotificationPolicyService.RESPONSE_BREACH));
    }
}
