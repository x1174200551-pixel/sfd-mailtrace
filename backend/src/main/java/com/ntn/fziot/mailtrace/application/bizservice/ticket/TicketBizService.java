package com.ntn.fziot.mailtrace.application.bizservice.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntn.fziot.mailtrace.application.bizservice.assignment.AssignmentRuleMatchResult;
import com.ntn.fziot.mailtrace.application.bizservice.assignment.AssignmentRuleService;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.application.bizservice.security.DataScopeService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.application.bizservice.sla.SlaDeadlineResult;
import com.ntn.fziot.mailtrace.application.bizservice.sla.SlaDeadlineService;
import com.ntn.fziot.mailtrace.application.bizservice.sysparam.TicketNumberRuleService;
import com.ntn.fziot.mailtrace.infrastructure.mail.AttachmentInfo;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.infrastructure.storage.FileStorageService;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketAssignRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketEventVO;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketMessageVO;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketPriorityRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketRemarkRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketReplyRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketStatusRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketSummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketAttachmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEventEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketMessageEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketEventMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMessageMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketAttachmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketBizService {

    // ---------- 状态常量 ----------
    public static final String STATUS_PENDING_ASSIGN = "PENDING_ASSIGN";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_WAITING_CUSTOMER = "WAITING_CUSTOMER";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // ---------- 事件类型 ----------
    public static final String EVENT_CREATED = "CREATED";
    public static final String EVENT_ASSIGNED = "ASSIGNED";
    public static final String EVENT_FIRST_REPLY = "FIRST_REPLY";
    public static final String EVENT_AGENT_REPLY = "AGENT_REPLY";
    public static final String EVENT_INTERNAL_NOTE = "INTERNAL_NOTE";
    public static final String EVENT_STATUS_CHANGED = "STATUS_CHANGED";
    public static final String EVENT_PRIORITY_CHANGED = "PRIORITY_CHANGED";
    public static final String EVENT_CLOSED = "CLOSED";
    public static final String EVENT_CANCELLED = "CANCELLED";
    public static final String EVENT_CUSTOMER_FOLLOWUP = "CUSTOMER_FOLLOWUP";
    public static final String EVENT_REOPENED = "REOPENED";

    // ---------- 消息方向 ----------
    public static final String DIRECTION_OUTBOUND = "OUTBOUND";
    public static final String DIRECTION_INTERNAL = "INTERNAL";
    public static final String DIRECTION_INBOUND = "INBOUND";

    // ---------- 权限常量 ----------
    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_NOT_FOUND = 40401;
    private static final String OPERATOR_SYSTEM = "system";
    private static final String MODULE_TICKET = "TICKET";
    private static final int MAX_TICKET_INSERT_ATTEMPTS = 10;

    private static final Set<String> VALID_STATUSES = Set.of(
            STATUS_PENDING_ASSIGN, STATUS_PROCESSING, STATUS_WAITING_CUSTOMER, STATUS_CLOSED, STATUS_CANCELLED
    );

    /** 非终态列表（可流转） */
    private static final Set<String> ACTIVE_STATUSES = Set.of(
            STATUS_PENDING_ASSIGN, STATUS_PROCESSING, STATUS_WAITING_CUSTOMER
    );

    private final TicketMapper ticketMapper;
    private final TicketEventMapper ticketEventMapper;
    private final TicketMessageMapper ticketMessageMapper;
    private final MailboxMapper mailboxMapper;
    private final UserMapper userMapper;
    private final OperationLogMapper operationLogMapper;
    private final TicketNumberRuleService ticketNumberRuleService;
    private final AutoReplyService autoReplyService;
    private final MailSendService mailSendService;
    private final TicketAttachmentMapper ticketAttachmentMapper;
    private final AssignmentRuleService assignmentRuleService;
    private final SlaDeadlineService slaDeadlineService;
    private final DataScopeService dataScopeService;
    private final PermissionService permissionService;
    private final FileStorageService fileStorageService;
    private final CustomerTicketAccessService customerTicketAccessService;

    // ==================== 创建工单（系统内部调用） ====================

    /**
     * 系统内部创建工单，从 IMAP 拉取的邮件创建而来。
     * 不校验权限，由系统调用。
     *
     * @param mailboxId      来源邮箱ID
     * @param subject        邮件主题
     * @param customerEmail  客户邮箱
     * @param customerName   客户名称（可选）
     * @param contentText    邮件正文纯文本
     * @param contentHtml    邮件正文HTML（可选）
     * @param messageId      邮件Message-ID（去重用）
     * @param inReplyTo      In-Reply-To头
     * @param references     References头
     * @param mailSentAt     邮件原始发送时间
     * @return 创建的工单ID
     */
    @Transactional
    public Long createTicket(Long mailboxId, String subject, String customerEmail, String customerName,
                             String contentText, String contentHtml, String messageId,
                             String inReplyTo, String references, LocalDateTime mailSentAt) {
        return createTicket(
                mailboxId, subject, customerEmail, customerName,
                contentText, contentHtml, messageId, inReplyTo, references, mailSentAt,
                List.of(), List.of(), List.of(), null, null, List.of()
        );
    }

    @Transactional
    public Long createTicket(Long mailboxId, String subject, String customerEmail, String customerName,
                             String contentText, String contentHtml, String messageId,
                             String inReplyTo, String references, LocalDateTime mailSentAt,
                             List<String> toAddresses, List<String> ccAddresses, List<String> bccAddresses,
                             String rawHeaders, byte[] rawEml, List<AttachmentInfo> attachments) {
        // 1、创建工单，并在入库前计算首次响应/解决 SLA 截止时间。
        TicketEntity ticket = new TicketEntity();
        ticket.setSubject(subject);
        ticket.setStatus(STATUS_PENDING_ASSIGN);
        ticket.setPriority("NORMAL");
        ticket.setMailboxId(mailboxId);
        ticket.setCustomerEmail(customerEmail);
        ticket.setLinkSuspect(false);
        ticket.setSlaBreached(false);
        ticket.setSlaWarningSent(false);
        ticket.setSlaBreachNotified(false);
        CustomerTicketAccessService.CustomerTicketAccess customerAccess = customerTicketAccessService.createAccess();
        ticket.setCustomerAccessCodeHash(customerAccess.codeHash());
        ticket.setCustomerAccessExpiresAt(customerAccess.expiresAt());
        ticket.setCustomerAccessEnabled(true);
        applySlaDeadlines(ticket, mailSentAt);
        ticket.setCreatedBy(OPERATOR_SYSTEM);
        ticket.setUpdatedBy(OPERATOR_SYSTEM);
        insertTicketWithRetry(ticket, mailboxId, customerEmail, subject);

        // 2、保存原始邮件消息
        TicketMessageEntity msg = new TicketMessageEntity();
        msg.setTicketId(ticket.getId());
        msg.setDirection(DIRECTION_INBOUND);
        msg.setMessageId(MessageThreadService.normalizeMessageId(messageId));
        msg.setInReplyTo(MessageThreadService.normalizeMessageId(inReplyTo));
        msg.setMailReferences(references);
        msg.setFromAddress(customerEmail);
        msg.setToAddress(joinAddresses(toAddresses));
        msg.setToAddresses(joinAddresses(toAddresses));
        msg.setCcAddresses(joinAddresses(ccAddresses));
        msg.setBccAddresses(joinAddresses(bccAddresses));
        msg.setSubject(subject);
        msg.setContentText(contentText);
        msg.setContentHtml(contentHtml);
        msg.setRawHeaders(rawHeaders);
        saveRawEml(msg, messageId, rawEml);
        msg.setSentAt(mailSentAt);
        msg.setCreatedBy(OPERATOR_SYSTEM);
        msg.setUpdatedBy(OPERATOR_SYSTEM);
        ticketMessageMapper.insert(msg);
        String renderedHtml = saveIncomingAttachments(ticket.getId(), msg.getId(), contentHtml, attachments);
        if (renderedHtml != null && !renderedHtml.equals(contentHtml)) {
            msg.setContentHtml(renderedHtml);
            ticketMessageMapper.updateById(msg);
        }

        // 3、记录生命周期事件：CREATED
        recordEvent(ticket.getId(), EVENT_CREATED, "工单已创建，来源邮箱ID：" + mailboxId, OPERATOR_SYSTEM, LocalDateTime.now());

        // 4、尝试自动分配（分配规则优先，邮箱默认处理人兜底）
        autoAssignByRules(ticket, mailboxId, subject, customerEmail);

        // 5、发送自动回执（失败不影响工单）
        AutoReplyService.AutoReplyResult autoReplyResult = autoReplyService.sendAutoReply(
                ticket.getId(), mailboxId, customerAccess.code());

        // 6、保存自动回执消息到会话（OUTBOUND）
        if (autoReplyResult != null && autoReplyResult.success()) {
            try {
                MailboxEntity mb = mailboxMapper.selectById(mailboxId);
                String mailFrom = (mb != null && mb.getEmailAddress() != null) ? mb.getEmailAddress() : "system@mailtrace.local";
                TicketMessageEntity autoReplyMsg = new TicketMessageEntity();
                autoReplyMsg.setTicketId(ticket.getId());
                autoReplyMsg.setDirection(DIRECTION_OUTBOUND);
                autoReplyMsg.setFromAddress(mailFrom);
                autoReplyMsg.setToAddress(customerEmail);
                autoReplyMsg.setSubject(autoReplyResult.subject());
                autoReplyMsg.setContentText(autoReplyResult.contentText());
                autoReplyMsg.setContentHtml(autoReplyResult.contentHtml());
                autoReplyMsg.setMessageId(MessageThreadService.normalizeMessageId(autoReplyResult.messageId()));
                autoReplyMsg.setSentAt(LocalDateTime.now());
                autoReplyMsg.setCreatedBy(OPERATOR_SYSTEM);
                autoReplyMsg.setUpdatedBy(OPERATOR_SYSTEM);
                ticketMessageMapper.insert(autoReplyMsg);
            } catch (Exception e) {
                log.warn("保存自动回执消息失败，不影响工单 ticketId={}", ticket.getId(), e);
            }
        }

        log.info("工单创建成功 id={} ticketNo={}", ticket.getId(), ticket.getTicketNo());
        return ticket.getId();
    }

    private void insertTicketWithRetry(TicketEntity ticket, Long mailboxId, String customerEmail, String subject) {
        for (int attempt = 1; attempt <= MAX_TICKET_INSERT_ATTEMPTS; attempt++) {
            String ticketNo = ticketNumberRuleService.generateNextTicketNo();
            ticket.setId(null);
            ticket.setTicketNo(ticketNo);
            log.info("生成工单号 ticketNo={} mailboxId={} customer={} subject={}", ticketNo, mailboxId, customerEmail, subject);
            try {
                ticketMapper.insert(ticket);
                return;
            } catch (DuplicateKeyException exception) {
                if (attempt >= MAX_TICKET_INSERT_ATTEMPTS) {
                    throw new BusinessException(CODE_BAD_REQUEST, "工单号生成冲突，请稍后重试");
                }
                log.warn("工单号入库冲突，重新生成 ticketNo={} attempt={}", ticketNo, attempt);
            }
        }
    }

    // ==================== 客户追信回流 ====================

    /**
     * 客户发来追信邮件时处理：
     * <ul>
     *   <li>将邮件保存为 INBOUND 消息到工单会话</li>
     *   <li>CLOSED/CANCELLED → 自动重新开启为 PROCESSING</li>
     *   <li>WAITING_CUSTOMER → 自动转为 PROCESSING</li>
     *   <li>其他状态 → 只加消息，不改状态</li>
     * </ul>
     *
     * @param ticketId     已有工单 ID
     * @param subject      邮件主题
     * @param fromAddress  发件人地址（客户）
     * @param contentText  纯文本正文
     * @param contentHtml  HTML 正文
     * @param messageId    邮件 Message-ID
     * @param inReplyTo    In-Reply-To头
     * @param references   References头
     * @param sentAt       邮件发送时间
     */
    public void handleCustomerFollowUp(Long ticketId, String subject, String fromAddress,
                                       String contentText, String contentHtml,
                                       String messageId, String inReplyTo, String references,
                                       LocalDateTime sentAt) {
        handleCustomerFollowUp(
                ticketId, subject, fromAddress, contentText, contentHtml,
                messageId, inReplyTo, references, sentAt,
                List.of(), List.of(), List.of(), null, null, List.of()
        );
    }

    @Transactional
    public void handleCustomerFollowUp(Long ticketId, String subject, String fromAddress,
                                       String contentText, String contentHtml,
                                       String messageId, String inReplyTo, String references,
                                       LocalDateTime sentAt,
                                       List<String> toAddresses, List<String> ccAddresses, List<String> bccAddresses,
                                       String rawHeaders, byte[] rawEml, List<AttachmentInfo> attachments) {
        TicketEntity ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            log.warn("客户追信跳过：工单不存在 ticketId={}", ticketId);
            return;
        }

        // 1、保存客户追信消息
        TicketMessageEntity msg = new TicketMessageEntity();
        msg.setTicketId(ticket.getId());
        msg.setDirection(DIRECTION_INBOUND);
        msg.setFromAddress(fromAddress);
        msg.setToAddress(joinAddresses(toAddresses));
        msg.setToAddresses(joinAddresses(toAddresses));
        msg.setCcAddresses(joinAddresses(ccAddresses));
        msg.setBccAddresses(joinAddresses(bccAddresses));
        msg.setSubject(subject);
        msg.setContentText(contentText);
        msg.setContentHtml(contentHtml);
        msg.setMessageId(MessageThreadService.normalizeMessageId(messageId));
        msg.setInReplyTo(MessageThreadService.normalizeMessageId(inReplyTo));
        msg.setMailReferences(references);
        msg.setRawHeaders(rawHeaders);
        saveRawEml(msg, messageId, rawEml);
        msg.setSentAt(sentAt != null ? sentAt : LocalDateTime.now());
        msg.setCreatedBy(OPERATOR_SYSTEM);
        msg.setUpdatedBy(OPERATOR_SYSTEM);
        ticketMessageMapper.insert(msg);
        String renderedHtml = saveIncomingAttachments(ticket.getId(), msg.getId(), contentHtml, attachments);
        if (renderedHtml != null && !renderedHtml.equals(contentHtml)) {
            msg.setContentHtml(renderedHtml);
            ticketMessageMapper.updateById(msg);
        }

        // 2、更新客户最后来信时间；状态事件仍记录为系统处理时间。
        LocalDateTime customerMailAt = msg.getSentAt();
        LocalDateTime now = LocalDateTime.now();
        updateTicket(ticket.getId(), Map.of("last_customer_mail_at", customerMailAt));

        // 3、状态流转
        String oldStatus = ticket.getStatus();
        if (STATUS_CLOSED.equals(oldStatus) || STATUS_CANCELLED.equals(oldStatus)) {
            // 关闭后追信 → 重新开启
            updateTicketStatus(ticket.getId(), STATUS_PROCESSING);
            recordEvent(ticket.getId(), EVENT_REOPENED,
                    "客户追信，工单重新开启（原状态：" + statusLabel(oldStatus) + "）", OPERATOR_SYSTEM, now);
            log.info("客户追信：工单重新开启 ticketId={} ticketNo={} oldStatus={}",
                    ticket.getId(), ticket.getTicketNo(), oldStatus);
        } else if (STATUS_WAITING_CUSTOMER.equals(oldStatus)) {
            // 等待客户回复中 → 客户已回复，自动转处理中
            updateTicketStatus(ticket.getId(), STATUS_PROCESSING);
            recordEvent(ticket.getId(), EVENT_CUSTOMER_FOLLOWUP,
                    "客户已回复，工单自动转为处理中", OPERATOR_SYSTEM, now);
            log.info("客户追信：工单转为处理中 ticketId={} ticketNo={}", ticket.getId(), ticket.getTicketNo());
        } else {
            // 其他状态只记录消息，不作流转
            recordEvent(ticket.getId(), EVENT_CUSTOMER_FOLLOWUP,
                    "客户追信，新增会话消息", OPERATOR_SYSTEM, now);
            log.info("客户追信：新增消息 ticketId={} ticketNo={} status={}",
                    ticket.getId(), ticket.getTicketNo(), oldStatus);
        }
    }

    private void saveRawEml(TicketMessageEntity msg, String messageId, byte[] rawEml) {
        if (rawEml == null || rawEml.length == 0) {
            msg.setRawEmlSize(0L);
            return;
        }
        String objectKey = fileStorageService.upload(
                buildRawEmlFileName(messageId),
                rawEml.length,
                "message/rfc822",
                new ByteArrayInputStream(rawEml)
        );
        msg.setRawEmlObjectKey(objectKey);
        msg.setRawEmlSize((long) rawEml.length);
    }

    private String saveIncomingAttachments(Long ticketId, Long messageId, String contentHtml, List<AttachmentInfo> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return contentHtml;
        }

        String renderedHtml = contentHtml;
        int savedCount = 0;
        for (AttachmentInfo attachment : attachments) {
            byte[] content = attachment.content();
            if (content == null || content.length == 0) {
                log.warn("来信附件内容为空，完整内容已保留在原始EML中 ticketId={} messageId={} fileName={}",
                        ticketId, messageId, attachment.fileName());
                continue;
            }

            String fileName = attachment.fileName() != null && !attachment.fileName().isBlank()
                    ? attachment.fileName()
                    : "attachment";
            String objectKey = fileStorageService.upload(
                    fileName,
                    content.length,
                    attachment.contentType(),
                    new ByteArrayInputStream(content)
            );

            TicketAttachmentEntity entity = new TicketAttachmentEntity();
            entity.setTicketId(ticketId);
            entity.setMessageId(messageId);
            entity.setFileName(fileName);
            entity.setFileSize((long) content.length);
            entity.setContentType(attachment.contentType());
            entity.setObjectKey(objectKey);
            entity.setIsInline(attachment.isInline());
            entity.setContentId(attachment.contentId());
            entity.setUploadedBy(OPERATOR_SYSTEM);
            entity.setCreatedAt(LocalDateTime.now());
            ticketAttachmentMapper.insert(entity);
            savedCount++;

            if (renderedHtml != null && attachment.isInline() && attachment.contentId() != null) {
                String url = buildAttachmentDownloadUrl(ticketId, entity.getId());
                renderedHtml = renderedHtml
                        .replace("cid:" + attachment.contentId(), url)
                        .replace("cid:<" + attachment.contentId() + ">", url);
            }
        }

        if (savedCount > 0) {
            log.info("来信附件保存完成 ticketId={} messageId={} count={}", ticketId, messageId, savedCount);
        }
        return renderedHtml;
    }

    private String joinAddresses(List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        String joined = addresses.stream()
                .filter(address -> address != null && !address.isBlank())
                .collect(Collectors.joining(","));
        return joined.isBlank() ? null : joined;
    }

    private String buildRawEmlFileName(String messageId) {
        String normalized = MessageThreadService.normalizeMessageId(messageId);
        if (normalized == null || normalized.isBlank()) {
            return "mailtrace-original.eml";
        }
        return normalized.replaceAll("[^A-Za-z0-9._-]", "_") + ".eml";
    }

    private String buildRawEmlDownloadFileName(String ticketNo, Long messageId) {
        String prefix = ticketNo != null && !ticketNo.isBlank() ? ticketNo : "ticket";
        String suffix = messageId != null ? String.valueOf(messageId) : "message";
        return (prefix + "-" + suffix).replaceAll("[^A-Za-z0-9._-]", "_") + ".eml";
    }

    // ==================== 分页查询 ====================

    /**
     * 工单分页查询，ADMIN/AGENT 均可查看。
     */
    public TicketPageResponse pageTickets(CurrentUserPrincipal principal, String keyword, String status,
                                          Boolean slaBreached, Long assigneeId, Long mailboxId,
                                          LocalDateTime createdFrom, LocalDateTime createdTo,
                                          Integer page, Integer size) {
        permissionService.assertPermission(principal, "ticket:read", "无权查看工单");

        long currentPage = normalizePage(page);
        long pageSize = normalizeSize(size);
        LambdaQueryWrapper<TicketEntity> wrapper = buildPageQuery(keyword, status, slaBreached, assigneeId, mailboxId, createdFrom, createdTo);
        dataScopeService.applyTicketScope(wrapper, principal);
        wrapper.orderByDesc(TicketEntity::getCreatedAt)
                .orderByDesc(TicketEntity::getId);

        Page<TicketEntity> result = ticketMapper.selectPage(Page.of(currentPage, pageSize), wrapper);
        List<TicketSummaryVO> records = result.getRecords().stream()
                .map(this::toSummaryVO)
                .toList();

        return new TicketPageResponse(
                records, result.getTotal(), result.getCurrent(),
                result.getSize(), result.getPages()
        );
    }

    // ==================== 工单详情 ====================

    /**
     * 工单详情，含邮件消息列表和生命周期事件。
     */
    public TicketVO getTicket(CurrentUserPrincipal principal, Long id) {
        permissionService.assertPermission(principal, "ticket:read", "无权查看工单");
        TicketEntity ticket = requireTicket(id);
        dataScopeService.assertTicketVisible(principal, ticket);
        return toDetailVO(ticket);
    }

    public RawMailDownload downloadRawEml(CurrentUserPrincipal principal, Long ticketId, Long messageId) {
        permissionService.assertPermission(principal, "ticket:read", "无权查看工单");
        TicketEntity ticket = requireTicket(ticketId);
        dataScopeService.assertTicketVisible(principal, ticket);
        if (messageId == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "消息ID不能为空");
        }
        TicketMessageEntity message = ticketMessageMapper.selectById(messageId);
        if (message == null || !ticketId.equals(message.getTicketId())) {
            throw new BusinessException(CODE_NOT_FOUND, "邮件消息不存在");
        }
        if (message.getRawEmlObjectKey() == null || message.getRawEmlObjectKey().isBlank()) {
            throw new BusinessException(CODE_NOT_FOUND, "原始邮件不存在");
        }
        return new RawMailDownload(
                buildRawEmlDownloadFileName(ticket.getTicketNo(), message.getId()),
                message.getRawEmlSize(),
                fileStorageService.download(message.getRawEmlObjectKey())
        );
    }

    // ==================== 分配处理人 ====================

    @Transactional
    public TicketVO assignTicket(CurrentUserPrincipal principal, Long id, TicketAssignRequest request) {
        permissionService.assertPermission(principal, "ticket:assign", "无权转派工单");
        TicketEntity ticket = requireTicket(id);
        dataScopeService.assertTicketOperable(principal, ticket);
        assertActive(ticket);

        if (request.assigneeId() == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "请指定处理人");
        }
        UserEntity assignee = userMapper.selectById(request.assigneeId());
        if (assignee == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "处理人不存在");
        }

        // 更新处理人
        updateTicket(ticket.getId(), Map.of(
                "assignee_id", request.assigneeId()
        ));

        // 如果当前是待分配状态，改为处理中
        if (STATUS_PENDING_ASSIGN.equals(ticket.getStatus())) {
            updateTicketStatus(ticket.getId(), STATUS_PROCESSING);
        }

        String reason = normalize(request.reason());
        String content = "转派处理人：" + assignee.getDisplayName()
                + (reason.isEmpty() ? "" : "；原因：" + truncateContent(reason));
        recordEvent(ticket.getId(), EVENT_ASSIGNED, content, principal.account(), LocalDateTime.now());

        // 更新内存中的 ticket 状态用于返回
        ticket.setAssigneeId(request.assigneeId());
        if (STATUS_PENDING_ASSIGN.equals(ticket.getStatus())) {
            ticket.setStatus(STATUS_PROCESSING);
        }
        recordLog(principal, "ASSIGN", ticket.getId(), content);

        // 发送分配通知给处理人
        boolean notifyAssignee = request.notifyAssignee() == null || Boolean.TRUE.equals(request.notifyAssignee());
        MailboxEntity mailbox = mailboxMapper.selectById(ticket.getMailboxId());
        if (notifyAssignee && mailbox != null) {
            sendAssignNotify(ticket, mailbox, assignee);
        }

        return toDetailVO(ticketMapper.selectById(id));
    }

    // ==================== 领取未分配工单 ====================

    @Transactional
    public TicketVO claimTicket(CurrentUserPrincipal principal, Long id) {
        permissionService.assertPermission(principal, "ticket:claim", "无权领取工单");
        TicketEntity ticket = requireTicket(id);
        dataScopeService.assertTicketVisible(principal, ticket);
        assertActive(ticket);
        if (principal.id() == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "当前用户缺少用户ID，无法领取工单");
        }
        if (ticket.getAssigneeId() != null) {
            throw new BusinessException(CODE_BAD_REQUEST, "工单已分配，不能领取");
        }

        LambdaUpdateWrapper<TicketEntity> updateWrapper = new LambdaUpdateWrapper<TicketEntity>()
                .eq(TicketEntity::getId, ticket.getId())
                .isNull(TicketEntity::getAssigneeId)
                .set(TicketEntity::getAssigneeId, principal.id())
                .set(TicketEntity::getUpdatedBy, principal.account());
        if (STATUS_PENDING_ASSIGN.equals(ticket.getStatus())) {
            updateWrapper.set(TicketEntity::getStatus, STATUS_PROCESSING);
        }
        int updated = ticketMapper.update(null, updateWrapper);
        if (updated == 0) {
            throw new BusinessException(CODE_BAD_REQUEST, "工单已被领取或已分配");
        }

        String operatorName = principal.displayName() == null || principal.displayName().isBlank()
                ? principal.account()
                : principal.displayName();
        String content = "领取未分配工单：" + operatorName;
        recordEvent(ticket.getId(), EVENT_ASSIGNED, content, principal.account(), LocalDateTime.now());
        recordLog(principal, "CLAIM", ticket.getId(), content);

        return toDetailVO(ticketMapper.selectById(id));
    }

    // ==================== 工单统计 ====================

    public record TicketStats(long totalCount, long pendingAssignCount, long processingCount, long waitingCustomerCount, long slaOverdueCount, long closedTodayCount) {}

    public TicketStats stats(CurrentUserPrincipal principal) {
        permissionService.assertPermission(principal, "ticket:read", "无权查看工单统计");
        return new TicketStats(
                countTickets(principal, null, null, false),
                countTickets(principal, STATUS_PENDING_ASSIGN, null, false),
                countTickets(principal, STATUS_PROCESSING, null, false),
                countTickets(principal, STATUS_WAITING_CUSTOMER, null, false),
                countTickets(principal, null, true, false),
                countTickets(principal, STATUS_CLOSED, null, true)
        );
    }

    // ==================== 回复客户 / 内部备注 ====================

    @Transactional
    public TicketVO replyTicket(CurrentUserPrincipal principal, Long id, TicketReplyRequest request) {
        boolean isInternal = Boolean.TRUE.equals(request.internal());
        permissionService.assertPermission(principal, isInternal ? "ticket:note" : "ticket:reply",
                isInternal ? "无权添加内部备注" : "无权回复客户");
        TicketEntity ticket = requireTicket(id);
        dataScopeService.assertTicketOperable(principal, ticket);
        assertActive(ticket);

        String content = normalize(request.content());
        if (content.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "回复内容不能为空");
        }

        String direction = isInternal ? DIRECTION_INTERNAL : DIRECTION_OUTBOUND;
        String messageSubject = isInternal ? ticket.getSubject() : buildAgentReplySubject(ticket);

        // 记录消息
        TicketMessageEntity message = new TicketMessageEntity();
        message.setTicketId(ticket.getId());
        message.setDirection(direction);
        message.setFromAddress(null);
        message.setToAddress(ticket.getCustomerEmail());
        message.setSubject(messageSubject);
        message.setContentText(content);
        if (request.htmlContent() != null && !request.htmlContent().isBlank()) {
            message.setContentHtml(request.htmlContent());
        }
        message.setSentAt(LocalDateTime.now());
        message.setOperatorId(principal.id());
        message.setCreatedBy(principal.account());
        message.setUpdatedBy(principal.account());
        ticketMessageMapper.insert(message);

        // 关联附件——直接创建记录
        var replyAttachments = request.attachments();
        if (replyAttachments != null && !replyAttachments.isEmpty()) {
            for (var att : replyAttachments) {
                TicketAttachmentEntity entity = new TicketAttachmentEntity();
                entity.setTicketId(ticket.getId());
                entity.setMessageId(message.getId());
                entity.setObjectKey(att.objectKey());
                entity.setFileName(att.fileName() != null ? att.fileName() : att.objectKey());
                entity.setFileSize(att.fileSize() != null ? att.fileSize() : 0);
                entity.setContentType(att.contentType());
                entity.setIsInline(false);
                entity.setUploadedBy(principal.account());
                entity.setCreatedAt(LocalDateTime.now());
                ticketAttachmentMapper.insert(entity);
            }
            log.info("回复关联附件 {} 个 ticketId={} messageId={}", replyAttachments.size(), ticket.getId(), message.getId());
        }

        if (isInternal) {
            // 内部备注：不改变工单状态，不记录首次响应
            recordEvent(ticket.getId(), EVENT_INTERNAL_NOTE, "内部备注：" + truncateContent(content), principal.account(), LocalDateTime.now());
            log.info("内部备注 ticketId={} operator={}", ticket.getId(), principal.account());
        } else {
            // 对外回复：更新状态为 WAITING_CUSTOMER（如非终态）
            if (!STATUS_CLOSED.equals(ticket.getStatus()) && !STATUS_CANCELLED.equals(ticket.getStatus())) {
                updateTicketStatus(ticket.getId(), STATUS_WAITING_CUSTOMER);
            }

            // 记录首次响应时间
            if (ticket.getFirstReplyAt() == null) {
                LocalDateTime now = LocalDateTime.now();
                updateTicket(ticket.getId(), Map.of("first_reply_at", now));
                recordEvent(ticket.getId(), EVENT_FIRST_REPLY, "首次对外回复客户", principal.account(), now);
            }

            LocalDateTime now = LocalDateTime.now();
            updateTicket(ticket.getId(), Map.of("last_agent_reply_at", now));
            recordEvent(ticket.getId(), EVENT_AGENT_REPLY, "回复客户：" + truncateContent(content), principal.account(), now);

            // SMTP 发送回复邮件给客户
            try {
                MailboxEntity mailbox = mailboxMapper.selectById(ticket.getMailboxId());
                if (mailbox != null) {
                    String htmlContent = request.htmlContent() != null && !request.htmlContent().isBlank() ? request.htmlContent() : content;
                    MailSendService.SendResult sendResult = mailSendService.sendRawMail(
                            mailbox.getId(), ticket.getCustomerEmail(), messageSubject, htmlContent, "AGENT_REPLY");
                    if (sendResult.success()) {
                        String sentMessageId = MessageThreadService.normalizeMessageId(sendResult.messageId());
                        if (sentMessageId != null) {
                            message.setMessageId(sentMessageId);
                            message.setUpdatedBy(principal.account());
                            ticketMessageMapper.updateById(message);
                        }
                        log.info("回复邮件已发送 ticketId={} to={} messageId={}",
                                ticket.getId(), ticket.getCustomerEmail(), sentMessageId);
                    } else {
                        log.warn("回复邮件发送失败 ticketId={} reason={}", ticket.getId(), sendResult.message());
                    }
                } else {
                    log.warn("回复邮件跳过：邮箱配置不存在 mailboxId={}", ticket.getMailboxId());
                }
            } catch (Exception e) {
                log.warn("回复邮件发送异常，不影响工单 ticketId={}", ticket.getId(), e);
            }

            log.info("对外回复 ticketId={} operator={}", ticket.getId(), principal.account());
        }

        recordLog(principal, isInternal ? "INTERNAL_NOTE" : "REPLY", ticket.getId(),
                (isInternal ? "内部备注：" : "回复客户：") + truncateContent(content));

        return toDetailVO(ticketMapper.selectById(id));
    }

    private String buildAgentReplySubject(TicketEntity ticket) {
        String ticketNo = ticket.getTicketNo() == null ? "" : ticket.getTicketNo();
        String subject = ticket.getSubject() == null ? "" : ticket.getSubject();
        return "关于工单 " + ticketNo + " 的回复：" + subject;
    }

    // ==================== 变更状态 ====================

    @Transactional
    public TicketVO updateStatus(CurrentUserPrincipal principal, Long id, TicketStatusRequest request) {
        return updateStatusInternal(principal, id, request, "ticket:update_status", "无权变更工单状态");
    }

    private TicketVO updateStatusInternal(CurrentUserPrincipal principal, Long id, TicketStatusRequest request,
                                          String permissionCode, String forbiddenMessage) {
        permissionService.assertPermission(principal, permissionCode, forbiddenMessage);
        TicketEntity ticket = requireTicket(id);
        dataScopeService.assertTicketOperable(principal, ticket);
        assertActive(ticket);

        String newStatus = normalize(request.status()).toUpperCase();
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new BusinessException(CODE_BAD_REQUEST, "不支持的状态：" + newStatus);
        }
        if (STATUS_PENDING_ASSIGN.equals(newStatus) || STATUS_WAITING_CUSTOMER.equals(newStatus)) {
            throw new BusinessException(CODE_BAD_REQUEST, "不能手动变更为待分配或待客户回复状态");
        }

        String oldStatus = ticket.getStatus();
        updateTicketStatus(ticket.getId(), newStatus);

        String reason = normalize(request.reason());
        String statusChangeContent = "状态变更：" + statusLabel(oldStatus) + " → " + statusLabel(newStatus);
        if (STATUS_CLOSED.equals(newStatus)) {
            updateTicket(ticket.getId(), Map.of("closed_at", LocalDateTime.now()));
            recordEvent(ticket.getId(), EVENT_CLOSED,
                    statusChangeContent + "；工单已关闭" + (reason.isEmpty() ? "" : "；说明：" + truncateContent(reason)),
                    principal.account(), LocalDateTime.now());
        } else if (STATUS_CANCELLED.equals(newStatus)) {
            recordEvent(ticket.getId(), EVENT_CANCELLED,
                    statusChangeContent + "；工单已取消" + (reason.isEmpty() ? "" : "；说明：" + truncateContent(reason)),
                    principal.account(), LocalDateTime.now());
        } else {
            recordEvent(ticket.getId(), EVENT_STATUS_CHANGED, statusChangeContent,
                    principal.account(), LocalDateTime.now());
        }

        recordLog(principal, "STATUS_CHANGE", ticket.getId(), "工单" + statusChangeContent);
        return toDetailVO(ticketMapper.selectById(id));
    }

    // ==================== 变更优先级 ====================

    @Transactional
    public TicketVO updatePriority(CurrentUserPrincipal principal, Long id, TicketPriorityRequest request) {
        permissionService.assertPermission(principal, "ticket:update_priority", "无权变更工单优先级");
        TicketEntity ticket = requireTicket(id);
        dataScopeService.assertTicketOperable(principal, ticket);
        assertActive(ticket);

        String newPriority = normalize(request.priority()).toUpperCase();
        if (!Set.of("LOW", "NORMAL", "HIGH", "URGENT").contains(newPriority)) {
            throw new BusinessException(CODE_BAD_REQUEST, "不支持的优先级：" + newPriority);
        }

        String oldPriority = ticket.getPriority();
        ticketMapper.update(null, new LambdaUpdateWrapper<TicketEntity>()
                .eq(TicketEntity::getId, ticket.getId())
                .set(TicketEntity::getPriority, newPriority)
                .set(TicketEntity::getUpdatedBy, principal.account()));

        String priorityLabel = priorityLabel(oldPriority);
        String newLabel = priorityLabel(newPriority);
        String reason = normalize(request.reason());
        String content = "优先级变更：" + priorityLabel + " → " + newLabel
                + (reason.isEmpty() ? "" : "；说明：" + truncateContent(reason));
        recordEvent(ticket.getId(), EVENT_PRIORITY_CHANGED, content,
                principal.account(), LocalDateTime.now());
        recordLog(principal, "PRIORITY_CHANGE", ticket.getId(), "工单" + content);

        return toDetailVO(ticketMapper.selectById(id));
    }

    // ==================== 更新备注 ====================

    @Transactional
    public TicketVO updateRemark(CurrentUserPrincipal principal, Long id, TicketRemarkRequest request) {
        permissionService.assertPermission(principal, "ticket:update_remark", "无权编辑工单备注");
        TicketEntity ticket = requireTicket(id);
        dataScopeService.assertTicketOperable(principal, ticket);
        ticketMapper.update(null, new LambdaUpdateWrapper<TicketEntity>()
                .eq(TicketEntity::getId, ticket.getId())
                .set(TicketEntity::getRemark, request.remark())
                .set(TicketEntity::getUpdatedBy, principal.account()));
        return toDetailVO(ticketMapper.selectById(id));
    }

    // ==================== 关闭工单 ====================

    @Transactional
    public TicketVO closeTicket(CurrentUserPrincipal principal, Long id, TicketStatusRequest request) {
        String reason = request == null ? null : request.reason();
        return updateStatusInternal(principal, id, new TicketStatusRequest("CLOSED", reason),
                "ticket:close", "无权关闭工单");
    }

    // ==================== 内部方法 ====================

    private void autoAssignByRules(TicketEntity ticket, Long mailboxId, String subject, String customerEmail) {
        // 1、加载来源邮箱，用于 MAILBOX 规则匹配、通知发信和旧逻辑兜底。
        MailboxEntity mailbox = mailboxMapper.selectById(mailboxId);
        AssignmentRuleMatchResult matchResult = assignmentRuleService.matchForTicket(
                mailboxId, mailbox == null ? null : mailbox.getEmailAddress(), subject, customerEmail);

        // 2、优先应用启用的分配规则：按 priority_order 命中的第一条有效规则生效。
        if (matchResult != null) {
            ticket.setAssigneeId(matchResult.assigneeId());
            ticket.setStatus(STATUS_PROCESSING);
            ticketMapper.updateById(ticket);

            String content = "自动分配处理人：" + matchResult.assigneeName()
                    + "；命中规则：" + matchResult.ruleName()
                    + "（" + matchResult.matchType() + "）";
            recordEvent(ticket.getId(), EVENT_ASSIGNED, content, OPERATOR_SYSTEM, LocalDateTime.now());
            log.info("自动分配处理人 ticketId={} assignee={} ruleId={} ruleName={}",
                    ticket.getId(), matchResult.assigneeName(), matchResult.ruleId(), matchResult.ruleName());

            // 3、规则允许通知且邮箱可用时，发送分配通知。
            if (Boolean.TRUE.equals(matchResult.notifyEnabled()) && mailbox != null) {
                sendAssignNotify(ticket, mailbox, matchResult.assigneeId(),
                        matchResult.assigneeName(), matchResult.assigneeEmail());
            }
            return;
        }

        // 4、没有规则命中时，保留原邮箱默认处理人逻辑，避免无配置场景回退。
        autoAssignByMailboxDefault(ticket, mailbox, mailboxId);
    }

    private void applySlaDeadlines(TicketEntity ticket, LocalDateTime mailSentAt) {
        // 1、以客户来信时间作为 SLA 起算时间；缺失时使用当前建单时间兜底。
        LocalDateTime startAt = mailSentAt != null ? mailSentAt : LocalDateTime.now();
        ticket.setLastCustomerMailAt(startAt);
        try {
            // 2、调用 SLA 计算服务，写入命中策略和首次响应/解决截止时间。
            SlaDeadlineResult deadline = slaDeadlineService.calculateForNewTicket(startAt);
            ticket.setSlaPolicyId(deadline.policyId());
            ticket.setSlaResponseDeadline(deadline.responseDeadline());
            ticket.setSlaResolveDeadline(deadline.resolveDeadline());
        } catch (Exception exception) {
            // 3、SLA 配置异常不阻断收信建单，后续可由配置检查或提醒任务补偿处理。
            log.warn("SLA 截止时间计算失败，跳过本次 SLA 写入 ticketNo={} startAt={}",
                    ticket.getTicketNo(), startAt, exception);
        }
    }

    private void autoAssignByMailboxDefault(TicketEntity ticket, MailboxEntity mailbox, Long mailboxId) {
        // 1、邮箱不存在或未配置默认处理人时，工单保持待分配。
        if (mailbox == null || mailbox.getDefaultAssigneeId() == null) {
            log.info("邮箱无默认处理人，工单保持待分配 ticketId={} mailboxId={}", ticket.getId(), mailboxId);
            return;
        }

        // 2、默认处理人必须存在且启用，否则继续保持待分配。
        UserEntity assignee = userMapper.selectById(mailbox.getDefaultAssigneeId());
        if (assignee == null || !Boolean.TRUE.equals(assignee.getEnabled())) {
            log.info("默认处理人无效或已停用，工单保持待分配 ticketId={} assigneeId={}", ticket.getId(), mailbox.getDefaultAssigneeId());
            return;
        }

        // 3、回写处理人和处理中状态，并记录自动分配事件。
        ticket.setAssigneeId(mailbox.getDefaultAssigneeId());
        ticket.setStatus(STATUS_PROCESSING);
        ticketMapper.updateById(ticket);

        recordEvent(ticket.getId(), EVENT_ASSIGNED,
                "自动分配处理人：" + assignee.getDisplayName() + "；来源：邮箱默认处理人",
                OPERATOR_SYSTEM, LocalDateTime.now());
        log.info("自动分配处理人 ticketId={} assignee={}", ticket.getId(), assignee.getDisplayName());

        // 4、发送分配通知。
        sendAssignNotify(ticket, mailbox, assignee);
    }

    /**
     * 发送分配通知邮件给处理人。
     */
    private void sendAssignNotify(TicketEntity ticket, MailboxEntity mailbox, UserEntity assignee) {
        sendAssignNotify(ticket, mailbox, assignee.getId(), assignee.getDisplayName(), assignee.getEmail());
    }

    private void sendAssignNotify(TicketEntity ticket, MailboxEntity mailbox,
                                  Long assigneeId, String assigneeName, String assigneeEmail) {
        if (assigneeEmail == null || assigneeEmail.isBlank()) {
            log.info("分配通知跳过：处理人无邮箱 ticketId={} assigneeId={}", ticket.getId(), assigneeId);
            return;
        }
        String subject = "新工单分配：" + ticket.getTicketNo() + " " + ticket.getSubject();
        String priorityLabel = switch (ticket.getPriority()) {
            case "HIGH" -> "高";
            case "URGENT" -> "紧急";
            default -> "普通";
        };
        String content = "您好，" + assigneeName + "，\n\n"
                + "系统已将以下工单分配给您，请及时处理：\n\n"
                + "工单号：" + ticket.getTicketNo() + "\n"
                + "主题：" + ticket.getSubject() + "\n"
                + "客户：" + ticket.getCustomerEmail() + "\n"
                + "优先级：" + priorityLabel + "\n\n"
                + "请登录系统查看详情。";

        MailSendService.SendResult result = mailSendService.sendRawMail(
                mailbox.getId(), assigneeEmail, subject, content, "ASSIGN_NOTIFY");
        if (result.success()) {
            log.info("分配通知已发送 ticketId={} to={}", ticket.getId(), assigneeEmail);
        } else {
            log.warn("分配通知发送失败 ticketId={} reason={}", ticket.getId(), result.message());
        }
    }

    private void recordEvent(Long ticketId, String eventType, String content, String operator, LocalDateTime eventAt) {
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

    private void updateTicketStatus(Long ticketId, String newStatus) {
        ticketMapper.update(null, new LambdaUpdateWrapper<TicketEntity>()
                .eq(TicketEntity::getId, ticketId)
                .set(TicketEntity::getStatus, newStatus)
                .set(TicketEntity::getUpdatedBy, "system"));
    }

    private void updateTicket(Long ticketId, Map<String, Object> fields) {
        TicketEntity update = new TicketEntity();
        update.setId(ticketId);
        if (fields.containsKey("assignee_id")) {
            update.setAssigneeId((Long) fields.get("assignee_id"));
        }
        if (fields.containsKey("first_reply_at")) {
            update.setFirstReplyAt((LocalDateTime) fields.get("first_reply_at"));
        }
        if (fields.containsKey("last_agent_reply_at")) {
            update.setLastAgentReplyAt((LocalDateTime) fields.get("last_agent_reply_at"));
        }
        if (fields.containsKey("last_customer_mail_at")) {
            update.setLastCustomerMailAt((LocalDateTime) fields.get("last_customer_mail_at"));
        }
        if (fields.containsKey("closed_at")) {
            update.setClosedAt((LocalDateTime) fields.get("closed_at"));
        }
        update.setUpdatedBy("system");
        ticketMapper.updateById(update);
    }

    private LambdaQueryWrapper<TicketEntity> buildPageQuery(String keyword, String status, Boolean slaBreached,
                                                            Long assigneeId, Long mailboxId,
                                                            LocalDateTime createdFrom, LocalDateTime createdTo) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<>();
        String normalizedKeyword = normalize(keyword);
        if (!normalizedKeyword.isEmpty()) {
            wrapper.and(q -> q
                    .like(TicketEntity::getTicketNo, normalizedKeyword)
                    .or()
                    .like(TicketEntity::getSubject, normalizedKeyword)
                    .or()
                    .like(TicketEntity::getCustomerEmail, normalizedKeyword));
        }
        String normalizedStatus = normalize(status).toUpperCase();
        if (VALID_STATUSES.contains(normalizedStatus)) {
            wrapper.eq(TicketEntity::getStatus, normalizedStatus);
        }
        if (slaBreached != null) {
            wrapper.eq(TicketEntity::getSlaBreached, slaBreached);
        }
        if (assigneeId != null) {
            wrapper.eq(TicketEntity::getAssigneeId, assigneeId);
        }
        if (mailboxId != null) {
            wrapper.eq(TicketEntity::getMailboxId, mailboxId);
        }
        if (createdFrom != null) {
            wrapper.ge(TicketEntity::getCreatedAt, createdFrom);
        }
        if (createdTo != null) {
            wrapper.le(TicketEntity::getCreatedAt, createdTo);
        }
        return wrapper;
    }

    private long countTickets(CurrentUserPrincipal principal, String status, Boolean slaBreached, boolean closedToday) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<>();
        dataScopeService.applyTicketScope(wrapper, principal);
        if (status != null) {
            wrapper.eq(TicketEntity::getStatus, status);
        }
        if (slaBreached != null) {
            wrapper.eq(TicketEntity::getSlaBreached, slaBreached);
        }
        if (closedToday) {
            LocalDate today = LocalDate.now();
            wrapper.ge(TicketEntity::getClosedAt, today.atStartOfDay())
                    .lt(TicketEntity::getClosedAt, today.plusDays(1).atStartOfDay());
        }
        return ticketMapper.selectCount(wrapper);
    }

    private TicketEntity requireTicket(Long id) {
        if (id == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "工单ID不能为空");
        }
        TicketEntity ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException(CODE_NOT_FOUND, "工单不存在");
        }
        return ticket;
    }

    private void assertActive(TicketEntity ticket) {
        if (STATUS_CLOSED.equals(ticket.getStatus()) || STATUS_CANCELLED.equals(ticket.getStatus())) {
            throw new BusinessException(CODE_BAD_REQUEST, "工单已" + ("CLOSED".equals(ticket.getStatus()) ? "关闭" : "取消") + "，无法操作");
        }
    }

    private TicketSummaryVO toSummaryVO(TicketEntity ticket) {
        String assigneeName = resolveUserName(ticket.getAssigneeId());
        String mailboxName = resolveMailboxName(ticket.getMailboxId());
        return new TicketSummaryVO(
                ticket.getId(), ticket.getTicketNo(), ticket.getSubject(), ticket.getStatus(), ticket.getPriority(),
                ticket.getCustomerEmail(), ticket.getAssigneeId(), assigneeName,
                ticket.getMailboxId(), mailboxName,
                ticket.getLinkSuspect(), ticket.getFirstReplyAt() != null,
                ticket.getSlaResponseDeadline(), ticket.getSlaBreached(),
                ticket.getRemark(),
                ticket.getCreatedAt()
        );
    }

    private TicketVO toDetailVO(TicketEntity ticket) {
        String assigneeName = resolveUserName(ticket.getAssigneeId());
        String mailboxName = resolveMailboxName(ticket.getMailboxId());

        List<TicketAttachmentEntity> attachments = ticketAttachmentMapper.selectList(
                new LambdaQueryWrapper<TicketAttachmentEntity>()
                        .eq(TicketAttachmentEntity::getTicketId, ticket.getId())
        );
        Map<Long, List<TicketAttachmentEntity>> attachmentsByMessageId = attachments.stream()
                .filter(attachment -> attachment.getMessageId() != null)
                .collect(Collectors.groupingBy(TicketAttachmentEntity::getMessageId));

        List<TicketMessageVO> messages = ticketMessageMapper.selectList(
                new LambdaQueryWrapper<TicketMessageEntity>()
                        .eq(TicketMessageEntity::getTicketId, ticket.getId())
                        .orderByAsc(TicketMessageEntity::getCreatedAt)
        ).stream()
                .map(message -> toMessageVO(message, attachmentsByMessageId.getOrDefault(message.getId(), List.of())))
                .toList();

        List<TicketEventVO> events = ticketEventMapper.selectList(
                new LambdaQueryWrapper<TicketEventEntity>()
                        .eq(TicketEventEntity::getTicketId, ticket.getId())
                        .orderByAsc(TicketEventEntity::getEventAt)
        ).stream().map(this::toEventVO).toList();

        return new TicketVO(
                ticket.getId(), ticket.getTicketNo(), ticket.getSubject(), ticket.getStatus(), ticket.getPriority(),
                ticket.getMailboxId(), mailboxName,
                ticket.getCustomerId(), ticket.getCustomerEmail(),
                ticket.getAssigneeId(), assigneeName,
                ticket.getLinkSuspect(),
                ticket.getFirstReplyAt(), ticket.getClosedAt(),
                ticket.getSlaResponseDeadline(), ticket.getSlaResolveDeadline(),
                ticket.getSlaBreached(),
                ticket.getLastCustomerMailAt(), ticket.getLastAgentReplyAt(),
                ticket.getRemark(),
                ticket.getCreatedAt(), ticket.getUpdatedAt(),
                messages, events
        );
    }

    private TicketMessageVO toMessageVO(TicketMessageEntity message) {
        return toMessageVO(message, List.of());
    }

    private TicketMessageVO toMessageVO(TicketMessageEntity message, List<TicketAttachmentEntity> attachments) {
        String operatorName = null;
        if (message.getOperatorId() != null) {
            operatorName = resolveUserName(message.getOperatorId());
        }
        return new TicketMessageVO(
                message.getId(), message.getDirection(),
                message.getFromAddress(), message.getToAddress(),
                message.getToAddresses(), message.getCcAddresses(), message.getBccAddresses(),
                message.getSubject(), message.getContentText(), renderMessageHtml(message, attachments),
                message.getRawHeaders(), message.getRawEmlObjectKey(), message.getRawEmlSize(),
                message.getSentAt() != null ? message.getSentAt() : message.getCreatedAt(),
                operatorName, message.getCreatedAt()
        );
    }

    private String renderMessageHtml(TicketMessageEntity message, List<TicketAttachmentEntity> attachments) {
        String html = message.getContentHtml();
        if (html == null || html.isBlank() || attachments == null || attachments.isEmpty()) {
            return html;
        }
        String rendered = html;
        for (TicketAttachmentEntity attachment : attachments) {
            String url = buildAttachmentDownloadUrl(attachment.getTicketId(), attachment.getId());
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

    private String buildAttachmentDownloadUrl(Long ticketId, Long attachmentId) {
        return "/api/v1/tickets/" + ticketId + "/attachments/" + attachmentId + "/download";
    }

    private TicketEventVO toEventVO(TicketEventEntity event) {
        return new TicketEventVO(
                event.getId(), event.getEventType(), event.getEventContent(), event.getOperator(), event.getEventAt()
        );
    }

    private String resolveUserName(Long userId) {
        if (userId == null) return null;
        UserEntity user = userMapper.selectById(userId);
        return user == null ? null : user.getDisplayName();
    }

    private String resolveMailboxName(Long mailboxId) {
        if (mailboxId == null) return null;
        MailboxEntity mailbox = mailboxMapper.selectById(mailboxId);
        return mailbox == null ? null : mailbox.getMailboxName();
    }

    private void recordLog(CurrentUserPrincipal principal, String actionCode, Long bizId, String content) {
        OperationLogEntity log = new OperationLogEntity();
        log.setOperator(principal.account());
        log.setModuleCode(MODULE_TICKET);
        log.setActionCode(actionCode);
        log.setBizId(String.valueOf(bizId));
        log.setContent(content);
        log.setCreatedBy(principal.account());
        log.setUpdatedBy(principal.account());
        operationLogMapper.insert(log);
    }

    private String truncateContent(String content) {
        if (content == null) return "";
        return content.length() > 200 ? content.substring(0, 200) + "..." : content;
    }

    private String statusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case STATUS_PENDING_ASSIGN -> "待分配";
            case STATUS_PROCESSING -> "处理中";
            case STATUS_WAITING_CUSTOMER -> "待客户回复";
            case STATUS_CLOSED -> "已关闭";
            case STATUS_CANCELLED -> "已取消";
            default -> status;
        };
    }

    private String priorityLabel(String priority) {
        if (priority == null) return "";
        return switch (priority) {
            case "URGENT" -> "P1 紧急";
            case "HIGH" -> "P2 高";
            case "NORMAL" -> "P3 普通";
            case "LOW" -> "P4 低";
            default -> priority;
        };
    }

    private long normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long normalizeSize(Integer size) {
        if (size == null || size < 1) return 20;
        return Math.min(size, 100);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record RawMailDownload(String fileName, Long fileSize, InputStream inputStream) {
    }
}
