package com.sfonda.mailtrace.application.bizservice.template;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sfonda.mailtrace.application.bizservice.common.BusinessException;
import com.sfonda.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.sfonda.mailtrace.interfaces.vo.template.NotificationTemplateCreateRequest;
import com.sfonda.mailtrace.interfaces.vo.template.NotificationTemplateListResponse;
import com.sfonda.mailtrace.interfaces.vo.template.NotificationTemplateSummaryVO;
import com.sfonda.mailtrace.interfaces.vo.template.NotificationTemplateUpdateRequest;
import com.sfonda.mailtrace.interfaces.vo.template.NotificationTemplateVO;
import com.sfonda.mailtrace.interfaces.vo.template.TemplatePreviewRequest;
import com.sfonda.mailtrace.interfaces.vo.template.TemplatePreviewResponse;
import com.sfonda.mailtrace.interfaces.vo.template.TemplateVariableVO;
import com.sfonda.mailtrace.repox.mysql.entity.NotificationTemplateEntity;
import com.sfonda.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.sfonda.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.sfonda.mailtrace.repox.mysql.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_FORBIDDEN = 40302;
    private static final int CODE_NOT_FOUND = 40401;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[a-zA-Z0-9_]+}");

    private static final Map<String, TemplateVariableVO> VARIABLES = buildVariables();

    private final NotificationTemplateMapper notificationTemplateMapper;
    private final OperationLogMapper operationLogMapper;

    /**
     * 查询通知模板列表，并返回页面统计与可用变量。
     */
    public NotificationTemplateListResponse listTemplates(CurrentUserPrincipal principal, String keyword, Boolean enabled) {
        // 1、校验当前用户具备管理员权限
        assertAdmin(principal);
        // 2、按关键字和启用状态拼装查询条件
        LambdaQueryWrapper<NotificationTemplateEntity> wrapper = buildQuery(keyword, enabled)
                .orderByAsc(NotificationTemplateEntity::getTemplateCode);
        // 3、查询模板列表并按编码排序
        List<NotificationTemplateVO> records = notificationTemplateMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();
        // 4、汇总模板总数、启用数、停用数和变量清单
        return new NotificationTemplateListResponse(records, buildSummary(), List.copyOf(VARIABLES.values()));
    }

    /**
     * 新建通知模板。
     */
    @Transactional
    public NotificationTemplateVO createTemplate(CurrentUserPrincipal principal, NotificationTemplateCreateRequest request) {
        // 1、校验当前用户具备管理员权限
        assertAdmin(principal);
        // 2、规范化模板编码，并校验编码唯一性
        String templateCode = normalize(request.getTemplateCode()).toUpperCase();
        ensureTemplateCodeUnique(templateCode);
        // 3、校验主题和正文中的变量都在白名单内
        validateVariables(request.getSubjectTpl());
        validateVariables(request.getContentTpl());

        // 4、写入新模板基础信息、启用状态和创建人
        NotificationTemplateEntity template = new NotificationTemplateEntity();
        template.setTemplateCode(templateCode);
        template.setTemplateName(normalize(request.getTemplateName()));
        template.setSubjectTpl(normalize(request.getSubjectTpl()));
        template.setContentTpl(normalize(request.getContentTpl()));
        template.setEnabled(request.getEnabled());
        template.setCreatedBy(principal.account());
        template.setUpdatedBy(principal.account());
        notificationTemplateMapper.insert(template);

        // 5、写入模板创建操作日志并返回新模板详情
        recordLog(principal, "CREATE", template.getId(), "新建通知模板：" + templateCode);
        return toVO(notificationTemplateMapper.selectById(template.getId()));
    }

    /**
     * 保存通知模板配置。
     */
    @Transactional
    public NotificationTemplateVO updateTemplate(CurrentUserPrincipal principal, Long id,
                                                 NotificationTemplateUpdateRequest request) {
        // 1、校验当前用户具备管理员权限
        assertAdmin(principal);
        // 2、查询模板是否存在，防止保存无效 ID
        NotificationTemplateEntity existing = requireTemplate(id);
        // 3、校验主题和正文中的变量都在白名单内
        validateVariables(request.getSubjectTpl());
        validateVariables(request.getContentTpl());

        // 4、更新模板名称、主题、正文、启用状态和更新人
        notificationTemplateMapper.update(null, new LambdaUpdateWrapper<NotificationTemplateEntity>()
                .eq(NotificationTemplateEntity::getId, id)
                .set(NotificationTemplateEntity::getTemplateName, normalize(request.getTemplateName()))
                .set(NotificationTemplateEntity::getSubjectTpl, normalize(request.getSubjectTpl()))
                .set(NotificationTemplateEntity::getContentTpl, normalize(request.getContentTpl()))
                .set(NotificationTemplateEntity::getEnabled, request.getEnabled())
                .set(NotificationTemplateEntity::getUpdatedBy, principal.account()));

        // 5、写入操作日志，便于后续审计
        recordLog(principal, "UPDATE", id, "保存通知模板：" + existing.getTemplateCode());
        return toVO(notificationTemplateMapper.selectById(id));
    }

    /**
     * 根据示例数据渲染模板预览。
     */
    public TemplatePreviewResponse preview(CurrentUserPrincipal principal, TemplatePreviewRequest request) {
        // 1、校验当前用户具备管理员权限
        assertAdmin(principal);
        // 2、校验主题和正文中的变量都在白名单内
        validateVariables(request.getSubjectTpl());
        validateVariables(request.getContentTpl());
        // 3、合并默认示例数据和前端传入的示例值
        Map<String, String> sampleData = buildSampleData(request.getSampleData());
        // 4、替换主题和正文变量，返回预览结果
        return new TemplatePreviewResponse(
                render(request.getSubjectTpl(), sampleData),
                render(request.getContentTpl(), sampleData)
        );
    }

    private LambdaQueryWrapper<NotificationTemplateEntity> buildQuery(String keyword, Boolean enabled) {
        String normalizedKeyword = normalize(keyword);
        LambdaQueryWrapper<NotificationTemplateEntity> wrapper = new LambdaQueryWrapper<>();
        if (!normalizedKeyword.isEmpty()) {
            wrapper.and(query -> query
                    .like(NotificationTemplateEntity::getTemplateCode, normalizedKeyword)
                    .or()
                    .like(NotificationTemplateEntity::getTemplateName, normalizedKeyword));
        }
        if (enabled != null) {
            wrapper.eq(NotificationTemplateEntity::getEnabled, enabled);
        }
        return wrapper;
    }

    private NotificationTemplateSummaryVO buildSummary() {
        long total = notificationTemplateMapper.selectCount(new LambdaQueryWrapper<>());
        long enabled = notificationTemplateMapper.selectCount(
                new LambdaQueryWrapper<NotificationTemplateEntity>().eq(NotificationTemplateEntity::getEnabled, true));
        return new NotificationTemplateSummaryVO(total, enabled, total - enabled, VARIABLES.size());
    }

    private NotificationTemplateEntity requireTemplate(Long id) {
        if (id == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "模板ID不能为空");
        }
        NotificationTemplateEntity template = notificationTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(CODE_NOT_FOUND, "通知模板不存在");
        }
        return template;
    }

    private void validateVariables(String value) {
        Matcher matcher = VARIABLE_PATTERN.matcher(normalize(value));
        while (matcher.find()) {
            String variable = matcher.group();
            if (!VARIABLES.containsKey(variable)) {
                throw new BusinessException(CODE_BAD_REQUEST, "模板变量不支持：" + variable);
            }
        }
    }

    private void ensureTemplateCodeUnique(String templateCode) {
        Long count = notificationTemplateMapper.selectCount(
                new LambdaQueryWrapper<NotificationTemplateEntity>()
                        .eq(NotificationTemplateEntity::getTemplateCode, templateCode));
        if (count != null && count > 0) {
            throw new BusinessException(CODE_BAD_REQUEST, "模板编码已存在，请更换编码");
        }
    }

    private Map<String, String> buildSampleData(Map<String, String> requestSampleData) {
        Map<String, String> sampleData = new LinkedHashMap<>();
        sampleData.put("ticket_no", "TCK-20260722-0001");
        sampleData.put("subject", "订单物流查询");
        sampleData.put("customer_email", "customer@example.com");
        sampleData.put("customer_name", "张先生");
        sampleData.put("assignee_name", "李强");
        sampleData.put("mailbox_email", "service@sfonda.local");
        sampleData.put("sla_deadline", "2026-07-22 18:00");
        sampleData.put("ticket_link", "https://mailtrace.local/tickets/TCK-20260722-0001");
        if (requestSampleData != null) {
            requestSampleData.forEach((key, value) -> {
                if (key != null && value != null) {
                    sampleData.put(toSnakeCase(key), value);
                }
            });
        }
        return sampleData;
    }

    private String render(String template, Map<String, String> sampleData) {
        String rendered = normalize(template);
        for (Map.Entry<String, String> entry : sampleData.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return rendered;
    }

    private void assertAdmin(CurrentUserPrincipal principal) {
        if (principal == null || !ROLE_ADMIN.equals(principal.roleCode())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅管理员可操作通知模板");
        }
    }

    private void recordLog(CurrentUserPrincipal principal, String actionCode, Long bizId, String content) {
        OperationLogEntity log = new OperationLogEntity();
        log.setOperator(principal.account());
        log.setModuleCode("TEMPLATE");
        log.setActionCode(actionCode);
        log.setBizId(String.valueOf(bizId));
        log.setContent(content);
        log.setCreatedBy(principal.account());
        log.setUpdatedBy(principal.account());
        operationLogMapper.insert(log);
    }

    private NotificationTemplateVO toVO(NotificationTemplateEntity template) {
        return new NotificationTemplateVO(
                template.getId(),
                template.getTemplateCode(),
                template.getTemplateName(),
                template.getSubjectTpl(),
                template.getContentTpl(),
                template.getEnabled(),
                template.getUpdatedAt()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String toSnakeCase(String value) {
        return normalize(value)
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace('-', '_')
                .toLowerCase();
    }

    private static Map<String, TemplateVariableVO> buildVariables() {
        Map<String, TemplateVariableVO> variables = new LinkedHashMap<>();
        variables.put("{ticket_no}", new TemplateVariableVO("{ticket_no}", "工单号", "TCK-20260722-0001"));
        variables.put("{subject}", new TemplateVariableVO("{subject}", "工单主题", "订单物流查询"));
        variables.put("{customer_email}", new TemplateVariableVO("{customer_email}", "客户邮箱", "customer@example.com"));
        variables.put("{customer_name}", new TemplateVariableVO("{customer_name}", "客户名称", "张先生"));
        variables.put("{assignee_name}", new TemplateVariableVO("{assignee_name}", "处理人", "李强"));
        variables.put("{mailbox_email}", new TemplateVariableVO("{mailbox_email}", "服务邮箱", "service@sfonda.local"));
        variables.put("{sla_deadline}", new TemplateVariableVO("{sla_deadline}", "SLA 截止时间", "2026-07-22 18:00"));
        variables.put("{ticket_link}", new TemplateVariableVO("{ticket_link}", "工单链接", "https://mailtrace.local/tickets/TCK-20260722-0001"));
        return variables;
    }
}
