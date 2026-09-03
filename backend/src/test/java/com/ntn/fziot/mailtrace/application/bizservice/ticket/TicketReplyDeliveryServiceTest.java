package com.ntn.fziot.mailtrace.application.bizservice.ticket;

import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEventEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketMessageEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailSendLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketEventMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMessageMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.ntn.fziot.mailtrace.application.event.SlaMilestoneCompletedEvent;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketReplyDeliveryServiceTest {

    @Test
    void completeBySendLog_shouldUpdateTicketAndRemainIdempotent() {
        MailSendLogMapper sendLogMapper = mock(MailSendLogMapper.class);
        TicketMessageMapper messageMapper = mock(TicketMessageMapper.class);
        TicketMapper ticketMapper = mock(TicketMapper.class);
        TicketEventMapper eventMapper = mock(TicketEventMapper.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        TicketReplyDeliveryService service = new TicketReplyDeliveryService(
                sendLogMapper, messageMapper, ticketMapper, eventMapper, eventPublisher);

        LocalDateTime sentAt = LocalDateTime.parse("2026-09-01T09:30:00");
        MailSendLogEntity sendLog = new MailSendLogEntity();
        sendLog.setId(10L);
        sendLog.setTicketId(100L);
        sendLog.setTicketMessageId(200L);
        sendLog.setSendType("AGENT_REPLY");
        sendLog.setSendStatus("SUCCESS");
        sendLog.setSentAt(sentAt);
        TicketMessageEntity message = new TicketMessageEntity();
        message.setId(200L);
        message.setTicketId(100L);
        message.setSendStatus("SUCCESS");
        message.setContentText("已处理完成");
        message.setCreatedBy("agent");
        TicketEntity ticket = new TicketEntity();
        ticket.setId(100L);
        ticket.setStatus(TicketBizService.STATUS_PROCESSING);

        when(sendLogMapper.selectById(10L)).thenReturn(sendLog);
        when(messageMapper.selectByIdForUpdate(200L)).thenReturn(message);
        when(ticketMapper.selectByIdForUpdate(100L)).thenReturn(ticket);
        when(ticketMapper.updateById(any())).thenReturn(1);
        when(messageMapper.updateById(any())).thenReturn(1);

        assertTrue(service.completeBySendLog(10L));
        assertFalse(service.completeBySendLog(10L));
        assertEquals(TicketBizService.STATUS_WAITING_CUSTOMER, ticket.getStatus());
        assertEquals(sentAt, ticket.getFirstReplyAt());
        assertEquals(sentAt, ticket.getLastAgentReplyAt());
        assertNotNull(message.getDeliveryCompletedAt());

        ArgumentCaptor<TicketEventEntity> eventCaptor = ArgumentCaptor.forClass(TicketEventEntity.class);
        verify(eventMapper, times(2)).insert(eventCaptor.capture());
        assertEquals(TicketBizService.EVENT_FIRST_REPLY, eventCaptor.getAllValues().get(0).getEventType());
        assertEquals(TicketBizService.EVENT_AGENT_REPLY, eventCaptor.getAllValues().get(1).getEventType());
        verify(ticketMapper).updateById(ticket);
        verify(eventPublisher).publishEvent(any(SlaMilestoneCompletedEvent.class));
    }
}
