package com.ntn.fziot.mailtrace.application.bizservice.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.storage.FileStorageService;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.CustomerTicketDetailVO;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketAttachmentVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketAttachmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEventEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketMessageEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketAttachmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketEventMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMessageMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class CustomerTicketAccessService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_UNAUTHORIZED = 40103;
    private static final int CODE_FORBIDDEN = 40303;
    private static final int CODE_NOT_FOUND = 40401;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MESSAGE_CONTENT_LIMIT = 2000;
    private static final int MAX_ACCESS_FAILURES = 5;
    private static final Duration ACCESS_LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration INLINE_ATTACHMENT_TOKEN_TTL = Duration.ofMinutes(10);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final PasswordEncoder passwordEncoder;
    private final TicketMapper ticketMapper;
    private final TicketEventMapper ticketEventMapper;
    private final TicketMessageMapper ticketMessageMapper;
    private final TicketAttachmentMapper ticketAttachmentMapper;
    private final FileStorageService fileStorageService;
    private final String baseUrl;
    private final long ttlHours;
    private final int codeLength;
    private final ConcurrentMap<String, AccessAttempt> accessAttempts = new ConcurrentHashMap<>();

    public CustomerTicketAccessService(
            PasswordEncoder passwordEncoder,
            TicketMapper ticketMapper,
            TicketEventMapper ticketEventMapper,
            TicketMessageMapper ticketMessageMapper,
            TicketAttachmentMapper ticketAttachmentMapper,
            FileStorageService fileStorageService,
            @Value("${mailtrace.customer-view.base-url:http://localhost:5174/customer/tickets}") String baseUrl,
            @Value("${mailtrace.customer-view.access-ttl-hours:72}") long ttlHours,
            @Value("${mailtrace.customer-view.code-length:6}") int codeLength) {
        this.passwordEncoder = passwordEncoder;
        this.ticketMapper = ticketMapper;
        this.ticketEventMapper = ticketEventMapper;
        this.ticketMessageMapper = ticketMessageMapper;
        this.ticketAttachmentMapper = ticketAttachmentMapper;
        this.fileStorageService = fileStorageService;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.ttlHours = Math.max(1, ttlHours);
        this.codeLength = Math.min(8, Math.max(4, codeLength));
    }

    public CustomerTicketAccess createAccess() {
        String code = generateCode();
        return new CustomerTicketAccess(
                code,
                passwordEncoder.encode(code),
                LocalDateTime.now().plusHours(ttlHours)
        );
    }

    public String buildTicketUrl(TicketEntity ticket) {
        String ticketNo = ticket != null && ticket.getTicketNo() != null ? ticket.getTicketNo() : "";
        return baseUrl + "/" + ticketNo;
    }

    public String formatExpiresAt(LocalDateTime expiresAt) {
        return expiresAt == null ? "" : expiresAt.format(DISPLAY_TIME_FORMATTER);
    }

    public CustomerTicketDetailVO verifyAndGetDetail(String ticketNo, String accessCode, String clientIp) {
        String normalizedAccessCode = normalize(accessCode);
        String gateKey = accessGateKey(ticketNo, clientIp);
        assertAccessAllowed(gateKey);
        TicketEntity ticket;
        try {
            ticket = requireAccessibleTicket(ticketNo, normalizedAccessCode);
            clearAccessFailures(gateKey);
        } catch (BusinessException exception) {
            recordAccessFailure(gateKey, exception);
            throw exception;
        }

        List<TicketMessageEntity> visibleMessages = visibleMessages(ticket.getId());
        Map<Long, List<TicketAttachmentEntity>> attachmentsByMessageId = inlineAttachments(ticket.getId()).stream()
                .filter(attachment -> attachment.getMessageId() != null)
                .collect(Collectors.groupingBy(TicketAttachmentEntity::getMessageId));
        CustomerTicketDetailVO.CustomerTicketEmailVO email = buildEmailInfo(
                ticket, visibleMessages, attachmentsByMessageId);
        List<CustomerTicketDetailVO.CustomerTicketTimelineVO> timeline = buildTimeline(ticket);

        return new CustomerTicketDetailVO(
                ticket.getTicketNo(),
                ticket.getSubject(),
                ticket.getStatus(),
                statusLabel(ticket.getStatus()),
                ticket.getCustomerEmail(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getFirstReplyAt(),
                ticket.getClosedAt(),
                ticket.getSlaResponseDeadline(),
                ticket.getSlaResolveDeadline(),
                ticket.getSlaBreached(),
                ticket.getCustomerAccessExpiresAt(),
                email,
                visibleMessages.stream()
                        .map(message -> toCustomerMessage(
                                ticket.getTicketNo(), message, attachmentsByMessageId, ticket))
                        .toList(),
                timeline
        );
    }

    public CustomerAttachmentDownload downloadInlineAttachment(String ticketNo, Long attachmentId, String token, String clientIp) {
        String gateKey = accessGateKey(ticketNo, clientIp);
        assertAccessAllowed(gateKey);
        try {
            TicketEntity ticket = requireTokenAccessibleTicket(ticketNo);
            validateInlineAttachmentToken(ticket, attachmentId, token);
            TicketAttachmentEntity attachment = ticketAttachmentMapper.selectById(attachmentId);
            if (attachment == null || !ticket.getId().equals(attachment.getTicketId())
                    || !Boolean.TRUE.equals(attachment.getIsInline())) {
                throw new BusinessException(CODE_NOT_FOUND, "附件不存在或不可访问");
            }
            clearAccessFailures(gateKey);
            return new CustomerAttachmentDownload(toAttachmentVO(ticket.getTicketNo(), attachment, ticket),
                    fileStorageService.download(attachment.getObjectKey()));
        } catch (BusinessException exception) {
            recordAccessFailure(gateKey, exception);
            throw exception;
        }
    }

    private String generateCode() {
        StringBuilder builder = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank()
                ? "http://localhost:5174/customer/tickets"
                : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private TicketEntity requireAccessibleTicket(String ticketNo, String accessCode) {
        String normalizedTicketNo = normalize(ticketNo);
        String normalizedAccessCode = normalize(accessCode);
        if (normalizedTicketNo.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "工单编号不能为空");
        }
        if (normalizedAccessCode.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "请输入校验码");
        }

        TicketEntity ticket = ticketMapper.selectOne(
                new LambdaQueryWrapper<TicketEntity>()
                        .eq(TicketEntity::getTicketNo, normalizedTicketNo)
                        .last("LIMIT 1"));
        if (ticket == null) {
            throw new BusinessException(CODE_NOT_FOUND, "工单不存在或链接无效");
        }
        if (!Boolean.TRUE.equals(ticket.getCustomerAccessEnabled())) {
            throw new BusinessException(CODE_FORBIDDEN, "该工单暂不支持客户查看");
        }
        if (ticket.getCustomerAccessExpiresAt() == null
                || LocalDateTime.now().isAfter(ticket.getCustomerAccessExpiresAt())) {
            throw new BusinessException(CODE_FORBIDDEN, "链接已过期，请回复原邮件联系处理人员");
        }
        if (ticket.getCustomerAccessCodeHash() == null
                || !passwordEncoder.matches(normalizedAccessCode, ticket.getCustomerAccessCodeHash())) {
            throw new BusinessException(CODE_UNAUTHORIZED, "校验码不正确");
        }
        return ticket;
    }

    private TicketEntity requireTokenAccessibleTicket(String ticketNo) {
        String normalizedTicketNo = normalize(ticketNo);
        if (normalizedTicketNo.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "工单编号不能为空");
        }

        TicketEntity ticket = ticketMapper.selectOne(
                new LambdaQueryWrapper<TicketEntity>()
                        .eq(TicketEntity::getTicketNo, normalizedTicketNo)
                        .last("LIMIT 1"));
        if (ticket == null) {
            throw new BusinessException(CODE_NOT_FOUND, "工单不存在或链接无效");
        }
        if (!Boolean.TRUE.equals(ticket.getCustomerAccessEnabled())) {
            throw new BusinessException(CODE_FORBIDDEN, "该工单暂不支持客户查看");
        }
        if (ticket.getCustomerAccessExpiresAt() == null
                || LocalDateTime.now().isAfter(ticket.getCustomerAccessExpiresAt())) {
            throw new BusinessException(CODE_FORBIDDEN, "链接已过期，请回复原邮件联系处理人员");
        }
        return ticket;
    }

    private String buildCustomerAttachmentDownloadUrl(String ticketNo, Long attachmentId, TicketEntity ticket) {
        return "/api/v1/customer-tickets/" + encodeUrl(ticketNo)
                + "/attachments/" + attachmentId
                + "/download?token=" + encodeUrl(createInlineAttachmentToken(ticket, attachmentId));
    }

    private String encodeUrl(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String createInlineAttachmentToken(TicketEntity ticket, Long attachmentId) {
        if (attachmentId == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "附件ID不能为空");
        }
        long expiresAt = Math.min(
                System.currentTimeMillis() + INLINE_ATTACHMENT_TOKEN_TTL.toMillis(),
                toEpochMillis(ticket.getCustomerAccessExpiresAt())
        );
        String payload = normalize(ticket.getTicketNo()) + ":" + attachmentId + ":" + expiresAt;
        return base64Url(payload.getBytes(StandardCharsets.UTF_8)) + "." + base64Url(hmac(ticket, payload));
    }

    private void validateInlineAttachmentToken(TicketEntity ticket, Long attachmentId, String token) {
        if (attachmentId == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "附件ID不能为空");
        }
        String normalizedToken = normalize(token);
        String[] parts = normalizedToken.split("\\.", 2);
        if (parts.length != 2) {
            throw new BusinessException(CODE_UNAUTHORIZED, "附件访问令牌无效");
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] payloadParts = payload.split(":", 3);
            if (payloadParts.length != 3) {
                throw new BusinessException(CODE_UNAUTHORIZED, "附件访问令牌无效");
            }
            if (!normalize(ticket.getTicketNo()).equals(payloadParts[0])) {
                throw new BusinessException(CODE_UNAUTHORIZED, "附件访问令牌无效");
            }
            if (!String.valueOf(attachmentId).equals(payloadParts[1])) {
                throw new BusinessException(CODE_UNAUTHORIZED, "附件访问令牌无效");
            }
            long expiresAt = Long.parseLong(payloadParts[2]);
            if (System.currentTimeMillis() > expiresAt) {
                throw new BusinessException(CODE_FORBIDDEN, "附件访问令牌已过期");
            }
            byte[] expected = hmac(ticket, payload);
            byte[] actual = Base64.getUrlDecoder().decode(parts[1]);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw new BusinessException(CODE_UNAUTHORIZED, "附件访问令牌无效");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(CODE_UNAUTHORIZED, "附件访问令牌无效");
        }
    }

    private byte[] hmac(TicketEntity ticket, String payload) {
        String key = normalize(ticket.getCustomerAccessCodeHash());
        if (key.isEmpty()) {
            throw new BusinessException(CODE_FORBIDDEN, "该工单暂不支持客户查看");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new BusinessException(CODE_FORBIDDEN, "附件访问令牌生成失败");
        }
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private long toEpochMillis(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String accessGateKey(String ticketNo, String clientIp) {
        String normalizedTicketNo = normalize(ticketNo).toUpperCase(Locale.ROOT);
        String normalizedClientIp = normalize(clientIp);
        return normalizedTicketNo + "|" + (normalizedClientIp.isEmpty() ? "unknown" : normalizedClientIp);
    }

    private void assertAccessAllowed(String gateKey) {
        AccessAttempt attempt = accessAttempts.get(gateKey);
        if (attempt == null || attempt.lockedUntilMillis() <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (attempt.lockedUntilMillis() > now) {
            throw new BusinessException(CODE_FORBIDDEN, "校验失败次数过多，请稍后再试");
        }
        accessAttempts.remove(gateKey, attempt);
    }

    private void recordAccessFailure(String gateKey, BusinessException exception) {
        if (exception.getCode() != CODE_UNAUTHORIZED && exception.getCode() != CODE_NOT_FOUND) {
            return;
        }
        long now = System.currentTimeMillis();
        accessAttempts.compute(gateKey, (key, oldAttempt) -> {
            int failureCount = 1;
            long firstFailedAt = now;
            if (oldAttempt != null && now - oldAttempt.firstFailedAtMillis() <= ACCESS_LOCK_DURATION.toMillis()) {
                failureCount = oldAttempt.failureCount() + 1;
                firstFailedAt = oldAttempt.firstFailedAtMillis();
            }
            long lockedUntil = failureCount >= MAX_ACCESS_FAILURES
                    ? now + ACCESS_LOCK_DURATION.toMillis()
                    : 0;
            return new AccessAttempt(failureCount, firstFailedAt, lockedUntil);
        });
    }

    private void clearAccessFailures(String gateKey) {
        accessAttempts.remove(gateKey);
    }

    private TicketAttachmentVO toAttachmentVO(String ticketNo, TicketAttachmentEntity attachment, TicketEntity ticket) {
        return new TicketAttachmentVO(
                attachment.getId(),
                attachment.getMessageId(),
                attachment.getFileName(),
                attachment.getFileSize(),
                attachment.getContentType(),
                buildCustomerAttachmentDownloadUrl(ticketNo, attachment.getId(), ticket),
                attachment.getIsInline(),
                attachment.getContentId(),
                attachment.getUploadedBy(),
                attachment.getCreatedAt()
        );
    }

    private List<TicketMessageEntity> visibleMessages(Long ticketId) {
        return ticketMessageMapper.selectList(
                        new LambdaQueryWrapper<TicketMessageEntity>()
                                .eq(TicketMessageEntity::getTicketId, ticketId)
                                .orderByAsc(TicketMessageEntity::getSentAt)
                                .orderByAsc(TicketMessageEntity::getCreatedAt))
                .stream()
                .filter(message -> TicketBizService.DIRECTION_INBOUND.equals(message.getDirection())
                        || (TicketBizService.DIRECTION_OUTBOUND.equals(message.getDirection())
                        && "SUCCESS".equals(message.getSendStatus())))
                .toList();
    }

    private List<TicketAttachmentEntity> inlineAttachments(Long ticketId) {
        return ticketAttachmentMapper.selectList(
                new LambdaQueryWrapper<TicketAttachmentEntity>()
                        .eq(TicketAttachmentEntity::getTicketId, ticketId)
                        .eq(TicketAttachmentEntity::getIsInline, true)
        );
    }

    private CustomerTicketDetailVO.CustomerTicketEmailVO buildEmailInfo(
            TicketEntity ticket,
            List<TicketMessageEntity> visibleMessages,
            Map<Long, List<TicketAttachmentEntity>> attachmentsByMessageId) {
        TicketMessageEntity firstInbound = visibleMessages.stream()
                .filter(message -> TicketBizService.DIRECTION_INBOUND.equals(message.getDirection()))
                .findFirst()
                .orElse(visibleMessages.isEmpty() ? null : visibleMessages.get(0));
        if (firstInbound == null) {
            return new CustomerTicketDetailVO.CustomerTicketEmailVO(
                    ticket.getCustomerEmail(), null, ticket.getSubject(), ticket.getCreatedAt(), "", null);
        }
        return new CustomerTicketDetailVO.CustomerTicketEmailVO(
                firstInbound.getFromAddress(),
                firstInbound.getToAddress(),
                firstInbound.getSubject(),
                messageTime(firstInbound),
                messageText(firstInbound),
                renderCustomerMessageHtml(ticket.getTicketNo(), firstInbound,
                        attachmentsByMessageId.getOrDefault(firstInbound.getId(), List.of()), ticket)
        );
    }

    private CustomerTicketDetailVO.CustomerTicketMessageVO toCustomerMessage(
            String ticketNo,
            TicketMessageEntity message,
            Map<Long, List<TicketAttachmentEntity>> attachmentsByMessageId,
            TicketEntity ticket) {
        return new CustomerTicketDetailVO.CustomerTicketMessageVO(
                message.getDirection(),
                message.getFromAddress(),
                message.getToAddress(),
                message.getSubject(),
                messageTime(message),
                messageText(message),
                renderCustomerMessageHtml(ticketNo, message,
                        attachmentsByMessageId.getOrDefault(message.getId(), List.of()), ticket)
        );
    }

    private String renderCustomerMessageHtml(
            String ticketNo,
            TicketMessageEntity message,
            List<TicketAttachmentEntity> attachments,
            TicketEntity ticket) {
        String html = message.getContentHtml();
        if (html == null || html.isBlank() || attachments == null || attachments.isEmpty()) {
            return html;
        }
        String rendered = html;
        for (TicketAttachmentEntity attachment : attachments) {
            String url = buildCustomerAttachmentDownloadUrl(ticketNo, attachment.getId(), ticket);
            if (attachment.getContentId() != null && !attachment.getContentId().isBlank()) {
                rendered = rendered
                        .replace("cid:" + attachment.getContentId(), url)
                        .replace("cid:<" + attachment.getContentId() + ">", url);
            }
            if (attachment.getObjectKey() != null && !attachment.getObjectKey().isBlank()) {
                rendered = rendered.replaceAll(
                        "https?://[^\"'\\s<>]+/" + java.util.regex.Pattern.quote(attachment.getObjectKey()),
                        url
                );
            }
        }
        return rendered;
    }

    private List<CustomerTicketDetailVO.CustomerTicketTimelineVO> buildTimeline(TicketEntity ticket) {
        List<TicketEventEntity> events = ticketEventMapper.selectList(
                new LambdaQueryWrapper<TicketEventEntity>()
                        .eq(TicketEventEntity::getTicketId, ticket.getId())
                        .orderByAsc(TicketEventEntity::getEventAt));
        List<CustomerTicketDetailVO.CustomerTicketTimelineVO> timeline = new ArrayList<>();
        boolean assignedAdded = false;
        boolean firstReplyAdded = false;
        boolean firstReplyAgentSkipped = false;
        boolean closedAdded = false;
        boolean cancelledAdded = false;

        for (TicketEventEntity event : events) {
            String type = event.getEventType();
            if (TicketBizService.EVENT_CREATED.equals(type)) {
                timeline.add(timeline("RECEIVED", "工单已创建",
                        "系统已收到您的邮件并创建工单，工单编号为 " + ticket.getTicketNo() + "。",
                        "系统", event.getEventAt()));
            } else if (TicketBizService.EVENT_ASSIGNED.equals(type) && !assignedAdded) {
                timeline.add(timeline("PROCESSING", "已进入处理",
                        "工单已进入处理队列，后续处理进度会通过邮件或本页面同步。",
                        "系统", event.getEventAt()));
                assignedAdded = true;
            } else if (TicketBizService.EVENT_FIRST_REPLY.equals(type) && !firstReplyAdded) {
                timeline.add(timeline("FIRST_REPLY", "已首次回复",
                        "客服已通过邮件回复，首次响应已完成。",
                        "邮件通知", event.getEventAt()));
                firstReplyAdded = true;
            } else if (TicketBizService.EVENT_AGENT_REPLY.equals(type)) {
                if (!firstReplyAgentSkipped) {
                    firstReplyAgentSkipped = true;
                    if (firstReplyAdded) {
                        continue;
                    }
                }
                timeline.add(timeline(firstReplyAdded ? "AGENT_REPLY" : "FIRST_REPLY",
                        firstReplyAdded ? "已回复客户" : "已首次回复",
                        "客服已通过邮件回复，处理进度已同步至客户邮箱。",
                        "邮件通知", event.getEventAt()));
            } else if (TicketBizService.EVENT_CUSTOMER_FOLLOWUP.equals(type)) {
                timeline.add(timeline("CUSTOMER_FOLLOWUP", "已收到补充信息",
                        "系统已收到您的补充邮件，处理人员会继续跟进。",
                        "客户来信", event.getEventAt()));
            } else if (TicketBizService.EVENT_REOPENED.equals(type)) {
                timeline.add(timeline("REOPENED", "工单已重新打开",
                        "系统已根据新的客户来信重新打开工单。",
                        "系统", event.getEventAt()));
            } else if (TicketBizService.EVENT_CLOSED.equals(type) && !closedAdded) {
                timeline.add(timeline("CLOSED", "工单已完成",
                        "该工单已完成处理。",
                        "系统", event.getEventAt()));
                closedAdded = true;
            } else if (TicketBizService.EVENT_CANCELLED.equals(type) && !cancelledAdded) {
                timeline.add(timeline("CANCELLED", "工单已取消",
                        "该工单已取消。",
                        "系统", event.getEventAt()));
                cancelledAdded = true;
            } else if (TicketBizService.EVENT_STATUS_CHANGED.equals(type)) {
                appendStatusChangedTimeline(ticket, event, timeline);
            }
        }

        if (timeline.stream().noneMatch(item -> "RECEIVED".equals(item.stage()))) {
            timeline.add(timeline("RECEIVED", "工单已创建",
                    "系统已收到您的邮件并创建工单，工单编号为 " + ticket.getTicketNo() + "。",
                    "系统", ticket.getCreatedAt()));
        }
        timeline.sort(Comparator.comparing(CustomerTicketDetailVO.CustomerTicketTimelineVO::eventAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return timeline;
    }

    private void appendStatusChangedTimeline(
            TicketEntity ticket,
            TicketEventEntity event,
            List<CustomerTicketDetailVO.CustomerTicketTimelineVO> timeline) {
        if (event.getEventContent() == null) {
            return;
        }
        if (event.getEventContent().contains(statusLabel(TicketBizService.STATUS_WAITING_CUSTOMER))) {
            timeline.add(timeline("WAITING_CUSTOMER", "等待客户回复",
                    "客服已回复，当前等待客户补充或确认。",
                    "当前进展", event.getEventAt()));
        } else if (event.getEventContent().contains(statusLabel(TicketBizService.STATUS_PROCESSING))) {
            timeline.add(timeline("PROCESSING", "处理中",
                    currentProgressText(ticket),
                    "当前进展", event.getEventAt()));
        }
    }

    private CustomerTicketDetailVO.CustomerTicketTimelineVO timeline(
            String stage, String title, String content, String badge, LocalDateTime eventAt) {
        return new CustomerTicketDetailVO.CustomerTicketTimelineVO(stage, title, content, badge, eventAt);
    }

    private String currentProgressText(TicketEntity ticket) {
        if (TicketBizService.STATUS_CLOSED.equals(ticket.getStatus())) {
            return "该工单已完成处理。";
        }
        if (TicketBizService.STATUS_CANCELLED.equals(ticket.getStatus())) {
            return "该工单已取消。";
        }
        if (TicketBizService.STATUS_WAITING_CUSTOMER.equals(ticket.getStatus())) {
            return "客服已回复，当前等待客户补充或确认。";
        }
        if (ticket.getSlaResolveDeadline() != null) {
            return "当前正在处理中，预计在 " + formatExpiresAt(ticket.getSlaResolveDeadline()) + " 前给出处理结果。";
        }
        return "当前正在处理中，处理进度会通过邮件或本页面同步。";
    }

    private LocalDateTime messageTime(TicketMessageEntity message) {
        return message.getSentAt() != null ? message.getSentAt() : message.getCreatedAt();
    }

    private String messageText(TicketMessageEntity message) {
        String text = normalize(message.getContentText());
        if (text.isEmpty()) {
            text = htmlToText(message.getContentHtml());
        }
        if (text.length() <= MESSAGE_CONTENT_LIMIT) {
            return text;
        }
        return text.substring(0, MESSAGE_CONTENT_LIMIT) + "...";
    }

    private String htmlToText(String html) {
        String text = normalize(html);
        if (text.isEmpty()) {
            return "";
        }
        return text
                .replaceAll("(?is)<(script|style).*?>.*?</\\1>", "")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .trim();
    }

    private String statusLabel(String status) {
        return switch (normalize(status).toUpperCase(Locale.ROOT)) {
            case TicketBizService.STATUS_PENDING_ASSIGN -> "待处理";
            case TicketBizService.STATUS_PROCESSING -> "处理中";
            case TicketBizService.STATUS_WAITING_CUSTOMER -> "待客户回复";
            case TicketBizService.STATUS_CLOSED -> "已完成";
            case TicketBizService.STATUS_CANCELLED -> "已取消";
            default -> status;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record CustomerTicketAccess(
            String code,
            String codeHash,
            LocalDateTime expiresAt
    ) {
    }

    public record CustomerAttachmentDownload(
            TicketAttachmentVO attachment,
            InputStream inputStream
    ) {
    }

    private record AccessAttempt(
            int failureCount,
            long firstFailedAtMillis,
            long lockedUntilMillis
    ) {
    }
}
