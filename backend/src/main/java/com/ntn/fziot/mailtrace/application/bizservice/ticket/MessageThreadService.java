package com.ntn.fziot.mailtrace.application.bizservice.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.infrastructure.mail.ParsedMail;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketMessageEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 邮件线程关联服务。
 * 根据 In-Reply-To / References / 主题中的工单号，将新邮件关联到已有工单。
 * <p>
 * 优先级：
 * 1. In-Reply-To / References → 匹配 mt_ticket_message → 关联原工单
 * 2. 主题含工单号（前缀-日期-随机数） → 匹配 mt_ticket → 关联工单
 * 3. 均不匹配 → 返回 null，由调用方决定新建工单
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageThreadService {

    /** 工单号正则：兼容 TCK-20260724-0001 与 TCK-260821093012-482931 */
    private static final Pattern TICKET_NO_PATTERN = Pattern.compile("([A-Z0-9]{2,8}[-_]?(?:\\d{12}|\\d{8}|\\d{6}|\\d{4})[-_]?\\d{1,6})");

    private final TicketMessageMapper ticketMessageMapper;
    private final TicketMapper ticketMapper;

    /**
     * 解析邮件应归属的工单 ID。
     *
     * @param mail 已解析的邮件
     * @return 工单 ID，null 表示未匹配到任何已有工单
     */
    public Long resolveTicketId(ParsedMail mail) {
        // 优先级 1：In-Reply-To / References 匹配
        Long byMessageId = resolveByMessageId(mail);
        if (byMessageId != null) {
            log.info("线程关联：通过 Message-ID 匹配到工单 ticketId={} messageId={}",
                    byMessageId, mail.messageId());
            return byMessageId;
        }

        // 优先级 2：主题提取工单号
        Long byTicketNo = resolveByTicketNo(mail);
        if (byTicketNo != null) {
            log.info("线程关联：通过工单号匹配到工单 ticketId={} subject={}",
                    byTicketNo, mail.subject());
            return byTicketNo;
        }

        // 均不匹配
        log.info("线程关联：未匹配到已有工单，将新建 messageId={} subject={}",
                mail.messageId(), mail.subject());
        return null;
    }

    /**
     * 通过 In-Reply-To / References 头匹配已有消息的 Message-ID。
     */
    private Long resolveByMessageId(ParsedMail mail) {
        // 优先检查 In-Reply-To
        if (mail.inReplyTo() != null && !mail.inReplyTo().isBlank()) {
            String refId = extractMessageId(mail.inReplyTo());
            Long ticketId = findTicketIdByMessageId(refId);
            if (ticketId != null) return ticketId;
        }

        // 再检查 References（取最后一个 ID）
        if (mail.references() != null && !mail.references().isBlank()) {
            String refId = extractLastMessageId(mail.references());
            if (refId != null) {
                Long ticketId = findTicketIdByMessageId(refId);
                if (ticketId != null) return ticketId;
            }
        }

        return null;
    }

    /**
     * 从引用头中提取干净的消息 ID（去掉 <>）。
     */
    private String extractMessageId(String raw) {
        return normalizeMessageId(raw);
    }

    public static String normalizeMessageId(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isBlank()) return null;
        if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    /**
     * 从 References 头中提取最后一个 Message-ID。
     */
    private String extractLastMessageId(String references) {
        if (references == null || references.isBlank()) return null;
        // References 可包含多个 <id1> <id2> <id3>
        String[] parts = references.trim().split("\\s+");
        return extractMessageId(parts[parts.length - 1]);
    }

    /**
     * 根据 Message-ID 查 mt_ticket_message，返回 ticketId。
     */
    private Long findTicketIdByMessageId(String messageId) {
        String normalized = normalizeMessageId(messageId);
        if (normalized == null || normalized.isBlank()) return null;
        String bracketed = "<" + normalized + ">";
        TicketMessageEntity msg = ticketMessageMapper.selectOne(
                new LambdaQueryWrapper<TicketMessageEntity>()
                        .and(wrapper -> wrapper
                                .eq(TicketMessageEntity::getMessageId, normalized)
                                .or()
                                .eq(TicketMessageEntity::getMessageId, bracketed))
                        .last("LIMIT 1"));
        return msg != null ? msg.getTicketId() : null;
    }

    /**
     * 从邮件主题中提取工单号并匹配工单。
     */
    private Long resolveByTicketNo(ParsedMail mail) {
        if (mail.subject() == null || mail.subject().isBlank()) return null;
        Matcher matcher = TICKET_NO_PATTERN.matcher(mail.subject());
        if (matcher.find()) {
            String ticketNo = matcher.group(1);
            TicketEntity ticket = ticketMapper.selectOne(
                    new LambdaQueryWrapper<TicketEntity>()
                            .eq(TicketEntity::getTicketNo, ticketNo)
                            .last("LIMIT 1"));
            return ticket != null ? ticket.getId() : null;
        }
        return null;
    }
}
