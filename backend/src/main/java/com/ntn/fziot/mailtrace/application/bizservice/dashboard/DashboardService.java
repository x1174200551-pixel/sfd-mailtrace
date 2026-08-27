package com.ntn.fziot.mailtrace.application.bizservice.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.DataScopeService;
import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.dashboard.DashboardReportVO;
import com.ntn.fziot.mailtrace.interfaces.vo.dashboard.DashboardSummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.dashboard.DashboardTodoListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketSummaryVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailFetchLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailFetchLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailSendLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int DEFAULT_TODO_LIMIT = 10;
    private static final int MAX_TODO_LIMIT = 50;
    private static final Set<String> TODO_STATUSES = Set.of("PENDING_ASSIGN", "PROCESSING", "WAITING_CUSTOMER");

    private final TicketMapper ticketMapper;
    private final UserMapper userMapper;
    private final MailboxMapper mailboxMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final MailFetchLogMapper mailFetchLogMapper;
    private final MailSendLogMapper mailSendLogMapper;
    private final DataScopeService dataScopeService;
    private final PermissionService permissionService;
    private final EnterpriseMailboxAccessService enterpriseMailboxAccessService;

    /**
     * 查询工作台核心统计摘要。
     */
    public DashboardSummaryVO summary(CurrentUserPrincipal principal) {
        return summary(principal, null, null);
    }

    public DashboardSummaryVO summary(CurrentUserPrincipal principal, Long enterpriseId, Long mailboxId) {
        // 1、工作台仅允许管理员和处理人访问，统计口径与工单列表一致。
        permissionService.assertPermission(principal, "dashboard:read", "无权查看工作台");

        // 2、按工单状态分别统计核心卡片指标。
        DashboardFilter filter = new DashboardFilter(enterpriseId, mailboxId);
        long totalCount = countTickets(principal, filter, null, null, false);
        long pendingAssignCount = countTickets(principal, filter, "PENDING_ASSIGN", null, false);
        long processingCount = countTickets(principal, filter, "PROCESSING", null, false);
        long waitingCustomerCount = countTickets(principal, filter, "WAITING_CUSTOMER", null, false);
        long slaOverdueCount = countTickets(principal, filter, null, true, false);
        long closedTodayCount = countTickets(principal, filter, "CLOSED", null, true);

        // 3、汇总活跃工单数，供工作台顶部概览直接展示。
        long activeCount = pendingAssignCount + processingCount + waitingCustomerCount;
        return new DashboardSummaryVO(
                totalCount,
                pendingAssignCount,
                processingCount,
                waitingCustomerCount,
                slaOverdueCount,
                closedTodayCount,
                activeCount
        );
    }

    /**
     * 查询当前用户待办工单。
     */
    public DashboardTodoListResponse myTodos(CurrentUserPrincipal principal, Integer limit) {
        return myTodos(principal, null, null, limit);
    }

    public DashboardTodoListResponse myTodos(CurrentUserPrincipal principal, Long enterpriseId, Long mailboxId, Integer limit) {
        // 1、工作台待办仅允许管理员和处理人访问，并且必须按当前登录用户过滤。
        permissionService.assertPermission(principal, "dashboard:read", "无权查看工作台");
        int normalizedLimit = normalizeLimit(limit);

        // 2、查询当前用户可见的可处理状态工单，SLA 近的优先，随后按创建时间倒序。
        DashboardFilter filter = new DashboardFilter(enterpriseId, mailboxId);
        LambdaQueryWrapper<TicketEntity> baseWrapper = buildMyTodoWrapper(principal, filter);
        List<TicketSummaryVO> records = ticketMapper.selectList(baseWrapper
                        .last("ORDER BY sla_response_deadline IS NULL ASC, sla_response_deadline ASC, created_at DESC LIMIT "
                                + normalizedLimit))
                .stream()
                .map(this::toSummaryVO)
                .toList();

        // 3、按相同用户和状态口径计算摘要，供工作台卡片和角标使用。
        long totalCount = ticketMapper.selectCount(buildMyTodoWrapper(principal, filter));
        long processingCount = ticketMapper.selectCount(buildMyTodoWrapper(principal, filter)
                .eq(TicketEntity::getStatus, "PROCESSING"));
        long waitingCustomerCount = ticketMapper.selectCount(buildMyTodoWrapper(principal, filter)
                .eq(TicketEntity::getStatus, "WAITING_CUSTOMER"));
        long slaOverdueCount = ticketMapper.selectCount(buildMyTodoWrapper(principal, filter)
                .eq(TicketEntity::getSlaBreached, true));

        // 4、返回待办列表和摘要。
        return new DashboardTodoListResponse(
                records,
                totalCount,
                processingCount,
                waitingCustomerCount,
                slaOverdueCount,
                normalizedLimit
        );
    }

    /**
     * 查询工作台运营报表。
     */
    public DashboardReportVO report(CurrentUserPrincipal principal) {
        return report(principal, null, null);
    }

    public DashboardReportVO report(CurrentUserPrincipal principal, Long enterpriseId, Long mailboxId) {
        // 1、工作台报表沿用工作台权限，工单相关指标沿用工单数据范围。
        permissionService.assertPermission(principal, "dashboard:read", "无权查看工作台");

        // 2、计算当天处理效率、SLA 和优先级等核心业务指标。
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plusHours(2);

        DashboardFilter filter = new DashboardFilter(enterpriseId, mailboxId);
        long todayCreatedCount = countTicketsCreatedBetween(principal, filter, todayStart, tomorrowStart);
        long todayClosedCount = countTicketsClosedBetween(principal, filter, todayStart, tomorrowStart);
        long activeCount = countTodoTickets(principal, filter, null, null);
        long overdueCount = countTodoTickets(principal, filter, null, true);
        long pendingAssignCount = countTodoTickets(principal, filter, "PENDING_ASSIGN", null);
        long waitingCustomerCount = countTodoTickets(principal, filter, "WAITING_CUSTOMER", null);
        long dueSoonCount = countDueSoonTickets(principal, filter, now, soon);
        long linkSuspectCount = countLinkSuspectTickets(principal, filter);
        int completionRate = percent(todayClosedCount, todayCreatedCount);
        int slaRiskRate = percent(overdueCount, activeCount);
        int firstReplyRate = countFirstReplyRate(principal, filter, now);
        int resolveRate = countResolveRate(principal, filter);

        List<DashboardReportVO.ChartItem> priorityItems = buildPriorityItems(principal, filter);
        long priorityMaxValue = Math.max(priorityItems.stream().mapToLong(DashboardReportVO.ChartItem::value).max().orElse(0), 1);

        // 3、计算邮件链路运行指标，用于判断收发和追信关联是否异常。
        Set<Long> readableMailboxIds = enterpriseMailboxAccessService.resolveReadableMailboxIds(principal);
        LambdaQueryWrapper<MailFetchLogEntity> fetchLogWrapper = new LambdaQueryWrapper<MailFetchLogEntity>()
                .ge(MailFetchLogEntity::getStartedAt, todayStart)
                .lt(MailFetchLogEntity::getStartedAt, tomorrowStart);
        applyFetchLogScope(fetchLogWrapper, readableMailboxIds);
        applyFetchLogFilter(fetchLogWrapper, filter);
        List<MailFetchLogEntity> todayFetchLogs = mailFetchLogMapper.selectList(fetchLogWrapper);
        long fetchTotal = todayFetchLogs.size();
        long fetchSuccess = todayFetchLogs.stream().filter(log -> Boolean.TRUE.equals(log.getSuccess())).count();
        long fetchedMailCount = todayFetchLogs.stream().mapToLong(log -> safeInt(log.getFetchedCount())).sum();
        long createdTicketCount = todayFetchLogs.stream().mapToLong(log -> safeInt(log.getCreatedTicketCount())).sum();
        long linkedTicketCount = todayFetchLogs.stream().mapToLong(log -> safeInt(log.getLinkedCount())).sum();
        LambdaQueryWrapper<MailSendLogEntity> sendLogWrapper = new LambdaQueryWrapper<MailSendLogEntity>()
                .in(MailSendLogEntity::getSendStatus, List.of("PENDING", "FAILED", "RETRYING"));
        applySendLogScope(sendLogWrapper, readableMailboxIds);
        applySendLogFilter(sendLogWrapper, filter);
        long sendExceptionCount = mailSendLogMapper.selectCount(sendLogWrapper);
        int mailSuccessRate = fetchTotal == 0 ? 100 : percent(fetchSuccess, fetchTotal);

        // 4、组装页面需要的报表区块，前端不再自行推导业务口径。
        return new DashboardReportVO(
                new DashboardReportVO.Efficiency(
                        completionRate,
                        List.of(
                                metric("今日新增", todayCreatedCount, "进入处理池", "primary"),
                                metric("今日完成", todayClosedCount, "已关闭", "success"),
                                metric("新增积压", todayCreatedCount - todayClosedCount, "新增 - 关闭", todayCreatedCount > todayClosedCount ? "warning" : "success")
                        )
                ),
                new DashboardReportVO.PriorityDistribution(priorityMaxValue, priorityItems),
                new DashboardReportVO.SlaHealth(
                        overdueCount > 0 ? "有风险" : "正常",
                        overdueCount > 0 ? "danger" : "success",
                        List.of(
                                metric("首响达成率", firstReplyRate + "%", "按首次回复截止判断", firstReplyRate >= 90 ? "success" : "warning"),
                                metric("超时率", slaRiskRate + "%", overdueCount + " 个已超时", overdueCount > 0 ? "danger" : "success"),
                                metric("即将超时", dueSoonCount, "建议 2 小时内优先处理", dueSoonCount > 0 ? "warning" : "success"),
                                metric("解决达成率", resolveRate + "%", "按关闭截止判断", resolveRate >= 90 ? "success" : "info")
                        )
                ),
                new DashboardReportVO.MailFlow(
                        mailSuccessRate >= 95 && sendExceptionCount == 0 ? "链路正常" : "链路检查",
                        mailSuccessRate >= 95 && sendExceptionCount == 0 ? "success" : "warning",
                        List.of(
                                flow("收件任务", mailSuccessRate + "%", "今日拉取 " + fetchedMailCount + " 封", "inbox", mailSuccessRate >= 95 ? "primary" : "warning"),
                                flow("自动建单", String.valueOf(createdTicketCount), "新邮件按规则建单", "mail-check", "success"),
                                flow("追信关联", String.valueOf(linkedTicketCount), "客户回复命中原工单", "message-circle", "info"),
                                flow("发件重试", String.valueOf(sendExceptionCount), "失败和待重试邮件", "mail-warning", sendExceptionCount > 0 ? "danger" : "success")
                        )
                ),
                new DashboardReportVO.ActionPanel(
                        overdueCount > 0 ? "风险优先" : "队列正常",
                        overdueCount > 0 ? "danger" : "success",
                        List.of(
                                action("先处理我的超时", "当前账号名下已超时", overdueCount, "danger", "triangle-alert", null, "ALL", true),
                                action("确认待分配队列", "需要尽快指定处理人", pendingAssignCount, "warning", "clock", null, "PENDING_ASSIGN", false),
                                action("跟进客户补充", "跟踪客户补充进度", waitingCustomerCount, "info", "message-circle", null, "WAITING_CUSTOMER", false),
                                action("排查发件异常", "失败和待重试邮件", sendExceptionCount, "danger", "mail-warning", "发件记录", null, false)
                        )
                ),
                buildAssigneeLoads(principal, filter),
                List.of(
                        quality("疑似断链工单", "需要核对邮件关联", linkSuspectCount, linkSuspectCount > 0 ? "warning" : "success", "bar-chart", null, "ALL", false),
                        quality("失败发送处理", "失败和待重试邮件进入发件记录排查", sendExceptionCount, sendExceptionCount > 0 ? "danger" : "success", "mail-warning", "发件记录", null, false),
                        quality("SLA 风险闭环", "超时工单优先转入处理队列", overdueCount, overdueCount > 0 ? "danger" : "success", "timer", null, "ALL", true)
                )
        );
    }

    private LambdaQueryWrapper<TicketEntity> buildMyTodoWrapper(CurrentUserPrincipal principal, DashboardFilter filter) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .in(TicketEntity::getStatus, TODO_STATUSES)
                .eq(TicketEntity::getAssigneeId, principal.id());
        dataScopeService.applyTicketScope(wrapper, principal);
        applyTicketFilter(wrapper, filter);
        return wrapper;
    }

    private void applyTicketFilter(LambdaQueryWrapper<TicketEntity> wrapper, DashboardFilter filter) {
        if (filter.enterpriseId() != null) {
            wrapper.eq(TicketEntity::getEnterpriseId, filter.enterpriseId());
        }
        if (filter.mailboxId() != null) {
            wrapper.eq(TicketEntity::getMailboxId, filter.mailboxId());
        }
    }

    private void applyFetchLogFilter(LambdaQueryWrapper<MailFetchLogEntity> wrapper, DashboardFilter filter) {
        if (filter.enterpriseId() != null) {
            wrapper.eq(MailFetchLogEntity::getEnterpriseId, filter.enterpriseId());
        }
        if (filter.mailboxId() != null) {
            wrapper.eq(MailFetchLogEntity::getMailboxId, filter.mailboxId());
        }
    }

    private void applySendLogFilter(LambdaQueryWrapper<MailSendLogEntity> wrapper, DashboardFilter filter) {
        if (filter.enterpriseId() != null) {
            wrapper.eq(MailSendLogEntity::getEnterpriseId, filter.enterpriseId());
        }
        if (filter.mailboxId() != null) {
            wrapper.eq(MailSendLogEntity::getMailboxId, filter.mailboxId());
        }
    }

    private void applyFetchLogScope(LambdaQueryWrapper<MailFetchLogEntity> wrapper, Set<Long> mailboxIds) {
        if (mailboxIds.isEmpty()) {
            wrapper.apply("1 = 0");
        } else {
            wrapper.in(MailFetchLogEntity::getMailboxId, mailboxIds);
        }
    }

    private void applySendLogScope(LambdaQueryWrapper<MailSendLogEntity> wrapper, Set<Long> mailboxIds) {
        if (mailboxIds.isEmpty()) {
            wrapper.apply("1 = 0");
        } else {
            wrapper.in(MailSendLogEntity::getMailboxId, mailboxIds);
        }
    }

    private long countTodoTickets(CurrentUserPrincipal principal, DashboardFilter filter, String status, Boolean slaBreached) {
        LambdaQueryWrapper<TicketEntity> wrapper = buildMyTodoWrapper(principal, filter);
        if (status != null) {
            wrapper.eq(TicketEntity::getStatus, status);
        }
        if (slaBreached != null) {
            wrapper.eq(TicketEntity::getSlaBreached, slaBreached);
        }
        return ticketMapper.selectCount(wrapper);
    }

    private long countTicketsCreatedBetween(CurrentUserPrincipal principal, DashboardFilter filter, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .ge(TicketEntity::getCreatedAt, start)
                .lt(TicketEntity::getCreatedAt, end);
        dataScopeService.applyTicketScope(wrapper, principal);
        applyTicketFilter(wrapper, filter);
        return ticketMapper.selectCount(wrapper);
    }

    private long countTicketsClosedBetween(CurrentUserPrincipal principal, DashboardFilter filter, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .eq(TicketEntity::getStatus, "CLOSED")
                .ge(TicketEntity::getClosedAt, start)
                .lt(TicketEntity::getClosedAt, end);
        dataScopeService.applyTicketScope(wrapper, principal);
        applyTicketFilter(wrapper, filter);
        return ticketMapper.selectCount(wrapper);
    }

    private long countDueSoonTickets(CurrentUserPrincipal principal, DashboardFilter filter, LocalDateTime now, LocalDateTime soon) {
        LambdaQueryWrapper<TicketEntity> wrapper = buildMyTodoWrapper(principal, filter)
                .and(scope -> scope
                        .between(TicketEntity::getSlaResponseDeadline, now, soon)
                        .or()
                        .between(TicketEntity::getSlaResolveDeadline, now, soon))
                .and(scope -> scope
                        .ne(TicketEntity::getSlaBreached, true)
                        .or()
                        .isNull(TicketEntity::getSlaBreached));
        return ticketMapper.selectCount(wrapper);
    }

    private long countLinkSuspectTickets(CurrentUserPrincipal principal, DashboardFilter filter) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .eq(TicketEntity::getLinkSuspect, true);
        dataScopeService.applyTicketScope(wrapper, principal);
        applyTicketFilter(wrapper, filter);
        return ticketMapper.selectCount(wrapper);
    }

    private int countFirstReplyRate(CurrentUserPrincipal principal, DashboardFilter filter, LocalDateTime now) {
        long total = countTicketsWithResponseDeadline(principal, filter);
        if (total == 0) {
            return 100;
        }
        long expiredMissing = countTicketsWithExpiredMissingFirstReply(principal, filter, now);
        long late = countTicketsWithLateFirstReply(principal, filter);
        return Math.max(0, 100 - percent(expiredMissing + late, total));
    }

    private long countTicketsWithResponseDeadline(CurrentUserPrincipal principal, DashboardFilter filter) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .isNotNull(TicketEntity::getSlaResponseDeadline);
        dataScopeService.applyTicketScope(wrapper, principal);
        applyTicketFilter(wrapper, filter);
        return ticketMapper.selectCount(wrapper);
    }

    private long countTicketsWithExpiredMissingFirstReply(CurrentUserPrincipal principal, DashboardFilter filter, LocalDateTime now) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .isNull(TicketEntity::getFirstReplyAt)
                .lt(TicketEntity::getSlaResponseDeadline, now);
        dataScopeService.applyTicketScope(wrapper, principal);
        applyTicketFilter(wrapper, filter);
        return ticketMapper.selectCount(wrapper);
    }

    private long countTicketsWithLateFirstReply(CurrentUserPrincipal principal, DashboardFilter filter) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .isNotNull(TicketEntity::getFirstReplyAt)
                .isNotNull(TicketEntity::getSlaResponseDeadline)
                .apply("first_reply_at > sla_response_deadline");
        dataScopeService.applyTicketScope(wrapper, principal);
        applyTicketFilter(wrapper, filter);
        return ticketMapper.selectCount(wrapper);
    }

    private int countResolveRate(CurrentUserPrincipal principal, DashboardFilter filter) {
        long total = countClosedTicketsWithResolveDeadline(principal, filter);
        if (total == 0) {
            return 100;
        }
        long late = countLateResolvedTickets(principal, filter);
        return Math.max(0, 100 - percent(late, total));
    }

    private long countClosedTicketsWithResolveDeadline(CurrentUserPrincipal principal, DashboardFilter filter) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .eq(TicketEntity::getStatus, "CLOSED")
                .isNotNull(TicketEntity::getSlaResolveDeadline)
                .isNotNull(TicketEntity::getClosedAt);
        dataScopeService.applyTicketScope(wrapper, principal);
        applyTicketFilter(wrapper, filter);
        return ticketMapper.selectCount(wrapper);
    }

    private long countLateResolvedTickets(CurrentUserPrincipal principal, DashboardFilter filter) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .eq(TicketEntity::getStatus, "CLOSED")
                .isNotNull(TicketEntity::getSlaResolveDeadline)
                .isNotNull(TicketEntity::getClosedAt)
                .apply("closed_at > sla_resolve_deadline");
        dataScopeService.applyTicketScope(wrapper, principal);
        applyTicketFilter(wrapper, filter);
        return ticketMapper.selectCount(wrapper);
    }

    private List<DashboardReportVO.ChartItem> buildPriorityItems(CurrentUserPrincipal principal, DashboardFilter filter) {
        return List.of(
                chart("紧急", countPriorityTodos(principal, filter, "URGENT"), "danger"),
                chart("高", countPriorityTodos(principal, filter, "HIGH"), "warning"),
                chart("普通", countPriorityTodos(principal, filter, "NORMAL"), "primary"),
                chart("低", countPriorityTodos(principal, filter, "LOW"), "success")
        );
    }

    private long countPriorityTodos(CurrentUserPrincipal principal, DashboardFilter filter, String priority) {
        return ticketMapper.selectCount(buildMyTodoWrapper(principal, filter)
                .eq(TicketEntity::getPriority, priority));
    }

    private List<DashboardReportVO.AssigneeLoad> buildAssigneeLoads(CurrentUserPrincipal principal, DashboardFilter filter) {
        List<TicketEntity> todoTickets = ticketMapper.selectList(buildMyTodoWrapper(principal, filter)
                .last("ORDER BY sla_breached DESC, sla_response_deadline IS NULL ASC, sla_response_deadline ASC, created_at DESC LIMIT 200"));
        Map<Long, List<TicketEntity>> grouped = todoTickets.stream()
                .collect(Collectors.groupingBy(ticket -> ticket.getAssigneeId() == null ? 0L : ticket.getAssigneeId()));
        return grouped.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue().size(), left.getValue().size()))
                .limit(4)
                .map(entry -> {
                    Long assigneeId = entry.getKey();
                    List<TicketEntity> tickets = entry.getValue();
                    long overdueCount = tickets.stream().filter(ticket -> Boolean.TRUE.equals(ticket.getSlaBreached())).count();
                    String name = assigneeId == 0L ? "未分配" : resolveUserName(assigneeId);
                    return new DashboardReportVO.AssigneeLoad(
                            name == null || name.isBlank() ? "未命名处理人" : name,
                            overdueCount > 0 ? overdueCount + " 个超时" : "待办 " + tickets.size() + " 个",
                            tickets.size(),
                            overdueCount > 0
                    );
                })
                .toList();
    }

    private long countTickets(CurrentUserPrincipal principal, DashboardFilter filter, String status, Boolean slaBreached, boolean closedToday) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<>();
        dataScopeService.applyTicketScope(wrapper, principal);
        applyTicketFilter(wrapper, filter);
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

    private record DashboardFilter(Long enterpriseId, Long mailboxId) {
    }

    private TicketSummaryVO toSummaryVO(TicketEntity ticket) {
        String assigneeName = resolveUserName(ticket.getAssigneeId());
        String mailboxName = resolveMailboxName(ticket.getMailboxId());
        String enterpriseName = resolveEnterpriseName(ticket.getEnterpriseId());
        return new TicketSummaryVO(
                ticket.getId(), ticket.getTicketNo(), ticket.getSubject(), ticket.getStatus(), ticket.getPriority(),
                ticket.getEnterpriseId(), enterpriseName,
                ticket.getCustomerEmail(), ticket.getAssigneeId(), assigneeName,
                ticket.getMailboxId(), mailboxName,
                ticket.getSlaPolicyId(), ticket.getAutoReplyTemplateId(),
                ticket.getAssignmentRuleGroupId(), ticket.getAssignmentRuleId(),
                ticket.getLinkSuspect(), ticket.getFirstReplyAt() != null,
                ticket.getSlaResponseDeadline(), ticket.getSlaBreached(),
                ticket.getRemark(),
                ticket.getCreatedAt()
        );
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        UserEntity user = userMapper.selectById(userId);
        return user == null ? null : user.getDisplayName();
    }

    private String resolveMailboxName(Long mailboxId) {
        if (mailboxId == null) {
            return null;
        }
        MailboxEntity mailbox = mailboxMapper.selectById(mailboxId);
        return mailbox == null ? null : mailbox.getMailboxName();
    }

    private String resolveEnterpriseName(Long enterpriseId) {
        if (enterpriseId == null) {
            return null;
        }
        EnterpriseEntity enterprise = enterpriseMapper.selectById(enterpriseId);
        return enterprise == null ? null : enterprise.getEnterpriseName();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_TODO_LIMIT;
        }
        if (limit < 1) {
            throw new BusinessException(CODE_BAD_REQUEST, "待办条数必须大于 0");
        }
        return Math.min(limit, MAX_TODO_LIMIT);
    }

    private DashboardReportVO.MetricItem metric(String label, long value, String detail, String tone) {
        return metric(label, String.valueOf(value), detail, tone);
    }

    private DashboardReportVO.MetricItem metric(String label, String value, String detail, String tone) {
        return new DashboardReportVO.MetricItem(label, value, detail, tone);
    }

    private DashboardReportVO.ChartItem chart(String label, long value, String tone) {
        return new DashboardReportVO.ChartItem(label, value, tone);
    }

    private DashboardReportVO.FlowItem flow(String label, String value, String detail, String iconKey, String tone) {
        return new DashboardReportVO.FlowItem(label, value, detail, iconKey, tone);
    }

    private DashboardReportVO.ActionItem action(String label, String detail, long value, String tone, String iconKey,
                                                String targetMenu, String ticketStatus, boolean slaBreachedOnly) {
        return new DashboardReportVO.ActionItem(label, detail, value, tone, iconKey, targetMenu, ticketStatus, slaBreachedOnly);
    }

    private DashboardReportVO.QualityCheck quality(String label, String detail, long value, String tone, String iconKey,
                                                   String targetMenu, String ticketStatus, boolean slaBreachedOnly) {
        return new DashboardReportVO.QualityCheck(label, detail, value, tone, iconKey, targetMenu, ticketStatus, slaBreachedOnly);
    }

    private int percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return Math.toIntExact(Math.round((numerator * 100.0) / denominator));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

}
