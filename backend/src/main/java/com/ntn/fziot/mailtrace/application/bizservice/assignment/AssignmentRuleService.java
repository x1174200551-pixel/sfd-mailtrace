package com.ntn.fziot.mailtrace.application.bizservice.assignment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
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
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.AssignmentRuleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
    private static final int CODE_FORBIDDEN = 40302;
    private static final int CODE_NOT_FOUND = 40401;
    private static final int CODE_CONFLICT = 40901;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_AGENT = "AGENT";
    private static final String MODULE_ASSIGNMENT_RULE = "ASSIGNMENT_RULE";
    private static final Set<String> VALID_MATCH_TYPES = Set.of(
            MATCH_DEFAULT, MATCH_SUBJECT_KEYWORD, MATCH_MAILBOX, MATCH_FROM_EMAIL
    );

    private final AssignmentRuleMapper assignmentRuleMapper;
    private final UserMapper userMapper;
    private final OperationLogMapper operationLogMapper;

    /**
     * 管理端测试规则匹配结果，不修改工单。
     */
    public AssignmentRuleMatchResponse testMatch(CurrentUserPrincipal principal, AssignmentRuleTestRequest request) {
        // 1、仅管理员允许模拟规则命中，避免处理人枚举规则配置。
        assertAdmin(principal);

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
        // 1、加载所有启用规则，数字越小优先级越高。
        List<AssignmentRuleEntity> rules = assignmentRuleMapper.selectList(new LambdaQueryWrapper<AssignmentRuleEntity>()
                .eq(AssignmentRuleEntity::getEnabled, true)
                .orderByAsc(AssignmentRuleEntity::getPriorityOrder)
                .orderByAsc(AssignmentRuleEntity::getId));

        // 2、逐条判断匹配条件，命中后再校验目标处理人是否仍可用。
        for (AssignmentRuleEntity rule : rules) {
            if (!matches(rule, mailboxId, mailboxAddress, subject, fromEmail)) {
                continue;
            }
            UserEntity assignee = userMapper.selectById(rule.getAssigneeId());
            if (!isAvailableAgent(assignee)) {
                log.info("分配规则命中但处理人无效，继续尝试下一条 ruleId={} assigneeId={}",
                        rule.getId(), rule.getAssigneeId());
                continue;
            }

            // 3、返回首条可执行规则，调用方负责回写工单和发送通知。
            return new AssignmentRuleMatchResult(
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

        // 4、无规则命中时返回空，由建单流程使用原邮箱默认处理人兜底。
        return null;
    }

    /**
     * 查询分配规则列表。
     */
    public AssignmentRuleListResponse listRules(CurrentUserPrincipal principal, String keyword,
                                                Boolean enabled, String matchType) {
        assertAdmin(principal);
        LambdaQueryWrapper<AssignmentRuleEntity> wrapper = buildQuery(keyword, enabled, matchType)
                .orderByAsc(AssignmentRuleEntity::getPriorityOrder)
                .orderByAsc(AssignmentRuleEntity::getId);
        List<AssignmentRuleVO> records = assignmentRuleMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();
        return new AssignmentRuleListResponse(records, buildSummary());
    }

    /**
     * 新建分配规则。
     */
    @Transactional
    public AssignmentRuleVO createRule(CurrentUserPrincipal principal, AssignmentRuleSaveRequest request) {
        assertAdmin(principal);
        String matchType = normalizeMatchType(request.getMatchType());
        String matchValue = normalizeMatchValue(matchType, request.getMatchValue());
        boolean defaultRule = normalizeDefaultRule(request.getDefaultRule(), matchType);
        assertAssigneeValid(request.getAssigneeId());
        ensureDefaultRuleUnique(defaultRule, null);

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
        assertAdmin(principal);
        AssignmentRuleEntity existing = requireRule(id);
        String matchType = normalizeMatchType(request.getMatchType());
        String matchValue = normalizeMatchValue(matchType, request.getMatchValue());
        boolean defaultRule = normalizeDefaultRule(request.getDefaultRule(), matchType);
        assertAssigneeValid(request.getAssigneeId());
        ensureDefaultRuleUnique(defaultRule, id);

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
        assertAdmin(principal);
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
        assertAdmin(principal);
        Set<Long> ids = new HashSet<>();
        for (AssignmentRuleSortItem item : request.rules()) {
            if (!ids.add(item.id())) {
                throw new BusinessException(CODE_BAD_REQUEST, "排序规则ID重复：" + item.id());
            }
        }
        for (AssignmentRuleSortItem item : request.rules()) {
            requireRule(item.id());
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
        assertAdmin(principal);
        AssignmentRuleEntity existing = requireRule(id);
        assignmentRuleMapper.deleteById(id);
        recordLog(principal, "DELETE", id, "删除分配规则：" + existing.getRuleName());
    }

    private LambdaQueryWrapper<AssignmentRuleEntity> buildQuery(String keyword, Boolean enabled, String matchType) {
        String normalizedKeyword = normalize(keyword);
        LambdaQueryWrapper<AssignmentRuleEntity> wrapper = new LambdaQueryWrapper<>();
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

    private AssignmentRuleSummaryVO buildSummary() {
        long total = assignmentRuleMapper.selectCount(new LambdaQueryWrapper<>());
        long enabled = assignmentRuleMapper.selectCount(
                new LambdaQueryWrapper<AssignmentRuleEntity>().eq(AssignmentRuleEntity::getEnabled, true));
        long defaultCount = assignmentRuleMapper.selectCount(
                new LambdaQueryWrapper<AssignmentRuleEntity>().eq(AssignmentRuleEntity::getDefaultRule, true));
        return new AssignmentRuleSummaryVO(total, enabled, total - enabled, defaultCount);
    }

    private void fillRule(AssignmentRuleEntity rule, AssignmentRuleSaveRequest request, String matchType,
                          String matchValue, boolean defaultRule, String operator) {
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

    private void assertAssigneeValid(Long assigneeId) {
        if (assigneeId == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "请选择分配目标处理人");
        }
        UserEntity assignee = userMapper.selectById(assigneeId);
        if (assignee == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "分配目标处理人不存在");
        }
        if (!isAvailableAgent(assignee)) {
            throw new BusinessException(CODE_BAD_REQUEST, "分配目标必须是启用的处理人账号");
        }
    }

    private boolean normalizeDefaultRule(Boolean defaultRule, String matchType) {
        if (Boolean.TRUE.equals(defaultRule) && !MATCH_DEFAULT.equals(matchType)) {
            throw new BusinessException(CODE_BAD_REQUEST, "默认规则的匹配类型必须为 DEFAULT");
        }
        return MATCH_DEFAULT.equals(matchType);
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

    private boolean isAvailableAgent(UserEntity assignee) {
        return assignee != null
                && ROLE_AGENT.equals(assignee.getRoleCode())
                && Boolean.TRUE.equals(assignee.getEnabled());
    }

    private void assertAdmin(CurrentUserPrincipal principal) {
        if (principal == null || !ROLE_ADMIN.equals(principal.roleCode())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅管理员可操作分配规则");
        }
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
