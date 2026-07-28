package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import org.springframework.stereotype.Service;

@Service
public class DataScopeService {

    private static final int CODE_FORBIDDEN = 40302;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_AGENT = "AGENT";
    private static final String RESOURCE_TICKET = "TICKET";
    private static final String SCOPE_ALL = "ALL";
    private static final String SCOPE_SELF = "SELF";

    private final PermissionService permissionService;

    public DataScopeService() {
        this.permissionService = null;
    }

    public DataScopeService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void assertAgentOrAdmin(CurrentUserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(CODE_FORBIDDEN, "未登录");
        }
        if (permissionService == null && !ROLE_ADMIN.equals(principal.roleCode()) && !ROLE_AGENT.equals(principal.roleCode())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅管理员和处理人可访问");
        }
    }

    public boolean isAdmin(CurrentUserPrincipal principal) {
        return hasAllTicketAccess(principal);
    }

    public boolean hasAllTicketAccess(CurrentUserPrincipal principal) {
        assertAgentOrAdmin(principal);
        return hasTicketScope(principal, SCOPE_ALL);
    }

    public void applyTicketScope(LambdaQueryWrapper<TicketEntity> wrapper, CurrentUserPrincipal principal) {
        if (hasAllTicketAccess(principal)) {
            return;
        }
        if (!hasTicketScope(principal, SCOPE_SELF)) {
            throw new BusinessException(CODE_FORBIDDEN, "无工单数据访问范围");
        }
        wrapper.and(scope -> scope
                .eq(TicketEntity::getAssigneeId, principal.id())
                .or()
                .isNull(TicketEntity::getAssigneeId));
    }

    public void assertTicketVisible(CurrentUserPrincipal principal, TicketEntity ticket) {
        assertAgentOrAdmin(principal);
        if (ticket == null || hasAllTicketAccess(principal)) {
            return;
        }
        if (!hasTicketScope(principal, SCOPE_SELF)) {
            throw new BusinessException(CODE_FORBIDDEN, "无工单数据访问范围");
        }
        Long assigneeId = ticket.getAssigneeId();
        if (assigneeId == null || assigneeId.equals(principal.id())) {
            return;
        }
        throw new BusinessException(CODE_FORBIDDEN, "无权查看该工单");
    }

    public void assertTicketOperable(CurrentUserPrincipal principal, TicketEntity ticket) {
        assertAgentOrAdmin(principal);
        if (ticket == null || hasAllTicketAccess(principal)) {
            return;
        }
        if (!hasTicketScope(principal, SCOPE_SELF)) {
            throw new BusinessException(CODE_FORBIDDEN, "无工单数据访问范围");
        }
        if (principal.id() != null && principal.id().equals(ticket.getAssigneeId())) {
            return;
        }
        throw new BusinessException(CODE_FORBIDDEN, "无权操作该工单");
    }

    private boolean hasTicketScope(CurrentUserPrincipal principal, String scopeCode) {
        if (permissionService == null) {
            return SCOPE_ALL.equals(scopeCode) && ROLE_ADMIN.equals(principal.roleCode())
                    || SCOPE_SELF.equals(scopeCode) && ROLE_AGENT.equals(principal.roleCode());
        }
        return permissionService.getCurrentPermissions(principal).hasDataScope(RESOURCE_TICKET, scopeCode);
    }
}
