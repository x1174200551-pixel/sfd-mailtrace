package com.ntn.fziot.mailtrace.application.bizservice.assignment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleGroupListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleGroupSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleGroupVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.AssignmentRuleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.AssignmentRuleGroupEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.AssignmentRuleGroupMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.AssignmentRuleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentRuleGroupService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_NOT_FOUND = 40401;
    private static final int CODE_CONFLICT = 40901;
    private static final String MODULE_ASSIGNMENT_RULE_GROUP = "ASSIGNMENT_RULE_GROUP";

    private final AssignmentRuleGroupMapper groupMapper;
    private final AssignmentRuleMapper ruleMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final MailboxMapper mailboxMapper;
    private final OperationLogMapper operationLogMapper;
    private final PermissionService permissionService;

    public AssignmentRuleGroupListResponse listGroups(CurrentUserPrincipal principal, Long enterpriseId,
                                                       String keyword, Boolean enabled) {
        permissionService.assertPermission(principal, "assignment_rule_group:read", "无权查看分配规则组");
        LambdaQueryWrapper<AssignmentRuleGroupEntity> wrapper = buildQuery(enterpriseId, keyword, enabled)
                .orderByDesc(AssignmentRuleGroupEntity::getEnabled)
                .orderByAsc(AssignmentRuleGroupEntity::getGroupName)
                .orderByAsc(AssignmentRuleGroupEntity::getId);
        List<AssignmentRuleGroupVO> records = groupMapper.selectList(wrapper).stream().map(this::toVO).toList();
        LambdaQueryWrapper<AssignmentRuleGroupEntity> summaryScope = summaryScope(enterpriseId);
        long total = groupMapper.selectCount(summaryScope);
        long enabledCount = groupMapper.selectCount(summaryScope(enterpriseId)
                .eq(AssignmentRuleGroupEntity::getEnabled, true));
        return new AssignmentRuleGroupListResponse(records, total, enabledCount, total - enabledCount);
    }

    public List<AssignmentRuleGroupVO> listOptions(CurrentUserPrincipal principal, Long enterpriseId,
                                                    Boolean enabled) {
        permissionService.assertPermission(principal, "assignment_rule_group:read", "无权查看分配规则组");
        return groupMapper.selectList(buildQuery(enterpriseId, null, enabled)
                        .orderByAsc(AssignmentRuleGroupEntity::getGroupName)
                        .orderByAsc(AssignmentRuleGroupEntity::getId))
                .stream().map(this::toVO).toList();
    }

    @Transactional
    public AssignmentRuleGroupVO createGroup(CurrentUserPrincipal principal, AssignmentRuleGroupSaveRequest request) {
        permissionService.assertPermission(principal, "assignment_rule_group:create", "无权新建分配规则组");
        assertEnterpriseEnabled(request.getEnterpriseId());
        String groupName = normalize(request.getGroupName());
        ensureNameUnique(request.getEnterpriseId(), groupName, null);
        AssignmentRuleGroupEntity entity = new AssignmentRuleGroupEntity();
        fillGroup(entity, request, principal.account());
        entity.setGroupName(groupName);
        entity.setCreatedBy(principal.account());
        groupMapper.insert(entity);
        recordLog(principal, "CREATE", entity.getId(), "新建分配规则组：" + groupName);
        return toVO(groupMapper.selectById(entity.getId()));
    }

    @Transactional
    public AssignmentRuleGroupVO updateGroup(CurrentUserPrincipal principal, Long id,
                                              AssignmentRuleGroupSaveRequest request) {
        permissionService.assertPermission(principal, "assignment_rule_group:update", "无权编辑分配规则组");
        AssignmentRuleGroupEntity existing = requireGroup(id);
        assertEnterpriseEnabled(request.getEnterpriseId());
        String groupName = normalize(request.getGroupName());
        ensureNameUnique(request.getEnterpriseId(), groupName, id);
        if (!existing.getEnterpriseId().equals(request.getEnterpriseId()) && isReferenced(id)) {
            throw new BusinessException(CODE_CONFLICT, "规则组已被邮箱或规则引用，不能变更所属企业");
        }
        AssignmentRuleGroupEntity update = new AssignmentRuleGroupEntity();
        update.setId(id);
        fillGroup(update, request, principal.account());
        update.setGroupName(groupName);
        groupMapper.updateById(update);
        recordLog(principal, "UPDATE", id, "编辑分配规则组：" + existing.getGroupName());
        return toVO(groupMapper.selectById(id));
    }

    @Transactional
    public AssignmentRuleGroupVO updateEnabled(CurrentUserPrincipal principal, Long id,
                                                AssignmentRuleEnabledRequest request) {
        permissionService.assertPermission(principal, "assignment_rule_group:enable", "无权启停分配规则组");
        AssignmentRuleGroupEntity existing = requireGroup(id);
        groupMapper.update(null, new LambdaUpdateWrapper<AssignmentRuleGroupEntity>()
                .eq(AssignmentRuleGroupEntity::getId, id)
                .set(AssignmentRuleGroupEntity::getEnabled, request.getEnabled())
                .set(AssignmentRuleGroupEntity::getUpdatedBy, principal.account()));
        recordLog(principal, Boolean.TRUE.equals(request.getEnabled()) ? "ENABLE" : "DISABLE", id,
                (Boolean.TRUE.equals(request.getEnabled()) ? "启用分配规则组：" : "停用分配规则组：")
                        + existing.getGroupName());
        return toVO(groupMapper.selectById(id));
    }

    @Transactional
    public void deleteGroup(CurrentUserPrincipal principal, Long id) {
        permissionService.assertPermission(principal, "assignment_rule_group:delete", "无权删除分配规则组");
        AssignmentRuleGroupEntity existing = requireGroup(id);
        if (mailboxMapper.selectCount(new LambdaQueryWrapper<MailboxEntity>()
                .eq(MailboxEntity::getAssignmentRuleGroupId, id)) > 0) {
            throw new BusinessException(CODE_CONFLICT, "分配规则组已被邮箱引用，不能删除");
        }
        if (ruleMapper.selectCount(new LambdaQueryWrapper<AssignmentRuleEntity>()
                .eq(AssignmentRuleEntity::getGroupId, id)) > 0) {
            throw new BusinessException(CODE_CONFLICT, "分配规则组下仍有规则，不能删除");
        }
        groupMapper.deleteById(id);
        recordLog(principal, "DELETE", id, "删除分配规则组：" + existing.getGroupName());
    }

    private LambdaQueryWrapper<AssignmentRuleGroupEntity> buildQuery(Long enterpriseId, String keyword,
                                                                      Boolean enabled) {
        LambdaQueryWrapper<AssignmentRuleGroupEntity> wrapper = new LambdaQueryWrapper<>();
        if (enterpriseId != null) {
            wrapper.eq(AssignmentRuleGroupEntity::getEnterpriseId, enterpriseId);
        }
        String normalizedKeyword = normalize(keyword);
        if (!normalizedKeyword.isEmpty()) {
            wrapper.like(AssignmentRuleGroupEntity::getGroupName, normalizedKeyword);
        }
        if (enabled != null) {
            wrapper.eq(AssignmentRuleGroupEntity::getEnabled, enabled);
        }
        return wrapper;
    }

    private LambdaQueryWrapper<AssignmentRuleGroupEntity> summaryScope(Long enterpriseId) {
        LambdaQueryWrapper<AssignmentRuleGroupEntity> wrapper = new LambdaQueryWrapper<>();
        return enterpriseId == null ? wrapper : wrapper.eq(AssignmentRuleGroupEntity::getEnterpriseId, enterpriseId);
    }

    private void assertEnterpriseEnabled(Long enterpriseId) {
        EnterpriseEntity enterprise = enterpriseId == null ? null : enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "所属企业不存在");
        }
        if (!Boolean.TRUE.equals(enterprise.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, "所属企业已停用，不能配置分配规则组");
        }
    }

    private void ensureNameUnique(Long enterpriseId, String groupName, Long excludeId) {
        LambdaQueryWrapper<AssignmentRuleGroupEntity> wrapper = new LambdaQueryWrapper<AssignmentRuleGroupEntity>()
                .eq(AssignmentRuleGroupEntity::getEnterpriseId, enterpriseId)
                .eq(AssignmentRuleGroupEntity::getGroupName, groupName);
        if (excludeId != null) {
            wrapper.ne(AssignmentRuleGroupEntity::getId, excludeId);
        }
        Long count = groupMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(CODE_CONFLICT, "同一企业下规则组名称不能重复");
        }
    }

    private boolean isReferenced(Long groupId) {
        return mailboxMapper.selectCount(new LambdaQueryWrapper<MailboxEntity>()
                .eq(MailboxEntity::getAssignmentRuleGroupId, groupId)) > 0
                || ruleMapper.selectCount(new LambdaQueryWrapper<AssignmentRuleEntity>()
                .eq(AssignmentRuleEntity::getGroupId, groupId)) > 0;
    }

    private AssignmentRuleGroupEntity requireGroup(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(CODE_BAD_REQUEST, "分配规则组ID不能为空");
        }
        AssignmentRuleGroupEntity entity = groupMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(CODE_NOT_FOUND, "分配规则组不存在");
        }
        return entity;
    }

    private void fillGroup(AssignmentRuleGroupEntity entity, AssignmentRuleGroupSaveRequest request, String operator) {
        entity.setEnterpriseId(request.getEnterpriseId());
        entity.setEnabled(request.getEnabled() == null || request.getEnabled());
        entity.setRemark(normalizeNullable(request.getRemark()));
        entity.setUpdatedBy(operator);
    }

    private void recordLog(CurrentUserPrincipal principal, String actionCode, Long bizId, String content) {
        OperationLogEntity log = new OperationLogEntity();
        log.setOperator(principal.account());
        log.setModuleCode(MODULE_ASSIGNMENT_RULE_GROUP);
        log.setActionCode(actionCode);
        log.setBizId(bizId == null ? null : String.valueOf(bizId));
        log.setContent(content);
        log.setCreatedBy(principal.account());
        log.setUpdatedBy(principal.account());
        operationLogMapper.insert(log);
    }

    private AssignmentRuleGroupVO toVO(AssignmentRuleGroupEntity entity) {
        return new AssignmentRuleGroupVO(entity.getId(), entity.getEnterpriseId(), entity.getGroupName(),
                entity.getEnabled(), entity.getRemark(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }
}
