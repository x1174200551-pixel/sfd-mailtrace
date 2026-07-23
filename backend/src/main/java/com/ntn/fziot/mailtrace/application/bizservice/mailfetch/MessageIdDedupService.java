package com.ntn.fziot.mailtrace.application.bizservice.mailfetch;

import com.ntn.fziot.mailtrace.infrastructure.mail.ParsedMail;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Message-ID 去重服务。
 * 检查 mt_ticket_message 表中是否已存在相同 Message-ID，
 * 避免同一封邮件被重复拉取和建单。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageIdDedupService {

    private final TicketMessageMapper ticketMessageMapper;

    /**
     * 判断某个 Message-ID 是否已经存在。
     * 使用原始 SQL 查询，不附加 MyBatis-Plus 的逻辑删除过滤。
     * 软删除的记录也视为重复，避免唯一索引冲突。
     *
     * @param messageId Message-ID（可为 null）
     * @return true = 已存在（重复），false = 不存在（新邮件）
     */
    public boolean isDuplicate(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return false;
        }
        int count = ticketMessageMapper.countExistingByMessageId(messageId);
        boolean duplicate = count > 0;
        if (duplicate) {
            log.debug("Message-ID 已存在，跳过重复 messageId={}", messageId);
        }
        return duplicate;
    }

    /**
     * 判断一封已解析邮件是否为重复。
     *
     * @param mail 已解析邮件
     * @return true = 重复
     */
    public boolean isDuplicate(ParsedMail mail) {
        if (mail == null) return false;
        return isDuplicate(mail.messageId());
    }

    /**
     * 从已解析邮件列表中过滤掉重复项，保留新邮件。
     * 保持原有顺序。
     *
     * @param mails 已解析邮件列表
     * @return 非重复的邮件列表
     */
    public List<ParsedMail> filterNew(List<ParsedMail> mails) {
        if (mails == null || mails.isEmpty()) {
            return List.of();
        }
        List<ParsedMail> result = new ArrayList<>();
        for (ParsedMail mail : mails) {
            if (!isDuplicate(mail)) {
                result.add(mail);
            } else {
                log.info("跳过重复邮件 messageId={} from={} subject={}",
                        mail.messageId(), mail.fromAddress(), truncateSubject(mail.subject()));
            }
        }
        int skipped = mails.size() - result.size();
        if (skipped > 0) {
            log.info("Message-ID 去重完成：总数={}，新邮件={}，跳过重复={}",
                    mails.size(), result.size(), skipped);
        }
        return result;
    }

    private String truncateSubject(String subject) {
        if (subject == null) return "";
        return subject.length() > 80 ? subject.substring(0, 80) + "..." : subject;
    }
}
