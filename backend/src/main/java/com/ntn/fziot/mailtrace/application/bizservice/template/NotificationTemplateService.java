package com.ntn.fziot.mailtrace.application.bizservice.template;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.template.NotificationTemplateCreateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.template.NotificationTemplateListResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.template.NotificationTemplateSummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.template.NotificationTemplateUpdateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.template.NotificationTemplateVO;
import com.ntn.fziot.mailtrace.interfaces.vo.template.TemplatePreviewRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.template.TemplatePreviewResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.template.TemplateVariableVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.NotificationTemplateEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_NOT_FOUND = 40401;
    private static final int CODE_CONFLICT = 40901;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[a-zA-Z0-9_]+}");
    private static final Set<String> TEMPLATE_TYPES = Set.of(
            "AUTO_REPLY", "ASSIGN_NOTIFY", "AGENT_REPLY", "SLA_WARNING", "SLA_BREACH", "SYSTEM");

    private static final Map<String, TemplateVariableVO> VARIABLES = buildVariables();

    private final NotificationTemplateMapper notificationTemplateMapper;
    private final MailboxMapper mailboxMapper;
    private final OperationLogMapper operationLogMapper;
    private final PermissionService permissionService;

    /**
     * 查询通知模板列表，并返回页面统计与可用变量。
     */
    public NotificationTemplateListResponse listTemplates(CurrentUserPrincipal principal, String templateType,
                                                            String keyword, Boolean enabled) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "notification_template:read", "无权查看通知模板");
        // 2、按关键字和启用状态拼装查询条件
        LambdaQueryWrapper<NotificationTemplateEntity> wrapper = buildQuery(templateType, keyword, enabled)
                .orderByAsc(NotificationTemplateEntity::getTemplateCode);
        // 3、查询模板列表并按编码排序
        List<NotificationTemplateVO> records = notificationTemplateMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();
        // 4、汇总模板总数、启用数、停用数和变量清单
        return new NotificationTemplateListResponse(
                records, buildSummary(templateType), List.copyOf(VARIABLES.values()));
    }

    /**
     * 新建通知模板。
     */
    @Transactional
    public NotificationTemplateVO createTemplate(CurrentUserPrincipal principal, NotificationTemplateCreateRequest request) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "notification_template:create", "无权新建通知模板");
        // 2、规范化模板编码，并校验编码唯一性
        String templateCode = normalize(request.getTemplateCode()).toUpperCase();
        String templateType = normalizeTemplateType(request.getTemplateType());
        ensureTemplateCodeUnique(templateCode, null);
        // 3、校验主题和正文中的变量都在白名单内
        validateVariables(request.getSubjectTpl());
        validateVariables(request.getContentTpl());

        // 4、写入新模板基础信息、启用状态和创建人
        NotificationTemplateEntity template = new NotificationTemplateEntity();
        template.setTemplateCode(templateCode);
        template.setTemplateType(templateType);
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
        permissionService.assertPermission(principal, "notification_template:update", "无权编辑通知模板");
        // 2、查询模板是否存在，防止保存无效 ID
        NotificationTemplateEntity existing = requireTemplate(id);
        String templateType = normalizeTemplateType(request.getTemplateType());
        ensureTemplateCodeUnique(existing.getTemplateCode(), id);
        if (isMailboxReferenced(id)
                && !templateType.equals(normalizeTemplateType(existing.getTemplateType()))) {
            throw new BusinessException(CODE_CONFLICT, "模板已被邮箱引用，不能变更模板类型");
        }
        if (isMailboxReferenced(id) && !Boolean.TRUE.equals(request.getEnabled())) {
            throw new BusinessException(CODE_CONFLICT, "模板已被邮箱引用，不能停用");
        }
        // 3、校验主题和正文中的变量都在白名单内
        validateVariables(request.getSubjectTpl());
        validateVariables(request.getContentTpl());

        // 4、更新模板名称、主题、正文、启用状态和更新人
        notificationTemplateMapper.update(null, new LambdaUpdateWrapper<NotificationTemplateEntity>()
                .eq(NotificationTemplateEntity::getId, id)
                .set(NotificationTemplateEntity::getTemplateType, templateType)
                .set(NotificationTemplateEntity::getTemplateName, normalize(request.getTemplateName()))
                .set(NotificationTemplateEntity::getSubjectTpl, normalize(request.getSubjectTpl()))
                .set(NotificationTemplateEntity::getContentTpl, normalize(request.getContentTpl()))
                .set(NotificationTemplateEntity::getEnabled, request.getEnabled())
                .set(NotificationTemplateEntity::getUpdatedBy, principal.account()));

        // 5、写入操作日志，便于后续审计
        recordLog(principal, "UPDATE", id, "保存通知模板：" + existing.getTemplateCode());
        return toVO(notificationTemplateMapper.selectById(id));
    }

    @Transactional
    public void deleteTemplate(CurrentUserPrincipal principal, Long id) {
        permissionService.assertPermission(principal, "notification_template:delete", "无权删除通知模板");
        NotificationTemplateEntity existing = requireTemplate(id);
        if (isMailboxReferenced(id)) {
            throw new BusinessException(CODE_CONFLICT, "通知模板已被邮箱引用，不能删除");
        }
        notificationTemplateMapper.deleteById(id);
        recordLog(principal, "DELETE", id, "删除通知模板：" + existing.getTemplateCode());
    }

    /**
     * 根据示例数据渲染模板预览。
     */
    public TemplatePreviewResponse preview(CurrentUserPrincipal principal, TemplatePreviewRequest request) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "notification_template:preview", "无权预览通知模板");
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

    private LambdaQueryWrapper<NotificationTemplateEntity> buildQuery(String templateType,
                                                                       String keyword, Boolean enabled) {
        String normalizedKeyword = normalize(keyword);
        LambdaQueryWrapper<NotificationTemplateEntity> wrapper = new LambdaQueryWrapper<>();
        String normalizedType = normalize(templateType).toUpperCase();
        if (!normalizedType.isEmpty()) {
            wrapper.eq(NotificationTemplateEntity::getTemplateType, normalizeTemplateType(normalizedType));
        }
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

    private NotificationTemplateSummaryVO buildSummary(String templateType) {
        long total = notificationTemplateMapper.selectCount(buildQuery(templateType, null, null));
        long enabled = notificationTemplateMapper.selectCount(
                buildQuery(templateType, null, true));
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

    private void ensureTemplateCodeUnique(String templateCode, Long excludeId) {
        LambdaQueryWrapper<NotificationTemplateEntity> wrapper =
                new LambdaQueryWrapper<NotificationTemplateEntity>()
                        .eq(NotificationTemplateEntity::getTemplateCode, templateCode);
        if (excludeId != null) {
            wrapper.ne(NotificationTemplateEntity::getId, excludeId);
        }
        Long count = notificationTemplateMapper.selectCount(
                wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(CODE_BAD_REQUEST, "模板编码已存在，请更换编码");
        }
    }

    private boolean isMailboxReferenced(Long templateId) {
        return mailboxMapper.selectCount(new LambdaQueryWrapper<MailboxEntity>()
                .and(wrapper -> wrapper
                        .eq(MailboxEntity::getAutoReplyTemplateId, templateId)
                        .or().eq(MailboxEntity::getAssignmentNotifyTemplateId, templateId)
                        .or().eq(MailboxEntity::getAgentReplyTemplateId, templateId)
                        .or().eq(MailboxEntity::getSlaWarningTemplateId, templateId)
                        .or().eq(MailboxEntity::getSlaBreachTemplateId, templateId))) > 0;
    }

    private String normalizeTemplateType(String templateType) {
        String normalized = normalize(templateType).toUpperCase();
        if (!TEMPLATE_TYPES.contains(normalized)) {
            throw new BusinessException(CODE_BAD_REQUEST, "模板类型不支持");
        }
        return normalized;
    }

    private Map<String, String> buildSampleData(Map<String, String> requestSampleData) {
        Map<String, String> sampleData = new LinkedHashMap<>();
        sampleData.put("ticket_no", "TCK-260821093012-482931");
        sampleData.put("subject", "订单物流查询");
        sampleData.put("customer_email", "customer@example.com");
        sampleData.put("customer_name", "张先生");
        sampleData.put("assignee_name", "李强");
        sampleData.put("mailbox_email", "service@ntn.fziot");
        sampleData.put("sla_deadline", "2026-07-22 18:00");
        sampleData.put("ticket_link", "https://mailtrace.local/tickets/TCK-260821093012-482931");
        sampleData.put("customer_ticket_url", "https://mailtrace.local/customer/tickets/TCK-260821093012-482931");
        sampleData.put("customer_ticket_code", "482931");
        sampleData.put("customer_ticket_expires_at", "2026-08-24 09:30");
        sampleData.put("reply_content", "您好，您的问题已经处理，请查看本次回复内容。");
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
                template.getTemplateType(),
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
        variables.put("{ticket_no}", new TemplateVariableVO("{ticket_no}", "工单号", "TCK-260821093012-482931"));
        variables.put("{subject}", new TemplateVariableVO("{subject}", "工单主题", "订单物流查询"));
        variables.put("{customer_email}", new TemplateVariableVO("{customer_email}", "客户邮箱", "customer@example.com"));
        variables.put("{customer_name}", new TemplateVariableVO("{customer_name}", "客户名称", "张先生"));
        variables.put("{assignee_name}", new TemplateVariableVO("{assignee_name}", "处理人", "李强"));
        variables.put("{mailbox_email}", new TemplateVariableVO("{mailbox_email}", "服务邮箱", "service@ntn.fziot"));
        variables.put("{sla_deadline}", new TemplateVariableVO("{sla_deadline}", "SLA 截止时间", "2026-07-22 18:00"));
        variables.put("{ticket_link}", new TemplateVariableVO("{ticket_link}", "工单链接", "https://mailtrace.local/tickets/TCK-260821093012-482931"));
        variables.put("{customer_ticket_url}", new TemplateVariableVO("{customer_ticket_url}", "客户查看链接", "https://mailtrace.local/customer/tickets/TCK-260821093012-482931"));
        variables.put("{customer_ticket_code}", new TemplateVariableVO("{customer_ticket_code}", "客户查看校验码", "482931"));
        variables.put("{customer_ticket_expires_at}", new TemplateVariableVO("{customer_ticket_expires_at}", "客户查看有效期", "2026-08-24 09:30"));
        variables.put("{reply_content}", new TemplateVariableVO("{reply_content}", "处理人回复内容", "您好，您的问题已经处理，请查看本次回复内容。"));
        return variables;
    }
}
