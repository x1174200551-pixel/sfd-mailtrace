package com.ntn.fziot.mailtrace.integration;

import com.ntn.fziot.mailtrace.application.bizservice.assignment.AssignmentRuleGroupService;
import com.ntn.fziot.mailtrace.application.bizservice.calendar.WorkCalendarService;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.enterprise.EnterpriseService;
import com.ntn.fziot.mailtrace.application.bizservice.mailbox.MailboxService;
import com.ntn.fziot.mailtrace.application.bizservice.sla.SlaPolicyService;
import com.ntn.fziot.mailtrace.application.bizservice.template.NotificationTemplateService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleGroupSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.assignment.AssignmentRuleGroupVO;
import com.ntn.fziot.mailtrace.interfaces.vo.calendar.WorkCalendarSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.calendar.WorkCalendarVO;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseVO;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxVO;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicySaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.sla.SlaPolicyVO;
import com.ntn.fziot.mailtrace.interfaces.vo.template.NotificationTemplateCreateRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.template.NotificationTemplateVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 显式运行的 P3 本地配置链路测试；所有业务测试数据在事务结束后回滚。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.cloud.nacos.config.enabled=false",
                "mailtrace.mail.fetch-initial-delay-ms=86400000",
                "mailtrace.mail.retry-initial-delay-ms=86400000",
                "mailtrace.sla.check-initial-delay-ms=86400000"
        }
)
@Transactional
class P3ConfigurationLocalIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EnterpriseService enterpriseService;
    @Autowired
    private WorkCalendarService workCalendarService;
    @Autowired
    private SlaPolicyService slaPolicyService;
    @Autowired
    private AssignmentRuleGroupService groupService;
    @Autowired
    private NotificationTemplateService templateService;
    @Autowired
    private MailboxService mailboxService;

    @Test
    void enterpriseConfigurationChain_shouldPersistSameEnterpriseReferencesAndProtectDeletion() {
        String suffix = Long.toUnsignedString(System.nanoTime());
        long userId = 8_801_001L;
        String roleCode = "IT_P3_" + suffix;
        Long roleId = insertRole(roleCode);
        assignRole(userId, roleId);
        CurrentUserPrincipal principal = new CurrentUserPrincipal(
                userId, "it-p3-" + suffix, "P3本地验收用户", "it-p3@example.com", roleCode);

        EnterpriseSaveRequest enterpriseRequest = new EnterpriseSaveRequest();
        enterpriseRequest.setEnterpriseName("P3验收企业-" + suffix);
        enterpriseRequest.setEnabled(true);
        EnterpriseVO enterprise = enterpriseService.createEnterprise(principal, enterpriseRequest);
        insertEnterpriseGrant(userId, enterprise.id());

        WorkCalendarSaveRequest calendarRequest = new WorkCalendarSaveRequest();
        calendarRequest.setEnterpriseId(enterprise.id());
        calendarRequest.setCalendarName("P3验收日历-" + suffix);
        calendarRequest.setTimezone("Asia/Shanghai");
        calendarRequest.setWorkdays(List.of(1, 2, 3, 4, 5));
        calendarRequest.setWorkStartTime("09:00");
        calendarRequest.setWorkEndTime("18:00");
        calendarRequest.setDefaultCalendar(false);
        WorkCalendarVO calendar = workCalendarService.createCalendar(principal, calendarRequest);

        SlaPolicySaveRequest policyRequest = new SlaPolicySaveRequest();
        policyRequest.setEnterpriseId(enterprise.id());
        policyRequest.setPolicyName("P3验收SLA-" + suffix);
        policyRequest.setEnabled(true);
        policyRequest.setDefaultPolicy(false);
        policyRequest.setResponseHours(4);
        policyRequest.setResolveHours(24);
        policyRequest.setWarningRemainHours(1);
        policyRequest.setCalendarId(calendar.id());
        SlaPolicyVO policy = slaPolicyService.createPolicy(principal, policyRequest);

        AssignmentRuleGroupSaveRequest groupRequest = new AssignmentRuleGroupSaveRequest();
        groupRequest.setEnterpriseId(enterprise.id());
        groupRequest.setGroupName("P3验收规则组-" + suffix);
        groupRequest.setEnabled(true);
        AssignmentRuleGroupVO group = groupService.createGroup(principal, groupRequest);

        NotificationTemplateCreateRequest templateRequest = new NotificationTemplateCreateRequest();
        templateRequest.setTemplateType("AUTO_REPLY");
        templateRequest.setTemplateCode("P3_AUTO_REPLY_" + suffix);
        templateRequest.setTemplateName("P3验收自动回复");
        templateRequest.setSubjectTpl("工单 {ticket_no} 已受理");
        templateRequest.setContentTpl("您好，{subject} 已受理");
        templateRequest.setEnabled(true);
        NotificationTemplateVO template = templateService.createTemplate(principal, templateRequest);

        MailboxSaveRequest mailboxRequest = new MailboxSaveRequest();
        mailboxRequest.setEnterpriseId(enterprise.id());
        mailboxRequest.setMailboxName("P3验收邮箱-" + suffix);
        mailboxRequest.setEmailAddress("p3-" + suffix + "@example.com");
        mailboxRequest.setImapHost("imap.example.com");
        mailboxRequest.setImapPort(993);
        mailboxRequest.setImapSslEnabled(true);
        mailboxRequest.setImapUsername(mailboxRequest.getEmailAddress());
        mailboxRequest.setImapPassword("p3-imap-secret");
        mailboxRequest.setImapFolder("INBOX");
        mailboxRequest.setFetchIntervalSec(120);
        mailboxRequest.setSmtpHost("smtp.example.com");
        mailboxRequest.setSmtpPort(587);
        mailboxRequest.setSmtpSslEnabled(true);
        mailboxRequest.setSmtpUsername(mailboxRequest.getEmailAddress());
        mailboxRequest.setSmtpPassword("p3-smtp-secret");
        mailboxRequest.setAutoReplyEnabled(true);
        mailboxRequest.setAutoReplyTemplateId(template.id());
        mailboxRequest.setSlaPolicyId(policy.id());
        mailboxRequest.setAssignmentRuleGroupId(group.id());
        mailboxRequest.setAssignmentFallbackType("NONE");
        MailboxVO mailbox = mailboxService.createMailbox(principal, mailboxRequest);

        assertEquals(enterprise.id(), mailbox.enterpriseId());
        assertEquals(template.id(), mailbox.autoReplyTemplateId());
        assertEquals(policy.id(), mailbox.slaPolicyId());
        assertEquals(group.id(), mailbox.assignmentRuleGroupId());
        assertEquals(1, enterpriseService.listVisibleOptions(principal, true).stream()
                .filter(option -> option.id().equals(enterprise.id())).count());
        assertThrows(BusinessException.class, () -> templateService.deleteTemplate(principal, template.id()));
        assertThrows(BusinessException.class, () -> slaPolicyService.deletePolicy(principal, policy.id()));
        assertThrows(BusinessException.class, () -> groupService.deleteGroup(principal, group.id()));
    }

    private Long insertRole(String roleCode) {
        Long roleId = insertAndReturnKey("""
                INSERT INTO mt_role (
                  role_code, role_name, role_desc, is_system, is_enabled, sort_order, created_by, updated_by
                ) VALUES (?, 'P3本地验收角色', '仅供事务回滚集成测试使用', 0, 1, 999, 'IT', 'IT')
                """, roleCode);
        jdbcTemplate.update("""
                INSERT INTO mt_role_permission (role_id, permission_id, created_by, updated_by)
                SELECT ?, id, 'IT', 'IT'
                  FROM mt_permission
                 WHERE permission_code IN (
                   'enterprise:read', 'enterprise:create', 'enterprise:update', 'enterprise:enable',
                   'mailbox:read', 'mailbox:create', 'mailbox:update',
                   'notification_template:read', 'notification_template:create',
                   'notification_template:update', 'notification_template:delete',
                   'sla_policy:read', 'sla_policy:create', 'sla_policy:update', 'sla_policy:delete',
                   'work_calendar:read', 'work_calendar:create', 'work_calendar:update', 'work_calendar:delete',
                   'assignment_rule_group:read', 'assignment_rule_group:create',
                   'assignment_rule_group:update', 'assignment_rule_group:delete'
                 )
                   AND is_enabled = 1
                   AND is_deleted = 0
                """, roleId);
        return roleId;
    }

    private void assignRole(long userId, Long roleId) {
        jdbcTemplate.update("""
                INSERT INTO mt_user_role (user_id, role_id, is_primary, created_by, updated_by)
                VALUES (?, ?, 1, 'IT', 'IT')
                """, userId, roleId);
    }

    private void insertEnterpriseGrant(long userId, Long enterpriseId) {
        jdbcTemplate.update("""
                INSERT INTO mt_user_data_grant
                  (user_id, grant_type, enterprise_id, mailbox_id, is_enabled, created_by, updated_by)
                VALUES (?, 'ENTERPRISE', ?, NULL, 1, 'IT', 'IT')
                """, userId, enterpriseId);
    }

    private Long insertAndReturnKey(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < args.length; index++) {
                statement.setObject(index + 1, args[index]);
            }
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
