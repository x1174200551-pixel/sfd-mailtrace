package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.department.DepartmentMemberService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class DataScopeService {

    private static final int CODE_FORBIDDEN = 40302;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SUPERVISOR = "SUPERVISOR";
    private static final String ROLE_AGENT = "AGENT";
    private static final String RESOURCE_TICKET = "TICKET";
    private static final String SCOPE_ALL = "ALL";
    private static final String SCOPE_SELF = "SELF";
    private static final String SCOPE_DEPT = "DEPT";
    private static final String SCOPE_DEPT_AND_CHILDREN = "DEPT_AND_CHILDREN";

    private final PermissionService permissionService;
    private final DepartmentMemberService departmentMemberService;

    public DataScopeService() {
        this.permissionService = null;
        this.departmentMemberService = null;
    }

    @Autowired
    public DataScopeService(PermissionService permissionService,
                            DepartmentMemberService departmentMemberService) {
        this.permissionService = permissionService;
        this.departmentMemberService = departmentMemberService;
    }

    public void assertAgentOrAdmin(CurrentUserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(CODE_FORBIDDEN, "未登录");
        }
        if (permissionService != null) {
            // permissionService 正常注入时，通过权限系统验证用户有基本权限
            if (permissionService.getCurrentPermissions(principal).permissions().isEmpty()) {
                throw new BusinessException(CODE_FORBIDDEN, "当前用户无任何权限");
            }
            return;
        }
        // 回退路径：permissionService 未注入时，按硬编码角色校验
        if (!ROLE_ADMIN.equals(principal.roleCode())
                && !ROLE_SUPERVISOR.equals(principal.roleCode())
                && !ROLE_AGENT.equals(principal.roleCode())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅管理员、主管和处理人可访问");
        }
    }

    public boolean isAdmin(CurrentUserPrincipal principal) {
        return hasAllTicketAccess(principal);
    }

    public boolean hasAllTicketAccess(CurrentUserPrincipal principal) {
        assertAgentOrAdmin(principal);
        return hasTicketScope(principal, SCOPE_ALL);
    }

    /**
     * 对工单列表查询 wrapper 套数据范围。
     * 按范围宽度依次检查：ALL → DEPT_AND_CHILDREN → DEPT → SELF。
     */
    public void applyTicketScope(LambdaQueryWrapper<TicketEntity> wrapper, CurrentUserPrincipal principal) {
        assertAgentOrAdmin(principal);

        // ALL 范围：不限制
        if (hasTicketScope(principal, SCOPE_ALL)) {
            return;
        }

        // DEPT_AND_CHILDREN / DEPT 范围：按部门成员 ID 过滤
        Set<Long> deptMemberIds = resolveDeptScopeMemberIds(principal);
        if (!deptMemberIds.isEmpty()) {
            final Set<Long> scopeIds = deptMemberIds;
            wrapper.and(scope -> scope
                    .in(TicketEntity::getAssigneeId, scopeIds)
                    .or()
                    .isNull(TicketEntity::getAssigneeId));
            return;
        }

        // SELF 范围：只看自己负责 + 未分配
        if (hasTicketScope(principal, SCOPE_SELF)) {
            wrapper.and(scope -> scope
                    .eq(TicketEntity::getAssigneeId, principal.id())
                    .or()
                    .isNull(TicketEntity::getAssigneeId));
            return;
        }

        throw new BusinessException(CODE_FORBIDDEN, "无工单数据访问范围");
    }

    /**
     * 校验当前用户对工单的可见性。
     * 按范围宽度依次检查：ALL → DEPT_AND_CHILDREN → DEPT → SELF。
     */
    public void assertTicketVisible(CurrentUserPrincipal principal, TicketEntity ticket) {
        assertAgentOrAdmin(principal);
        if (ticket == null || hasAllTicketAccess(principal)) {
            return;
        }

        Long assigneeId = ticket.getAssigneeId();

        // 未分配工单所有角色可见
        if (assigneeId == null) {
            return;
        }

        // DEPT_AND_CHILDREN / DEPT 范围：检查 assignee 是否在部门范围内
        Set<Long> deptMemberIds = resolveDeptScopeMemberIds(principal);
        if (!deptMemberIds.isEmpty() && deptMemberIds.contains(assigneeId)) {
            return;
        }

        // SELF 范围：自己负责的工单
        if (hasTicketScope(principal, SCOPE_SELF) && assigneeId.equals(principal.id())) {
            return;
        }

        throw new BusinessException(CODE_FORBIDDEN, "无权查看该工单");
    }

    /**
     * 校验当前用户对工单的可操作性。
     * 按范围宽度依次检查：ALL → DEPT_AND_CHILDREN → DEPT → SELF。
     */
    public void assertTicketOperable(CurrentUserPrincipal principal, TicketEntity ticket) {
        assertAgentOrAdmin(principal);
        if (ticket == null || hasAllTicketAccess(principal)) {
            return;
        }

        Long assigneeId = ticket.getAssigneeId();

        // DEPT_AND_CHILDREN / DEPT 范围：可操作部门范围内工单（含未分配）
        Set<Long> deptMemberIds = resolveDeptScopeMemberIds(principal);
        if (!deptMemberIds.isEmpty()) {
            if (assigneeId == null || deptMemberIds.contains(assigneeId)) {
                return;
            }
            throw new BusinessException(CODE_FORBIDDEN, "无权操作该工单");
        }

        // SELF 范围：只能操作自己负责的工单（未分配不可操作）
        if (hasTicketScope(principal, SCOPE_SELF)) {
            if (principal.id() != null && principal.id().equals(assigneeId)) {
                return;
            }
            throw new BusinessException(CODE_FORBIDDEN, "无权操作该工单");
        }

        throw new BusinessException(CODE_FORBIDDEN, "无工单数据访问范围");
    }

    /**
     * 根据用户的数据范围解析可见工单的负责人 ID 集合。
     * ALL 范围返回 null（不限）；SELF 范围仅返回用户本人 ID；
     * DEPT/DEPT_AND_CHILDREN 范围返回部门成员 ID 集合。
     * 用于 CustomerReadonlyService 等不通过 wrapper 过滤的场景。
     */
    public Set<Long> resolveTicketScopeUserIds(CurrentUserPrincipal principal) {
        assertAgentOrAdmin(principal);
        if (hasTicketScope(principal, SCOPE_ALL)) {
            return null;
        }
        Set<Long> deptIds = resolveDeptScopeMemberIds(principal);
        if (!deptIds.isEmpty()) {
            return deptIds;
        }
        if (hasTicketScope(principal, SCOPE_SELF)) {
            return principal.id() == null ? Collections.emptySet() : Set.of(principal.id());
        }
        return Collections.emptySet();
    }

    /**
     * 检查用户是否有 DEPT_AND_CHILDREN 或 DEPT 范围，并解析部门成员 ID。
     * 返回非空集合表示有部门范围；返回空集合表示无部门范围。
     */
    private Set<Long> resolveDeptScopeMemberIds(CurrentUserPrincipal principal) {
        if (departmentMemberService == null) {
            return Collections.emptySet();
        }
        if (hasTicketScope(principal, SCOPE_DEPT_AND_CHILDREN)) {
            Set<Long> ids = departmentMemberService.resolveDeptAndChildrenMemberIds(principal.id());
            if (!ids.isEmpty()) {
                return ids;
            }
        }
        if (hasTicketScope(principal, SCOPE_DEPT)) {
            Set<Long> ids = departmentMemberService.resolveDeptMemberIds(principal.id());
            if (!ids.isEmpty()) {
                return ids;
            }
        }
        return Collections.emptySet();
    }

    private boolean hasTicketScope(CurrentUserPrincipal principal, String scopeCode) {
        if (permissionService == null) {
            return SCOPE_ALL.equals(scopeCode) && ROLE_ADMIN.equals(principal.roleCode())
                    || SCOPE_SELF.equals(scopeCode) && ROLE_AGENT.equals(principal.roleCode());
        }
        return permissionService.getCurrentPermissions(principal).hasDataScope(RESOURCE_TICKET, scopeCode);
    }
}
