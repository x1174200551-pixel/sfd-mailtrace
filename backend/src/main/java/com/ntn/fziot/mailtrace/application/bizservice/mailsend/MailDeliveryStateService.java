package com.ntn.fziot.mailtrace.application.bizservice.mailsend;

import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketMessageEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailSendLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 邮件投递状态的短事务边界。SMTP 网络调用不能进入这些事务。
 */
@Service
@RequiredArgsConstructor
public class MailDeliveryStateService {

    private static final String OPERATOR_SYSTEM = "SYSTEM";

    private final MailSendLogMapper mailSendLogMapper;
    private final TicketMessageMapper ticketMessageMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimInitial(Long sendLogId) {
        return mailSendLogMapper.claimPendingForSend(sendLogId) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimRetry(Long sendLogId) {
        return mailSendLogMapper.claimFailedForRetry(sendLogId) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MailSendLogEntity reload(Long sendLogId) {
        return mailSendLogMapper.selectById(sendLogId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long sendLogId, LocalDateTime sentAt) {
        if (mailSendLogMapper.markDeliverySuccess(sendLogId, sentAt) != 1) {
            throw new IllegalStateException("发送日志成功状态更新失败：" + sendLogId);
        }
        MailSendLogEntity sendLog = requireLog(sendLogId);
        updateLinkedMessage(sendLog, "SUCCESS", sentAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long sendLogId, String errorMessage) {
        if (mailSendLogMapper.markDeliveryFailed(sendLogId, errorMessage) != 1) {
            throw new IllegalStateException("发送日志失败状态更新失败：" + sendLogId);
        }
        MailSendLogEntity sendLog = requireLog(sendLogId);
        updateLinkedMessage(sendLog, "FAILED", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markUnknown(Long sendLogId, String errorMessage) {
        return mailSendLogMapper.markDeliveryUnknown(sendLogId, errorMessage) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markCancelled(Long sendLogId, String reason) {
        return mailSendLogMapper.markDeliveryCancelled(sendLogId, reason) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markStaleUnknown(LocalDateTime before) {
        return mailSendLogMapper.markStaleDeliveriesUnknown(before);
    }

    private MailSendLogEntity requireLog(Long sendLogId) {
        MailSendLogEntity sendLog = mailSendLogMapper.selectById(sendLogId);
        if (sendLog == null) {
            throw new IllegalStateException("发送日志不存在：" + sendLogId);
        }
        return sendLog;
    }

    private void updateLinkedMessage(MailSendLogEntity sendLog, String status, LocalDateTime sentAt) {
        if (sendLog.getTicketMessageId() == null) {
            return;
        }
        TicketMessageEntity message = ticketMessageMapper.selectById(sendLog.getTicketMessageId());
        if (message == null) {
            throw new IllegalStateException("发送日志关联消息不存在：" + sendLog.getTicketMessageId());
        }
        message.setMessageId(MailThreadHeaders.normalizeMessageId(sendLog.getMessageId()));
        message.setInReplyTo(MailThreadHeaders.normalizeMessageId(sendLog.getInReplyTo()));
        message.setMailReferences(MailThreadHeaders.normalizeReferences(sendLog.getMailReferences()));
        message.setSendStatus(status);
        if (sentAt != null) {
            message.setSentAt(sentAt);
        }
        message.setUpdatedBy(OPERATOR_SYSTEM);
        if (ticketMessageMapper.updateById(message) != 1) {
            throw new IllegalStateException("工单消息状态更新失败：" + message.getId());
        }
    }
}
