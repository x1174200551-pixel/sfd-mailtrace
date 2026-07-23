package com.ntn.fziot.mailtrace.application.bizservice.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.sysparam.TicketNumberRuleService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketAssignRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketEventVO;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketMessageVO;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketReplyRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketStatusRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketSummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEventEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketMessageEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketEventMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMessageMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public static final String EVENT_CLOSED = "CLOSED";
    public static final String EVENT_CANCELLED = "CANCELLED";

    // ---------- 消息方向 ----------
    public static final String DIRECTION_OUTBOUND = "OUTBOUND";
    public static final String DIRECTION_INTERNAL = "INTERNAL";

    // ---------- 权限常量 ----------
    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_FORBIDDEN = 40302;
    private static final int CODE_NOT_FOUND = 40401;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String OPERATOR_SYSTEM = "system";
    private static final String MODULE_TICKET = "TICKET";

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
        // 1、生成工单号
        String ticketNo = ticketNumberRuleService.generateNextTicketNo();
        log.info("生成工单号 ticketNo={} mailboxId={} customer={} subject={}", ticketNo, mailboxId, customerEmail, subject);

        // 2、创建工单
        TicketEntity ticket = new TicketEntity();
        ticket.setTicketNo(ticketNo);
        ticket.setSubject(subject);
        ticket.setStatus(STATUS_PENDING_ASSIGN);
        ticket.setPriority("NORMAL");
        ticket.setMailboxId(mailboxId);
        ticket.setCustomerEmail(customerEmail);
        ticket.setLinkSuspect(false);
        ticket.setSlaBreached(false);
        ticket.setSlaWarningSent(false);
        ticket.setSlaBreachNotified(false);
        ticket.setCreatedBy(OPERATOR_SYSTEM);
        ticket.setUpdatedBy(OPERATOR_SYSTEM);
        ticketMapper.insert(ticket);

        // 3、记录生命周期事件：CREATED
        recordEvent(ticket.getId(), EVENT_CREATED, "工单已创建，来源邮箱ID：" + mailboxId, OPERATOR_SYSTEM, LocalDateTime.now());

        // 4、尝试自动分配（取邮箱默认处理人）
        autoAssignByMailbox(ticket, mailboxId);

        log.info("工单创建成功 id={} ticketNo={}", ticket.getId(), ticketNo);
        return ticket.getId();
    }

    // ==================== 分页查询 ====================

    /**
     * 工单分页查询，ADMIN/AGENT 均可查看。
     */
    public TicketPageResponse pageTickets(CurrentUserPrincipal principal, String keyword, String status,
                                          Long assigneeId, Long mailboxId,
                                          LocalDateTime createdFrom, LocalDateTime createdTo,
                                          Integer page, Integer size) {
        assertAgentOrAdmin(principal);

        long currentPage = normalizePage(page);
        long pageSize = normalizeSize(size);
        LambdaQueryWrapper<TicketEntity> wrapper = buildPageQuery(keyword, status, assigneeId, mailboxId, createdFrom, createdTo)
                .orderByDesc(TicketEntity::getCreatedAt)
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
        assertAgentOrAdmin(principal);
        TicketEntity ticket = requireTicket(id);
        return toDetailVO(ticket);
    }

    // ==================== 分配处理人 ====================

    @Transactional
    public TicketVO assignTicket(CurrentUserPrincipal principal, Long id, TicketAssignRequest request) {
        assertAgentOrAdmin(principal);
        TicketEntity ticket = requireTicket(id);
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

        String content = "分配处理人：" + assignee.getDisplayName();
        recordEvent(ticket.getId(), EVENT_ASSIGNED, content, principal.account(), LocalDateTime.now());

        // 更新内存中的 ticket 状态用于返回
        ticket.setAssigneeId(request.assigneeId());
        if (STATUS_PENDING_ASSIGN.equals(ticket.getStatus())) {
            ticket.setStatus(STATUS_PROCESSING);
        }
        recordLog(principal, "ASSIGN", ticket.getId(), content);

        return toDetailVO(ticketMapper.selectById(id));
    }

    // ==================== 回复客户 / 内部备注 ====================

    @Transactional
    public TicketVO replyTicket(CurrentUserPrincipal principal, Long id, TicketReplyRequest request) {
        assertAgentOrAdmin(principal);
        TicketEntity ticket = requireTicket(id);
        assertActive(ticket);

        String content = normalize(request.content());
        if (content.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "回复内容不能为空");
        }

        boolean isInternal = Boolean.TRUE.equals(request.internal());
        String direction = isInternal ? DIRECTION_INTERNAL : DIRECTION_OUTBOUND;

        // 记录消息
        TicketMessageEntity message = new TicketMessageEntity();
        message.setTicketId(ticket.getId());
        message.setDirection(direction);
        message.setFromAddress(null);
        message.setToAddress(ticket.getCustomerEmail());
        message.setSubject(ticket.getSubject());
        message.setContentText(content);
        message.setOperatorId(principal.id());
        message.setCreatedBy(principal.account());
        message.setUpdatedBy(principal.account());
        ticketMessageMapper.insert(message);

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
            log.info("对外回复 ticketId={} operator={}", ticket.getId(), principal.account());
        }

        recordLog(principal, isInternal ? "INTERNAL_NOTE" : "REPLY", ticket.getId(),
                (isInternal ? "内部备注：" : "回复客户：") + truncateContent(content));

        return toDetailVO(ticketMapper.selectById(id));
    }

    // ==================== 变更状态 ====================

    @Transactional
    public TicketVO updateStatus(CurrentUserPrincipal principal, Long id, TicketStatusRequest request) {
        assertAgentOrAdmin(principal);
        TicketEntity ticket = requireTicket(id);
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

        if (STATUS_CLOSED.equals(newStatus)) {
            updateTicket(ticket.getId(), Map.of("closed_at", LocalDateTime.now()));
            recordEvent(ticket.getId(), EVENT_CLOSED, "工单已关闭", principal.account(), LocalDateTime.now());
        } else if (STATUS_CANCELLED.equals(newStatus)) {
            recordEvent(ticket.getId(), EVENT_CANCELLED, "工单已取消", principal.account(), LocalDateTime.now());
        }
        recordEvent(ticket.getId(), EVENT_STATUS_CHANGED, "状态变更：" + oldStatus + " → " + newStatus,
                principal.account(), LocalDateTime.now());

        recordLog(principal, "STATUS_CHANGE", ticket.getId(), "工单状态变更：" + oldStatus + " → " + newStatus);
        return toDetailVO(ticketMapper.selectById(id));
    }

    // ==================== 关闭工单 ====================

    @Transactional
    public TicketVO closeTicket(CurrentUserPrincipal principal, Long id) {
        return updateStatus(principal, id, new TicketStatusRequest("CLOSED"));
    }

    // ==================== 内部方法 ====================

    private void autoAssignByMailbox(TicketEntity ticket, Long mailboxId) {
        MailboxEntity mailbox = mailboxMapper.selectById(mailboxId);
        if (mailbox == null || mailbox.getDefaultAssigneeId() == null) {
            log.info("邮箱无默认处理人，工单保持待分配 ticketId={} mailboxId={}", ticket.getId(), mailboxId);
            return;
        }
        UserEntity assignee = userMapper.selectById(mailbox.getDefaultAssigneeId());
        if (assignee == null || !Boolean.TRUE.equals(assignee.getEnabled())) {
            log.info("默认处理人无效或已停用，工单保持待分配 ticketId={} assigneeId={}", ticket.getId(), mailbox.getDefaultAssigneeId());
            return;
        }
        ticket.setAssigneeId(mailbox.getDefaultAssigneeId());
        ticket.setStatus(STATUS_PROCESSING);
        ticketMapper.updateById(ticket);

        recordEvent(ticket.getId(), EVENT_ASSIGNED, "自动分配处理人：" + assignee.getDisplayName(), OPERATOR_SYSTEM, LocalDateTime.now());
        log.info("自动分配处理人 ticketId={} assignee={}", ticket.getId(), assignee.getDisplayName());
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
        if (fields.containsKey("closed_at")) {
            update.setClosedAt((LocalDateTime) fields.get("closed_at"));
        }
        update.setUpdatedBy("system");
        ticketMapper.updateById(update);
    }

    private LambdaQueryWrapper<TicketEntity> buildPageQuery(String keyword, String status,
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

    private void assertAgentOrAdmin(CurrentUserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(CODE_FORBIDDEN, "未登录");
        }
        if (!ROLE_ADMIN.equals(principal.roleCode()) && !"AGENT".equals(principal.roleCode())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅管理员和处理人可操作工单");
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
                ticket.getCreatedAt()
        );
    }

    private TicketVO toDetailVO(TicketEntity ticket) {
        String assigneeName = resolveUserName(ticket.getAssigneeId());
        String mailboxName = resolveMailboxName(ticket.getMailboxId());

        List<TicketMessageVO> messages = ticketMessageMapper.selectList(
                new LambdaQueryWrapper<TicketMessageEntity>()
                        .eq(TicketMessageEntity::getTicketId, ticket.getId())
                        .orderByAsc(TicketMessageEntity::getCreatedAt)
        ).stream().map(this::toMessageVO).toList();

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
                ticket.getCreatedAt(), ticket.getUpdatedAt(),
                messages, events
        );
    }

    private TicketMessageVO toMessageVO(TicketMessageEntity message) {
        String operatorName = null;
        if (message.getOperatorId() != null) {
            operatorName = resolveUserName(message.getOperatorId());
        }
        return new TicketMessageVO(
                message.getId(), message.getDirection(),
                message.getFromAddress(), message.getToAddress(),
                message.getSubject(), message.getContentText(),
                message.getSentAt() != null ? message.getSentAt() : message.getCreatedAt(),
                operatorName, message.getCreatedAt()
        );
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
}
