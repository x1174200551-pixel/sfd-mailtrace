package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * P2-CUTOVER 后的数据权限入口：角色只决定功能权限，业务数据统一按用户企业/邮箱授权过滤。
 */
@Service
@RequiredArgsConstructor
public class DataScopeService {

    private static final int CODE_FORBIDDEN = 40302;

    private final PermissionService permissionService;
    private final EnterpriseMailboxAccessService enterpriseMailboxAccessService;

    public void assertAgentOrAdmin(CurrentUserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(CODE_FORBIDDEN, "未登录");
        }
        if (permissionService.getCurrentPermissions(principal).permissions().isEmpty()) {
            throw new BusinessException(CODE_FORBIDDEN, "当前用户无任何权限");
        }
    }

    public boolean isAdmin(CurrentUserPrincipal principal) {
        assertAgentOrAdmin(principal);
        return enterpriseMailboxAccessService.isAdmin(principal);
    }

    public boolean hasAllTicketAccess(CurrentUserPrincipal principal) {
        return isAdmin(principal);
    }

    /**
     * 所有工单查询先限定在当前用户可读邮箱范围内。空授权显式追加 1 = 0，禁止退化成全表。
     */
    public void applyTicketScope(LambdaQueryWrapper<TicketEntity> wrapper, CurrentUserPrincipal principal) {
        assertAgentOrAdmin(principal);
        applyReadableMailboxScope(wrapper, enterpriseMailboxAccessService.resolveReadableMailboxIds(principal));
    }

    public void assertTicketVisible(CurrentUserPrincipal principal, TicketEntity ticket) {
        assertAgentOrAdmin(principal);
        if (ticket == null) {
            return;
        }
        enterpriseMailboxAccessService.assertMailboxReadable(principal, ticket.getMailboxId());
    }

    public void assertTicketOperable(CurrentUserPrincipal principal, TicketEntity ticket) {
        assertAgentOrAdmin(principal);
        if (ticket == null) {
            return;
        }
        enterpriseMailboxAccessService.assertMailboxOperational(principal, ticket.getMailboxId());
    }

    public Set<Long> resolveReadableMailboxIds(CurrentUserPrincipal principal) {
        assertAgentOrAdmin(principal);
        return enterpriseMailboxAccessService.resolveReadableMailboxIds(principal);
    }

    private void applyReadableMailboxScope(LambdaQueryWrapper<TicketEntity> wrapper, Set<Long> mailboxIds) {
        if (mailboxIds == null || mailboxIds.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.in(TicketEntity::getMailboxId, mailboxIds);
    }
}
