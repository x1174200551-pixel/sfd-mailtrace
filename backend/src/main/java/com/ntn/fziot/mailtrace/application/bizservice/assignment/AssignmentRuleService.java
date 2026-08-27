package com.ntn.fziot.mailtrace.application.bizservice.assignment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleMatchResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleSortItem;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleSortRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleSummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleTestRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.AssignmentRuleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.AssignmentRuleGroupEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.AssignmentRuleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.AssignmentRuleGroupMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentRuleService {

    public static final String MATCH_DEFAULT = "DEFAULT";
    public static final String MATCH_SUBJECT_KEYWORD = "SUBJECT_KEYWORD";
    public static final String MATCH_MAILBOX = "MAILBOX";
    public static final String MATCH_FROM_EMAIL = "FROM_EMAIL";

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_NOT_FOUND = 40401;
    private static final int CODE_CONFLICT = 40901;
    private static final String MODULE_ASSIGNMENT_RULE = "ASSIGNMENT_RULE";
    private static final Set<String> VALID_MATCH_TYPES = Set.of(
            MATCH_DEFAULT, MATCH_SUBJECT_KEYWORD, MATCH_MAILBOX, MATCH_FROM_EMAIL
    );

    // P4：管理与建单匹配都按邮箱绑定的规则组隔离。
    private final AssignmentRuleMapper assignmentRuleMapper;
    private final AssignmentRuleGroupMapper assignmentRuleGroupMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final MailboxMapper mailboxMapper;
    private final UserMapper userMapper;
    private final OperationLogMapper operationLogMapper;
    private final PermissionService permissionService;
    private final EnterpriseMailboxAccessService enterpriseMailboxAccessService;

    /**
     * 管理端测试规则匹配结果，不修改工单。
     */
    public AssignmentRuleMatchResponse testMatch(CurrentUserPrincipal principal, AssignmentRuleTestRequest request) {
        // 1、仅管理员允许模拟规则命中，避免处理人枚举规则配置。
        permissionService.assertPermission(principal, "assignment_rule:test_match", "无权测试分配规则");

        // 2、复用建单规则引擎，保持页面测试与自动建单逻辑一致。
        AssignmentRuleMatchResult result = matchForTicket(
                request.getMailboxId(),
                request.getMailboxAddress(),
                request.getSubject(),
                request.getFromEmail());

        // 3、组装页面需要展示的可解释命中结果。
        if (result == null) {
            return AssignmentRuleMatchResponse.notMatched();
        }
        return new AssignmentRuleMatchResponse(
                true,
                result.ruleId(),
                result.ruleName(),
                result.matchType(),
                result.matchValue(),
                result.assigneeId(),
                result.assigneeName(),
                result.notifyEnabled()
        );
    }

    /**
     * 按优先级匹配自动分配规则。
     */
    public AssignmentRuleMatchResult matchForTicket(Long mailboxId, String mailboxAddress,
                                                    String subject, String fromEmail) {
        // 1、只允许从邮箱绑定且启用的同企业规则组加载规则；无绑定或停用时保持待分配。
        MailboxEntity mailbox = mailboxId == null ? null : mailboxMapper.selectById(mailboxId);
        if (mailbox == null || mailbox.getAssignmentRuleGroupId() == null) {
            return null;
        }
        AssignmentRuleGroupEntity group = assignmentRuleGroupMapper.selectById(mailbox.getAssignmentRuleGroupId());
        if (group == null || !Boolean.TRUE.equals(group.getEnabled())
                || !Objects.equals(group.getEnterpriseId(), mailbox.getEnterpriseId())) {
            log.warn("邮箱绑定的分配规则组不可用，跳过自动分配 mailboxId={} groupId={}",
                    mailboxId, mailbox.getAssignmentRuleGroupId());
            return null;
        }
        EnterpriseEntity enterprise = enterpriseMapper.selectById(group.getEnterpriseId());
        if (enterprise == null || !Boolean.TRUE.equals(enterprise.getEnabled())) {
            return null;
        }

        // 2、加载组内启用的非 DEFAULT 规则，数字越小优先级越高。
        List<AssignmentRuleEntity> rules = assignmentRuleMapper.selectList(new LambdaQueryWrapper<AssignmentRuleEntity>()
                .eq(AssignmentRuleEntity::getGroupId, group.getId())
                .eq(AssignmentRuleEntity::getEnabled, true)
                .ne(AssignmentRuleEntity::getMatchType, MATCH_DEFAULT)
                .orderByAsc(AssignmentRuleEntity::getPriorityOrder)
                .orderByAsc(AssignmentRuleEntity::getId));

        // 3、逐条判断匹配条件，命中后校验处理人仍可处理并可操作该邮箱。
        String effectiveMailboxAddress = normalize(mailboxAddress).isEmpty()
                ? mailbox.getEmailAddress()
                : mailboxAddress;
        for (AssignmentRuleEntity rule : rules) {
            if (MATCH_DEFAULT.equals(rule.getMatchType())) {
                continue;
            }
            if (!matches(rule, mailboxId, effectiveMailboxAddress, subject, fromEmail)) {
                continue;
            }
            UserEntity assignee = userMapper.selectById(rule.getAssigneeId());
            try {
                enterpriseMailboxAccessService.assertAssigneeCanAccessMailbox(rule.getAssigneeId(), mailboxId);
            } catch (BusinessException exception) {
                log.info("分配规则命中但处理人不可操作邮箱，继续尝试下一条 ruleId={} assigneeId={} mailboxId={}",
                        rule.getId(), rule.getAssigneeId(), mailboxId);
                continue;
            }
            if (assignee == null) {
                continue;
            }

            // 4、返回首条可执行规则，调用方负责回写工单策略快照和发送通知。
            return new AssignmentRuleMatchResult(
                    group.getId(),
                    rule.getId(),
                    rule.getRuleName(),
                    rule.getMatchType(),
                    rule.getMatchValue(),
                    assignee.getId(),
                    assignee.getDisplayName(),
                    assignee.getEmail(),
                    rule.getNotifyEnabled()
            );
        }

        // 5、无规则命中时返回空，是否使用邮箱默认处理人由 fallback 明确决定。
        return null;
    }

    /**
     * 查询分配规则列表。
     */
    public AssignmentRuleListResponse listRules(CurrentUserPrincipal principal, Long groupId, String keyword,
                                                Boolean enabled, String matchType) {
        permissionService.assertPermission(principal, "assignment_rule:read", "无权查看分配规则");
        LambdaQueryWrapper<AssignmentRuleEntity> wrapper = buildQuery(groupId, keyword, enabled, matchType)
                .orderByAsc(AssignmentRuleEntity::getPriorityOrder)
                .orderByAsc(AssignmentRuleEntity::getId);
        List<AssignmentRuleVO> records = assignmentRuleMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();
        return new AssignmentRuleListResponse(records, buildSummary(groupId));
    }

    /**
     * 新建分配规则。
     */
    @Transactional
    public AssignmentRuleVO createRule(CurrentUserPrincipal principal, AssignmentRuleSaveRequest request) {
        permissionService.assertPermission(principal, "assignment_rule:create", "无权新建分配规则");
        AssignmentRuleGroupEntity group = requireEnabledGroup(request.getGroupId());
        String matchType = normalizeManageMatchType(request.getMatchType());
        String matchValue = normalizeMatchValue(matchType, request.getMatchValue());
        boolean defaultRule = normalizeDefaultRule(request.getDefaultRule(), matchType);
        assertAssigneeValid(request.getAssigneeId(), group.getId());

        AssignmentRuleEntity rule = new AssignmentRuleEntity();
        fillRule(rule, request, matchType, matchValue, defaultRule, principal.account());
        rule.setCreatedBy(principal.account());
        rule.setUpdatedBy(principal.account());
        assignmentRuleMapper.insert(rule);

        recordLog(principal, "CREATE", rule.getId(), "新建分配规则：" + rule.getRuleName());
        return toVO(assignmentRuleMapper.selectById(rule.getId()));
    }

    /**
     * 编辑分配规则。
     */
    @Transactional
    public AssignmentRuleVO updateRule(CurrentUserPrincipal principal, Long id, AssignmentRuleSaveRequest request) {
        permissionService.assertPermission(principal, "assignment_rule:update", "无权编辑分配规则");
        AssignmentRuleEntity existing = requireRule(id);
        AssignmentRuleGroupEntity group = requireEnabledGroup(request.getGroupId());
        String matchType = normalizeManageMatchType(request.getMatchType());
        String matchValue = normalizeMatchValue(matchType, request.getMatchValue());
        boolean defaultRule = normalizeDefaultRule(request.getDefaultRule(), matchType);
        assertAssigneeValid(request.getAssigneeId(), group.getId());

        AssignmentRuleEntity update = new AssignmentRuleEntity();
        update.setId(id);
        fillRule(update, request, matchType, matchValue, defaultRule, principal.account());
        assignmentRuleMapper.updateById(update);

        recordLog(principal, "UPDATE", id, "编辑分配规则：" + existing.getRuleName());
        return toVO(assignmentRuleMapper.selectById(id));
    }

    /**
     * 启用或停用分配规则。
     */
    @Transactional
    public AssignmentRuleVO updateEnabled(CurrentUserPrincipal principal, Long id, AssignmentRuleEnabledRequest request) {
        permissionService.assertPermission(principal, "assignment_rule:enable", "无权启停分配规则");
        AssignmentRuleEntity existing = requireRule(id);
        assignmentRuleMapper.update(null, new LambdaUpdateWrapper<AssignmentRuleEntity>()
                .eq(AssignmentRuleEntity::getId, id)
                .set(AssignmentRuleEntity::getEnabled, request.getEnabled())
                .set(AssignmentRuleEntity::getUpdatedBy, principal.account()));

        recordLog(principal, Boolean.TRUE.equals(request.getEnabled()) ? "ENABLE" : "DISABLE", id,
                (Boolean.TRUE.equals(request.getEnabled()) ? "启用分配规则：" : "停用分配规则：") + existing.getRuleName());
        return toVO(assignmentRuleMapper.selectById(id));
    }

    /**
     * 批量更新规则优先级。
     */
    @Transactional
    public List<AssignmentRuleVO> sortRules(CurrentUserPrincipal principal, AssignmentRuleSortRequest request) {
        permissionService.assertPermission(principal, "assignment_rule:sort", "无权排序分配规则");
        Set<Long> ids = new HashSet<>();
        Set<Long> groupIds = new HashSet<>();
        for (AssignmentRuleSortItem item : request.rules()) {
            if (!ids.add(item.id())) {
                throw new BusinessException(CODE_BAD_REQUEST, "排序规则ID重复：" + item.id());
            }
        }
        for (AssignmentRuleSortItem item : request.rules()) {
            AssignmentRuleEntity rule = requireRule(item.id());
            if (rule.getGroupId() == null) {
                throw new BusinessException(CODE_BAD_REQUEST, "旧版未分组规则不能在新页面排序");
            }
            groupIds.add(rule.getGroupId());
        }
        if (groupIds.size() > 1) {
            throw new BusinessException(CODE_BAD_REQUEST, "只能在同一分配规则组内排序");
        }
        for (AssignmentRuleSortItem item : request.rules()) {
            assignmentRuleMapper.update(null, new LambdaUpdateWrapper<AssignmentRuleEntity>()
                    .eq(AssignmentRuleEntity::getId, item.id())
                    .set(AssignmentRuleEntity::getPriorityOrder, item.priorityOrder())
                    .set(AssignmentRuleEntity::getUpdatedBy, principal.account()));
        }
        recordLog(principal, "SORT", null, "调整分配规则排序：" + ids.size() + "条");
        return assignmentRuleMapper.selectList(new LambdaQueryWrapper<AssignmentRuleEntity>()
                        .in(AssignmentRuleEntity::getId, ids)
                        .orderByAsc(AssignmentRuleEntity::getPriorityOrder)
                        .orderByAsc(AssignmentRuleEntity::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 删除分配规则。
     */
    @Transactional
    public void deleteRule(CurrentUserPrincipal principal, Long id) {
        permissionService.assertPermission(principal, "assignment_rule:delete", "无权删除分配规则");
        AssignmentRuleEntity existing = requireRule(id);
        assignmentRuleMapper.deleteById(id);
        recordLog(principal, "DELETE", id, "删除分配规则：" + existing.getRuleName());
    }

    private LambdaQueryWrapper<AssignmentRuleEntity> buildQuery(Long groupId, String keyword,
                                                                 Boolean enabled, String matchType) {
        String normalizedKeyword = normalize(keyword);
        LambdaQueryWrapper<AssignmentRuleEntity> wrapper = new LambdaQueryWrapper<>();
        if (groupId != null) {
            wrapper.eq(AssignmentRuleEntity::getGroupId, groupId);
        }
        if (!normalizedKeyword.isEmpty()) {
            wrapper.and(query -> query
                    .like(AssignmentRuleEntity::getRuleName, normalizedKeyword)
                    .or()
                    .like(AssignmentRuleEntity::getMatchValue, normalizedKeyword));
        }
        if (enabled != null) {
            wrapper.eq(AssignmentRuleEntity::getEnabled, enabled);
        }
        String normalizedType = normalize(matchType).toUpperCase();
        if (!normalizedType.isEmpty()) {
            assertMatchType(normalizedType);
            wrapper.eq(AssignmentRuleEntity::getMatchType, normalizedType);
        }
        return wrapper;
    }

    private AssignmentRuleSummaryVO buildSummary(Long groupId) {
        long total = assignmentRuleMapper.selectCount(buildQuery(groupId, null, null, null));
        long enabled = assignmentRuleMapper.selectCount(
                buildQuery(groupId, null, true, null));
        long defaultCount = assignmentRuleMapper.selectCount(
                buildQuery(groupId, null, null, MATCH_DEFAULT));
        return new AssignmentRuleSummaryVO(total, enabled, total - enabled, defaultCount);
    }

    private void fillRule(AssignmentRuleEntity rule, AssignmentRuleSaveRequest request, String matchType,
                          String matchValue, boolean defaultRule, String operator) {
        rule.setGroupId(request.getGroupId());
        rule.setRuleName(normalize(request.getRuleName()));
        rule.setEnabled(request.getEnabled() == null || request.getEnabled());
        rule.setPriorityOrder(request.getPriorityOrder() == null ? 100 : request.getPriorityOrder());
        rule.setDefaultRule(defaultRule);
        rule.setMatchType(matchType);
        rule.setMatchValue(matchValue);
        rule.setAssigneeId(request.getAssigneeId());
        rule.setNotifyEnabled(request.getNotifyEnabled() == null || request.getNotifyEnabled());
        rule.setUpdatedBy(operator);
    }

    private AssignmentRuleEntity requireRule(Long id) {
        if (id == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "分配规则ID不能为空");
        }
        AssignmentRuleEntity rule = assignmentRuleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(CODE_NOT_FOUND, "分配规则不存在");
        }
        return rule;
    }

    private void assertAssigneeValid(Long assigneeId, Long groupId) {
        if (assigneeId == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "请选择分配目标处理人");
        }
        UserEntity assignee = userMapper.selectById(assigneeId);
        if (assignee == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "分配目标处理人不存在");
        }
        enterpriseMailboxAccessService.assertTicketProcessor(assigneeId);
        List<MailboxEntity> boundMailboxes = mailboxMapper.selectList(new LambdaQueryWrapper<MailboxEntity>()
                .eq(MailboxEntity::getAssignmentRuleGroupId, groupId));
        for (MailboxEntity mailbox : boundMailboxes) {
            enterpriseMailboxAccessService.assertAssigneeCanAccessMailbox(assigneeId, mailbox.getId());
        }
    }

    private boolean normalizeDefaultRule(Boolean defaultRule, String matchType) {
        if (Boolean.TRUE.equals(defaultRule) || MATCH_DEFAULT.equals(matchType)) {
            throw new BusinessException(CODE_BAD_REQUEST, "P3 分配规则不再支持 DEFAULT，请使用邮箱兜底策略");
        }
        return false;
    }

    private void ensureDefaultRuleUnique(boolean defaultRule, Long excludeId) {
        if (!defaultRule) {
            return;
        }
        LambdaQueryWrapper<AssignmentRuleEntity> wrapper = new LambdaQueryWrapper<AssignmentRuleEntity>()
                .eq(AssignmentRuleEntity::getDefaultRule, true);
        if (excludeId != null) {
            wrapper.ne(AssignmentRuleEntity::getId, excludeId);
        }
        Long count = assignmentRuleMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(CODE_CONFLICT, "默认分配规则已存在，请先编辑原默认规则");
        }
    }

    private String normalizeMatchType(String matchType) {
        String normalized = normalize(matchType).toUpperCase();
        assertMatchType(normalized);
        return normalized;
    }

    private String normalizeManageMatchType(String matchType) {
        String normalized = normalizeMatchType(matchType);
        if (MATCH_DEFAULT.equals(normalized)) {
            throw new BusinessException(CODE_BAD_REQUEST, "P3 分配规则不再支持 DEFAULT，请使用邮箱兜底策略");
        }
        return normalized;
    }

    private AssignmentRuleGroupEntity requireEnabledGroup(Long groupId) {
        if (groupId == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "请选择所属分配规则组");
        }
        AssignmentRuleGroupEntity group = assignmentRuleGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "分配规则组不存在");
        }
        if (!Boolean.TRUE.equals(group.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, "分配规则组已停用");
        }
        EnterpriseEntity enterprise = enterpriseMapper.selectById(group.getEnterpriseId());
        if (enterprise == null || !Boolean.TRUE.equals(enterprise.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, "分配规则组所属企业已停用");
        }
        return group;
    }

    private String normalizeMatchValue(String matchType, String matchValue) {
        if (MATCH_DEFAULT.equals(matchType)) {
            return null;
        }
        String normalized = normalize(matchValue);
        if (normalized.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "当前匹配类型需要填写匹配值");
        }
        if (MATCH_FROM_EMAIL.equals(matchType)) {
            return normalized.toLowerCase();
        }
        return normalized;
    }

    private void assertMatchType(String matchType) {
        if (!VALID_MATCH_TYPES.contains(matchType)) {
            throw new BusinessException(CODE_BAD_REQUEST, "匹配类型仅支持 DEFAULT/SUBJECT_KEYWORD/MAILBOX/FROM_EMAIL");
        }
    }

    private boolean matches(AssignmentRuleEntity rule, Long mailboxId, String mailboxAddress,
                            String subject, String fromEmail) {
        return switch (rule.getMatchType()) {
            case MATCH_DEFAULT -> true;
            case MATCH_SUBJECT_KEYWORD -> containsIgnoreCase(subject, rule.getMatchValue());
            case MATCH_MAILBOX -> matchesMailbox(rule.getMatchValue(), mailboxId, mailboxAddress);
            case MATCH_FROM_EMAIL -> equalsIgnoreCase(fromEmail, rule.getMatchValue());
            default -> false;
        };
    }

    private boolean matchesMailbox(String matchValue, Long mailboxId, String mailboxAddress) {
        String normalizedValue = normalize(matchValue);
        if (normalizedValue.isEmpty()) {
            return false;
        }
        if (mailboxId != null && normalizedValue.equals(String.valueOf(mailboxId))) {
            return true;
        }
        return equalsIgnoreCase(mailboxAddress, normalizedValue);
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        String normalizedSource = normalize(source);
        String normalizedKeyword = normalize(keyword);
        return !normalizedSource.isEmpty()
                && !normalizedKeyword.isEmpty()
                && normalizedSource.toLowerCase(Locale.ROOT).contains(normalizedKeyword.toLowerCase(Locale.ROOT));
    }

    private boolean equalsIgnoreCase(String source, String target) {
        String normalizedSource = normalize(source);
        String normalizedTarget = normalize(target);
        return !normalizedSource.isEmpty()
                && !normalizedTarget.isEmpty()
                && normalizedSource.equalsIgnoreCase(normalizedTarget);
    }

    private void recordLog(CurrentUserPrincipal principal, String actionCode, Long bizId, String content) {
        OperationLogEntity log = new OperationLogEntity();
        log.setOperator(principal.account());
        log.setModuleCode(MODULE_ASSIGNMENT_RULE);
        log.setActionCode(actionCode);
        log.setBizId(bizId == null ? null : String.valueOf(bizId));
        log.setContent(content);
        log.setCreatedBy(principal.account());
        log.setUpdatedBy(principal.account());
        operationLogMapper.insert(log);
    }

    private AssignmentRuleVO toVO(AssignmentRuleEntity rule) {
        UserEntity assignee = rule.getAssigneeId() == null ? null : userMapper.selectById(rule.getAssigneeId());
        return new AssignmentRuleVO(
                rule.getId(),
                rule.getGroupId(),
                rule.getRuleName(),
                rule.getEnabled(),
                rule.getPriorityOrder(),
                rule.getDefaultRule(),
                rule.getMatchType(),
                rule.getMatchValue(),
                rule.getAssigneeId(),
                assignee == null ? null : assignee.getDisplayName(),
                rule.getNotifyEnabled(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
