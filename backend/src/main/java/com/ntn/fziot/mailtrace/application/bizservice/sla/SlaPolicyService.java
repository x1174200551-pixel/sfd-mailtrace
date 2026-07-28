package com.ntn.fziot.mailtrace.application.bizservice.sla;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicyDefaultRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicyEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicyListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicySaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicySummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicyVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.SlaPolicyEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.SlaPolicyMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.WorkCalendarMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SlaPolicyService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_FORBIDDEN = 40302;
    private static final int CODE_NOT_FOUND = 40401;
    private static final int CODE_CONFLICT = 40901;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String MODULE_SLA_POLICY = "SLA_POLICY";

    private final SlaPolicyMapper slaPolicyMapper;
    private final WorkCalendarMapper workCalendarMapper;
    private final OperationLogMapper operationLogMapper;

    /**
     * 查询 SLA 策略列表。
     */
    public SlaPolicyListResponse listPolicies(CurrentUserPrincipal principal, String keyword,
                                              Boolean enabled, Boolean defaultPolicy) {
        // 1、仅管理员可进入 SLA 策略配置。
        assertAdmin(principal);

        // 2、按筛选条件查询策略，并保持默认策略、启用策略优先展示。
        LambdaQueryWrapper<SlaPolicyEntity> wrapper = buildQuery(keyword, enabled, defaultPolicy)
                .orderByDesc(SlaPolicyEntity::getDefaultPolicy)
                .orderByDesc(SlaPolicyEntity::getEnabled)
                .orderByAsc(SlaPolicyEntity::getId);
        List<SlaPolicyVO> records = slaPolicyMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();

        // 3、返回列表和摘要，供后续管理页顶部统计使用。
        return new SlaPolicyListResponse(records, buildSummary());
    }

    /**
     * 新建 SLA 策略。
     */
    @Transactional
    public SlaPolicyVO createPolicy(CurrentUserPrincipal principal, SlaPolicySaveRequest request) {
        // 1、校验管理员权限和策略字段组合。
        assertAdmin(principal);
        validatePolicy(request);
        boolean defaultPolicy = Boolean.TRUE.equals(request.getDefaultPolicy());
        ensureDefaultPolicyUnique(defaultPolicy, null);

        // 2、写入策略主体，当前阶段只保存配置，不计算工单 SLA 截止时间。
        SlaPolicyEntity policy = new SlaPolicyEntity();
        fillPolicy(policy, request, principal.account());
        policy.setCreatedBy(principal.account());
        policy.setUpdatedBy(principal.account());
        slaPolicyMapper.insert(policy);

        // 3、记录配置变更日志并返回最新数据。
        recordLog(principal, "CREATE", policy.getId(), "新建 SLA 策略：" + policy.getPolicyName());
        return toVO(slaPolicyMapper.selectById(policy.getId()));
    }

    /**
     * 编辑 SLA 策略。
     */
    @Transactional
    public SlaPolicyVO updatePolicy(CurrentUserPrincipal principal, Long id, SlaPolicySaveRequest request) {
        // 1、校验权限、策略存在性和字段组合。
        assertAdmin(principal);
        SlaPolicyEntity existing = requirePolicy(id);
        validatePolicy(request);
        boolean defaultPolicy = Boolean.TRUE.equals(request.getDefaultPolicy());
        ensureDefaultPolicyUnique(defaultPolicy, id);

        // 2、覆盖可编辑配置字段。
        SlaPolicyEntity update = new SlaPolicyEntity();
        update.setId(id);
        fillPolicy(update, request, principal.account());
        slaPolicyMapper.updateById(update);

        // 3、记录操作日志并返回最新策略。
        recordLog(principal, "UPDATE", id, "编辑 SLA 策略：" + existing.getPolicyName());
        return toVO(slaPolicyMapper.selectById(id));
    }

    /**
     * 启用或停用 SLA 策略。
     */
    @Transactional
    public SlaPolicyVO updateEnabled(CurrentUserPrincipal principal, Long id, SlaPolicyEnabledRequest request) {
        // 1、校验权限和策略存在性。
        assertAdmin(principal);
        SlaPolicyEntity existing = requirePolicy(id);

        // 2、停用默认策略前阻断，避免系统没有可用默认 SLA。
        if (Boolean.FALSE.equals(request.getEnabled()) && Boolean.TRUE.equals(existing.getDefaultPolicy())) {
            throw new BusinessException(CODE_BAD_REQUEST, "默认 SLA 策略不能停用，请先设置其他默认策略");
        }

        // 3、更新启停状态并写日志。
        slaPolicyMapper.update(null, new LambdaUpdateWrapper<SlaPolicyEntity>()
                .eq(SlaPolicyEntity::getId, id)
                .set(SlaPolicyEntity::getEnabled, request.getEnabled())
                .set(SlaPolicyEntity::getUpdatedBy, principal.account()));
        recordLog(principal, Boolean.TRUE.equals(request.getEnabled()) ? "ENABLE" : "DISABLE", id,
                (Boolean.TRUE.equals(request.getEnabled()) ? "启用 SLA 策略：" : "停用 SLA 策略：")
                        + existing.getPolicyName());

        // 4、返回最新策略。
        return toVO(slaPolicyMapper.selectById(id));
    }

    /**
     * 设置或取消默认 SLA 策略。
     */
    @Transactional
    public SlaPolicyVO updateDefault(CurrentUserPrincipal principal, Long id, SlaPolicyDefaultRequest request) {
        // 1、校验权限和策略存在性。
        assertAdmin(principal);
        SlaPolicyEntity existing = requirePolicy(id);

        // 2、该接口只允许设置新的默认策略，不允许把系统置为无默认策略。
        if (!Boolean.TRUE.equals(request.getDefaultPolicy())) {
            throw new BusinessException(CODE_BAD_REQUEST, "系统必须保留一个默认 SLA 策略");
        }
        if (!Boolean.TRUE.equals(existing.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, "停用的 SLA 策略不能设为默认");
        }

        // 3、设为默认前先清理其他默认策略，保证默认策略唯一。
        slaPolicyMapper.update(null, new LambdaUpdateWrapper<SlaPolicyEntity>()
                .ne(SlaPolicyEntity::getId, id)
                .eq(SlaPolicyEntity::getDefaultPolicy, true)
                .set(SlaPolicyEntity::getDefaultPolicy, false)
                .set(SlaPolicyEntity::getUpdatedBy, principal.account()));

        // 4、更新当前策略默认标识并记录日志。
        slaPolicyMapper.update(null, new LambdaUpdateWrapper<SlaPolicyEntity>()
                .eq(SlaPolicyEntity::getId, id)
                .set(SlaPolicyEntity::getDefaultPolicy, true)
                .set(SlaPolicyEntity::getUpdatedBy, principal.account()));
        recordLog(principal, "SET_DEFAULT", id, "设为默认 SLA 策略：" + existing.getPolicyName());

        // 5、返回最新策略。
        return toVO(slaPolicyMapper.selectById(id));
    }

    /**
     * 删除 SLA 策略。
     */
    @Transactional
    public void deletePolicy(CurrentUserPrincipal principal, Long id) {
        // 1、校验权限和策略存在性。
        assertAdmin(principal);
        SlaPolicyEntity existing = requirePolicy(id);

        // 2、默认策略不允许删除，避免新建工单没有默认 SLA 策略。
        if (Boolean.TRUE.equals(existing.getDefaultPolicy())) {
            throw new BusinessException(CODE_BAD_REQUEST, "默认 SLA 策略不能删除，请先设置其他默认策略");
        }

        // 3、逻辑删除并记录操作日志。
        slaPolicyMapper.deleteById(id);
        recordLog(principal, "DELETE", id, "删除 SLA 策略：" + existing.getPolicyName());
    }

    private LambdaQueryWrapper<SlaPolicyEntity> buildQuery(String keyword, Boolean enabled, Boolean defaultPolicy) {
        String normalizedKeyword = normalize(keyword);
        LambdaQueryWrapper<SlaPolicyEntity> wrapper = new LambdaQueryWrapper<>();
        if (!normalizedKeyword.isEmpty()) {
            wrapper.like(SlaPolicyEntity::getPolicyName, normalizedKeyword);
        }
        if (enabled != null) {
            wrapper.eq(SlaPolicyEntity::getEnabled, enabled);
        }
        if (defaultPolicy != null) {
            wrapper.eq(SlaPolicyEntity::getDefaultPolicy, defaultPolicy);
        }
        return wrapper;
    }

    private SlaPolicySummaryVO buildSummary() {
        long total = slaPolicyMapper.selectCount(new LambdaQueryWrapper<>());
        long enabled = slaPolicyMapper.selectCount(
                new LambdaQueryWrapper<SlaPolicyEntity>().eq(SlaPolicyEntity::getEnabled, true));
        long defaultCount = slaPolicyMapper.selectCount(
                new LambdaQueryWrapper<SlaPolicyEntity>().eq(SlaPolicyEntity::getDefaultPolicy, true));
        return new SlaPolicySummaryVO(total, enabled, total - enabled, defaultCount);
    }

    private void validatePolicy(SlaPolicySaveRequest request) {
        if (request.getResolveHours() != null && request.getResolveHours() < request.getResponseHours()) {
            throw new BusinessException(CODE_BAD_REQUEST, "解决时限不能小于首次响应时限");
        }
        if (request.getWarningRemainHours() >= request.getResponseHours()) {
            throw new BusinessException(CODE_BAD_REQUEST, "预警阈值必须小于首次响应时限");
        }
        if (request.getCalendarId() == null || request.getCalendarId() <= 0) {
            throw new BusinessException(CODE_BAD_REQUEST, "请选择工作日历");
        }
        if (workCalendarMapper.selectById(request.getCalendarId()) == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "工作日历不存在");
        }
        if (Boolean.TRUE.equals(request.getDefaultPolicy()) && Boolean.FALSE.equals(request.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, "默认 SLA 策略必须启用");
        }
    }

    private void ensureDefaultPolicyUnique(boolean defaultPolicy, Long excludeId) {
        if (!defaultPolicy) {
            return;
        }
        LambdaQueryWrapper<SlaPolicyEntity> wrapper = new LambdaQueryWrapper<SlaPolicyEntity>()
                .eq(SlaPolicyEntity::getDefaultPolicy, true);
        if (excludeId != null) {
            wrapper.ne(SlaPolicyEntity::getId, excludeId);
        }
        Long count = slaPolicyMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(CODE_CONFLICT, "默认 SLA 策略已存在，请先编辑原默认策略");
        }
    }

    private SlaPolicyEntity requirePolicy(Long id) {
        if (id == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "SLA 策略ID不能为空");
        }
        SlaPolicyEntity policy = slaPolicyMapper.selectById(id);
        if (policy == null) {
            throw new BusinessException(CODE_NOT_FOUND, "SLA 策略不存在");
        }
        return policy;
    }

    private void fillPolicy(SlaPolicyEntity policy, SlaPolicySaveRequest request, String operator) {
        policy.setPolicyName(normalize(request.getPolicyName()));
        policy.setEnabled(request.getEnabled() == null || request.getEnabled());
        policy.setDefaultPolicy(Boolean.TRUE.equals(request.getDefaultPolicy()));
        policy.setResponseHours(request.getResponseHours());
        policy.setResolveHours(request.getResolveHours());
        policy.setWarningRemainHours(request.getWarningRemainHours());
        policy.setEscalateAfterBreachHours(request.getEscalateAfterBreachHours());
        policy.setCalendarId(request.getCalendarId());
        policy.setUpdatedBy(operator);
    }

    private void recordLog(CurrentUserPrincipal principal, String actionCode, Long bizId, String content) {
        OperationLogEntity log = new OperationLogEntity();
        log.setOperator(principal.account());
        log.setModuleCode(MODULE_SLA_POLICY);
        log.setActionCode(actionCode);
        log.setBizId(bizId == null ? null : String.valueOf(bizId));
        log.setContent(content);
        log.setCreatedBy(principal.account());
        log.setUpdatedBy(principal.account());
        operationLogMapper.insert(log);
    }

    private SlaPolicyVO toVO(SlaPolicyEntity policy) {
        return new SlaPolicyVO(
                policy.getId(),
                policy.getPolicyName(),
                policy.getEnabled(),
                policy.getDefaultPolicy(),
                policy.getResponseHours(),
                policy.getResolveHours(),
                policy.getWarningRemainHours(),
                policy.getEscalateAfterBreachHours(),
                policy.getCalendarId(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }

    private void assertAdmin(CurrentUserPrincipal principal) {
        if (principal == null || !ROLE_ADMIN.equals(principal.roleCode())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅管理员可操作 SLA 策略");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
