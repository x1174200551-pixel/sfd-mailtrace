package com.ntn.fziot.mailtrace.application.bizservice.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.DataScopeService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.dashboard.DashboardReportVO;
import com.ntn.fziot.mailtrace.interfaces.vo.dashboard.DashboardSummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.dashboard.DashboardTodoListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketSummaryVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailFetchLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailFetchLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailSendLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
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
    private final MailFetchLogMapper mailFetchLogMapper;
    private final MailSendLogMapper mailSendLogMapper;
    private final DataScopeService dataScopeService;
    private final PermissionService permissionService;

    /**
     * 查询工作台核心统计摘要。
     */
    public DashboardSummaryVO summary(CurrentUserPrincipal principal) {
        // 1、工作台仅允许管理员和处理人访问，统计口径与工单列表一致。
        permissionService.assertPermission(principal, "dashboard:read", "无权查看工作台");

        // 2、按工单状态分别统计核心卡片指标。
        long totalCount = countTickets(principal, null, null, false);
        long pendingAssignCount = countTickets(principal, "PENDING_ASSIGN", null, false);
        long processingCount = countTickets(principal, "PROCESSING", null, false);
        long waitingCustomerCount = countTickets(principal, "WAITING_CUSTOMER", null, false);
        long slaOverdueCount = countTickets(principal, null, true, false);
        long closedTodayCount = countTickets(principal, "CLOSED", null, true);

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
        // 1、工作台待办仅允许管理员和处理人访问，并且必须按当前登录用户过滤。
        permissionService.assertPermission(principal, "dashboard:read", "无权查看工作台");
        int normalizedLimit = normalizeLimit(limit);

        // 2、查询当前用户可见的可处理状态工单，SLA 近的优先，随后按创建时间倒序。
        LambdaQueryWrapper<TicketEntity> baseWrapper = buildMyTodoWrapper(principal);
        List<TicketSummaryVO> records = ticketMapper.selectList(baseWrapper
                        .last("ORDER BY sla_response_deadline IS NULL ASC, sla_response_deadline ASC, created_at DESC LIMIT "
                                + normalizedLimit))
                .stream()
                .map(this::toSummaryVO)
                .toList();

        // 3、按相同用户和状态口径计算摘要，供工作台卡片和角标使用。
        long totalCount = ticketMapper.selectCount(buildMyTodoWrapper(principal));
        long processingCount = ticketMapper.selectCount(buildMyTodoWrapper(principal)
                .eq(TicketEntity::getStatus, "PROCESSING"));
        long waitingCustomerCount = ticketMapper.selectCount(buildMyTodoWrapper(principal)
                .eq(TicketEntity::getStatus, "WAITING_CUSTOMER"));
        long slaOverdueCount = ticketMapper.selectCount(buildMyTodoWrapper(principal)
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
        // 1、工作台报表沿用工作台权限，工单相关指标沿用工单数据范围。
        permissionService.assertPermission(principal, "dashboard:read", "无权查看工作台");

        // 2、计算当天处理效率、SLA 和优先级等核心业务指标。
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plusHours(2);

        long todayCreatedCount = countTicketsCreatedBetween(principal, todayStart, tomorrowStart);
        long todayClosedCount = countTicketsClosedBetween(principal, todayStart, tomorrowStart);
        long activeCount = countTodoTickets(principal, null, null);
        long overdueCount = countTodoTickets(principal, null, true);
        long pendingAssignCount = countTodoTickets(principal, "PENDING_ASSIGN", null);
        long waitingCustomerCount = countTodoTickets(principal, "WAITING_CUSTOMER", null);
        long dueSoonCount = countDueSoonTickets(principal, now, soon);
        long linkSuspectCount = countLinkSuspectTickets(principal);
        int completionRate = percent(todayClosedCount, todayCreatedCount);
        int slaRiskRate = percent(overdueCount, activeCount);
        int firstReplyRate = countFirstReplyRate(principal, now);
        int resolveRate = countResolveRate(principal);

        List<DashboardReportVO.ChartItem> priorityItems = buildPriorityItems(principal);
        long priorityMaxValue = Math.max(priorityItems.stream().mapToLong(DashboardReportVO.ChartItem::value).max().orElse(0), 1);

        // 3、计算邮件链路运行指标，用于判断收发和追信关联是否异常。
        List<MailFetchLogEntity> todayFetchLogs = mailFetchLogMapper.selectList(new LambdaQueryWrapper<MailFetchLogEntity>()
                .ge(MailFetchLogEntity::getStartedAt, todayStart)
                .lt(MailFetchLogEntity::getStartedAt, tomorrowStart));
        long fetchTotal = todayFetchLogs.size();
        long fetchSuccess = todayFetchLogs.stream().filter(log -> Boolean.TRUE.equals(log.getSuccess())).count();
        long fetchedMailCount = todayFetchLogs.stream().mapToLong(log -> safeInt(log.getFetchedCount())).sum();
        long createdTicketCount = todayFetchLogs.stream().mapToLong(log -> safeInt(log.getCreatedTicketCount())).sum();
        long linkedTicketCount = todayFetchLogs.stream().mapToLong(log -> safeInt(log.getLinkedCount())).sum();
        long sendExceptionCount = mailSendLogMapper.selectCount(new LambdaQueryWrapper<MailSendLogEntity>()
                .in(MailSendLogEntity::getSendStatus, List.of("PENDING", "FAILED", "RETRYING")));
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
                buildAssigneeLoads(principal),
                List.of(
                        quality("疑似断链工单", "需要核对邮件关联", linkSuspectCount, linkSuspectCount > 0 ? "warning" : "success", "bar-chart", null, "ALL", false),
                        quality("失败发送处理", "失败和待重试邮件进入发件记录排查", sendExceptionCount, sendExceptionCount > 0 ? "danger" : "success", "mail-warning", "发件记录", null, false),
                        quality("SLA 风险闭环", "超时工单优先转入处理队列", overdueCount, overdueCount > 0 ? "danger" : "success", "timer", null, "ALL", true)
                )
        );
    }

    private LambdaQueryWrapper<TicketEntity> buildMyTodoWrapper(CurrentUserPrincipal principal) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .in(TicketEntity::getStatus, TODO_STATUSES);
        dataScopeService.applyTicketScope(wrapper, principal);
        return wrapper;
    }

    private long countTodoTickets(CurrentUserPrincipal principal, String status, Boolean slaBreached) {
        LambdaQueryWrapper<TicketEntity> wrapper = buildMyTodoWrapper(principal);
        if (status != null) {
            wrapper.eq(TicketEntity::getStatus, status);
        }
        if (slaBreached != null) {
            wrapper.eq(TicketEntity::getSlaBreached, slaBreached);
        }
        return ticketMapper.selectCount(wrapper);
    }

    private long countTicketsCreatedBetween(CurrentUserPrincipal principal, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .ge(TicketEntity::getCreatedAt, start)
                .lt(TicketEntity::getCreatedAt, end);
        dataScopeService.applyTicketScope(wrapper, principal);
        return ticketMapper.selectCount(wrapper);
    }

    private long countTicketsClosedBetween(CurrentUserPrincipal principal, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .eq(TicketEntity::getStatus, "CLOSED")
                .ge(TicketEntity::getClosedAt, start)
                .lt(TicketEntity::getClosedAt, end);
        dataScopeService.applyTicketScope(wrapper, principal);
        return ticketMapper.selectCount(wrapper);
    }

    private long countDueSoonTickets(CurrentUserPrincipal principal, LocalDateTime now, LocalDateTime soon) {
        LambdaQueryWrapper<TicketEntity> wrapper = buildMyTodoWrapper(principal)
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

    private long countLinkSuspectTickets(CurrentUserPrincipal principal) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .eq(TicketEntity::getLinkSuspect, true);
        dataScopeService.applyTicketScope(wrapper, principal);
        return ticketMapper.selectCount(wrapper);
    }

    private int countFirstReplyRate(CurrentUserPrincipal principal, LocalDateTime now) {
        long total = countTicketsWithResponseDeadline(principal);
        if (total == 0) {
            return 100;
        }
        long expiredMissing = countTicketsWithExpiredMissingFirstReply(principal, now);
        long late = countTicketsWithLateFirstReply(principal);
        return Math.max(0, 100 - percent(expiredMissing + late, total));
    }

    private long countTicketsWithResponseDeadline(CurrentUserPrincipal principal) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .isNotNull(TicketEntity::getSlaResponseDeadline);
        dataScopeService.applyTicketScope(wrapper, principal);
        return ticketMapper.selectCount(wrapper);
    }

    private long countTicketsWithExpiredMissingFirstReply(CurrentUserPrincipal principal, LocalDateTime now) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .isNull(TicketEntity::getFirstReplyAt)
                .lt(TicketEntity::getSlaResponseDeadline, now);
        dataScopeService.applyTicketScope(wrapper, principal);
        return ticketMapper.selectCount(wrapper);
    }

    private long countTicketsWithLateFirstReply(CurrentUserPrincipal principal) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .isNotNull(TicketEntity::getFirstReplyAt)
                .isNotNull(TicketEntity::getSlaResponseDeadline)
                .apply("first_reply_at > sla_response_deadline");
        dataScopeService.applyTicketScope(wrapper, principal);
        return ticketMapper.selectCount(wrapper);
    }

    private int countResolveRate(CurrentUserPrincipal principal) {
        long total = countClosedTicketsWithResolveDeadline(principal);
        if (total == 0) {
            return 100;
        }
        long late = countLateResolvedTickets(principal);
        return Math.max(0, 100 - percent(late, total));
    }

    private long countClosedTicketsWithResolveDeadline(CurrentUserPrincipal principal) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .eq(TicketEntity::getStatus, "CLOSED")
                .isNotNull(TicketEntity::getSlaResolveDeadline)
                .isNotNull(TicketEntity::getClosedAt);
        dataScopeService.applyTicketScope(wrapper, principal);
        return ticketMapper.selectCount(wrapper);
    }

    private long countLateResolvedTickets(CurrentUserPrincipal principal) {
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<TicketEntity>()
                .eq(TicketEntity::getStatus, "CLOSED")
                .isNotNull(TicketEntity::getSlaResolveDeadline)
                .isNotNull(TicketEntity::getClosedAt)
                .apply("closed_at > sla_resolve_deadline");
        dataScopeService.applyTicketScope(wrapper, principal);
        return ticketMapper.selectCount(wrapper);
    }

    private List<DashboardReportVO.ChartItem> buildPriorityItems(CurrentUserPrincipal principal) {
        return List.of(
                chart("紧急", countPriorityTodos(principal, "URGENT"), "danger"),
                chart("高", countPriorityTodos(principal, "HIGH"), "warning"),
                chart("普通", countPriorityTodos(principal, "NORMAL"), "primary"),
                chart("低", countPriorityTodos(principal, "LOW"), "success")
        );
    }

    private long countPriorityTodos(CurrentUserPrincipal principal, String priority) {
        return ticketMapper.selectCount(buildMyTodoWrapper(principal)
                .eq(TicketEntity::getPriority, priority));
    }

    private List<DashboardReportVO.AssigneeLoad> buildAssigneeLoads(CurrentUserPrincipal principal) {
        List<TicketEntity> todoTickets = ticketMapper.selectList(buildMyTodoWrapper(principal)
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
