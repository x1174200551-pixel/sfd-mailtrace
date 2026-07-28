package com.ntn.fziot.mailtrace.application.bizservice.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.dashboard.DashboardSummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.dashboard.DashboardTodoListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketSummaryVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_FORBIDDEN = 40302;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_AGENT = "AGENT";
    private static final int DEFAULT_TODO_LIMIT = 10;
    private static final int MAX_TODO_LIMIT = 50;
    private static final Set<String> TODO_STATUSES = Set.of("PENDING_ASSIGN", "PROCESSING", "WAITING_CUSTOMER");

    private final TicketMapper ticketMapper;
    private final UserMapper userMapper;
    private final MailboxMapper mailboxMapper;

    /**
     * 查询工作台核心统计摘要。
     */
    public DashboardSummaryVO summary(CurrentUserPrincipal principal) {
        // 1、工作台仅允许管理员和处理人访问，统计口径与工单列表一致。
        assertAgentOrAdmin(principal);

        // 2、按工单状态分别统计核心卡片指标。
        long totalCount = ticketMapper.countActiveTotal();
        long pendingAssignCount = ticketMapper.countByStatus("PENDING_ASSIGN");
        long processingCount = ticketMapper.countByStatus("PROCESSING");
        long waitingCustomerCount = ticketMapper.countByStatus("WAITING_CUSTOMER");
        long slaOverdueCount = ticketMapper.countSlaOverdue();
        long closedTodayCount = ticketMapper.countClosedToday();

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
        assertAgentOrAdmin(principal);
        int normalizedLimit = normalizeLimit(limit);

        // 2、查询当前用户作为处理人的可处理状态工单，SLA 近的优先，随后按创建时间倒序。
        LambdaQueryWrapper<TicketEntity> baseWrapper = buildMyTodoWrapper(principal.id());
        List<TicketSummaryVO> records = ticketMapper.selectList(baseWrapper
                        .last("ORDER BY sla_response_deadline IS NULL ASC, sla_response_deadline ASC, created_at DESC LIMIT "
                                + normalizedLimit))
                .stream()
                .map(this::toSummaryVO)
                .toList();

        // 3、按相同用户和状态口径计算摘要，供工作台卡片和角标使用。
        long totalCount = ticketMapper.selectCount(buildMyTodoWrapper(principal.id()));
        long processingCount = ticketMapper.selectCount(buildMyTodoWrapper(principal.id())
                .eq(TicketEntity::getStatus, "PROCESSING"));
        long waitingCustomerCount = ticketMapper.selectCount(buildMyTodoWrapper(principal.id())
                .eq(TicketEntity::getStatus, "WAITING_CUSTOMER"));
        long slaOverdueCount = ticketMapper.selectCount(buildMyTodoWrapper(principal.id())
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

    private LambdaQueryWrapper<TicketEntity> buildMyTodoWrapper(Long assigneeId) {
        return new LambdaQueryWrapper<TicketEntity>()
                .eq(TicketEntity::getAssigneeId, assigneeId)
                .in(TicketEntity::getStatus, TODO_STATUSES);
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

    private void assertAgentOrAdmin(CurrentUserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(CODE_FORBIDDEN, "未登录");
        }
        if (!ROLE_ADMIN.equals(principal.roleCode()) && !ROLE_AGENT.equals(principal.roleCode())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅管理员和处理人可查看工作台");
        }
    }
}
