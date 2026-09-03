package com.ntn.fziot.mailtrace.application.bizservice.ticket;

import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEventEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketMessageEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailSendLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketEventMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMessageMapper;
import com.ntn.fziot.mailtrace.application.event.SlaMilestoneCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** SMTP 成功后，以幂等方式完成客服回复对应的工单状态与事件。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketReplyDeliveryService {

    private static final String AGENT_REPLY = "AGENT_REPLY";
    private static final String SUCCESS = "SUCCESS";
    private static final String SYSTEM = "SYSTEM";

    private final MailSendLogMapper mailSendLogMapper;
    private final TicketMessageMapper ticketMessageMapper;
    private final TicketMapper ticketMapper;
    private final TicketEventMapper ticketEventMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean completeBySendLog(Long sendLogId) {
        MailSendLogEntity sendLog = mailSendLogMapper.selectById(sendLogId);
        if (sendLog == null || !SUCCESS.equals(sendLog.getSendStatus())
                || !AGENT_REPLY.equals(sendLog.getSendType()) || sendLog.getTicketMessageId() == null) {
            return false;
        }

        TicketMessageEntity message = ticketMessageMapper.selectByIdForUpdate(sendLog.getTicketMessageId());
        if (message == null || !SUCCESS.equals(message.getSendStatus())
                || message.getDeliveryCompletedAt() != null) {
            return false;
        }
        TicketEntity ticket = ticketMapper.selectByIdForUpdate(message.getTicketId());
        if (ticket == null) {
            throw new IllegalStateException("客服回复关联工单不存在：" + message.getTicketId());
        }

        LocalDateTime deliveredAt = sendLog.getSentAt() != null ? sendLog.getSentAt() : LocalDateTime.now();
        String operator = message.getCreatedBy() == null || message.getCreatedBy().isBlank()
                ? SYSTEM : message.getCreatedBy();

        if (!TicketBizService.STATUS_CLOSED.equals(ticket.getStatus())
                && !TicketBizService.STATUS_CANCELLED.equals(ticket.getStatus())) {
            ticket.setStatus(TicketBizService.STATUS_WAITING_CUSTOMER);
        }
        boolean firstReplyCompleted = ticket.getFirstReplyAt() == null;
        if (firstReplyCompleted) {
            ticket.setFirstReplyAt(deliveredAt);
            insertEvent(ticket.getId(), TicketBizService.EVENT_FIRST_REPLY,
                    "首次对外回复客户", operator, deliveredAt);
        }
        if (ticket.getLastAgentReplyAt() == null || ticket.getLastAgentReplyAt().isBefore(deliveredAt)) {
            ticket.setLastAgentReplyAt(deliveredAt);
        }
        ticket.setUpdatedBy(operator);
        if (ticketMapper.updateById(ticket) != 1) {
            throw new IllegalStateException("客服回复工单状态更新失败：" + ticket.getId());
        }

        insertEvent(ticket.getId(), TicketBizService.EVENT_AGENT_REPLY,
                "回复客户：" + summarize(message), operator, deliveredAt);
        message.setDeliveryCompletedAt(deliveredAt);
        message.setUpdatedBy(operator);
        if (ticketMessageMapper.updateById(message) != 1) {
            throw new IllegalStateException("客服回复完成标记更新失败：" + message.getId());
        }
        if (firstReplyCompleted) {
            eventPublisher.publishEvent(new SlaMilestoneCompletedEvent(ticket.getId(), deliveredAt));
        }
        log.info("客服回复投递完成 ticketId={} messageId={} sendLogId={}",
                ticket.getId(), message.getId(), sendLogId);
        return true;
    }

    private void insertEvent(Long ticketId, String eventType, String content,
                             String operator, LocalDateTime eventAt) {
        TicketEventEntity event = new TicketEventEntity();
        event.setTicketId(ticketId);
        event.setEventType(eventType);
        event.setEventContent(content);
        event.setOperator(operator);
        event.setEventAt(eventAt);
        event.setCreatedBy(operator);
        event.setUpdatedBy(operator);
        ticketEventMapper.insert(event);
    }

    private String summarize(TicketMessageEntity message) {
        String value = message.getContentText();
        if (value == null || value.isBlank()) {
            value = message.getContentHtml();
        }
        if (value == null || value.isBlank()) {
            return "邮件已发送";
        }
        String normalized = value.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        return normalized.length() > 200 ? normalized.substring(0, 200) : normalized;
    }
}
