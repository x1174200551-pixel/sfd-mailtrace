package com.ntn.fziot.mailtrace.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.application.bizservice.ticket.TicketBizService;
import com.ntn.fziot.mailtrace.repox.mysql.entity.AssignmentRuleEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.AssignmentRuleGroupEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.CustomerEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.NotificationTemplateEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.SlaPolicyEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEventEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserDataGrantEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.WorkCalendarEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.AssignmentRuleGroupMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.AssignmentRuleMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.CustomerMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.SlaPolicyMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketEventMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserDataGrantMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.WorkCalendarMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MODEL-P4 本地真实数据库策略链路验收；业务数据随测试事务回滚，SMTP 使用 mock 避免外发。
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
class P4TicketStrategyLocalIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TicketBizService ticketBizService;
    @Autowired
    private EnterpriseMapper enterpriseMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserDataGrantMapper grantMapper;
    @Autowired
    private WorkCalendarMapper calendarMapper;
    @Autowired
    private SlaPolicyMapper slaPolicyMapper;
    @Autowired
    private AssignmentRuleGroupMapper groupMapper;
    @Autowired
    private AssignmentRuleMapper ruleMapper;
    @Autowired
    private NotificationTemplateMapper templateMapper;
    @Autowired
    private MailboxMapper mailboxMapper;
    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    private TicketEventMapper eventMapper;
    @Autowired
    private CustomerMapper customerMapper;

    @MockBean
    private MailSendService mailSendService;

    @Test
    void createTicket_shouldApplyOnlyMailboxBoundStrategiesAndPersistSnapshots() {
        String suffix = Long.toUnsignedString(System.nanoTime());
        EnterpriseEntity enterpriseA = insertEnterprise("P4验收企业A-" + suffix);
        EnterpriseEntity enterpriseB = insertEnterprise("P4验收企业B-" + suffix);
        UserEntity assignee = insertAssignee(suffix, enterpriseA.getId());

        WorkCalendarEntity calendar = insertCalendar(enterpriseA.getId(), suffix);
        SlaPolicyEntity policy = insertPolicy(enterpriseA.getId(), calendar.getId(), suffix);
        AssignmentRuleGroupEntity group = insertGroup(enterpriseA.getId(), suffix);
        AssignmentRuleEntity rule = insertRule(group.getId(), assignee.getId(), suffix);
        NotificationTemplateEntity template = insertTemplate(enterpriseA.getId(), suffix);
        MailboxEntity configuredMailbox = insertMailbox(
                enterpriseA.getId(), "p4-configured-" + suffix + "@example.com",
                template.getId(), policy.getId(), group.getId(), assignee.getId(), "NONE");

        AtomicInteger messageSequence = new AtomicInteger();
        when(mailSendService.sendRawMail(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> MailSendService.SendResult.ok(
                        "发送成功",
                        "<p4-auto-reply-" + messageSequence.incrementAndGet() + "@example.com>"));

        LocalDateTime mailAt = LocalDateTime.parse("2026-08-26T10:00:00");
        Long configuredTicketId = ticketBizService.createTicket(
                configuredMailbox.getId(), "P4VIP策略验收", " Same.Customer@Example.COM ", "策略客户",
                "正文", null, "<p4-configured-" + suffix + "@example.com>",
                null, null, mailAt);

        TicketEntity configuredTicket = ticketMapper.selectById(configuredTicketId);
        assertEquals(enterpriseA.getId(), configuredTicket.getEnterpriseId());
        assertEquals(configuredMailbox.getId(), configuredTicket.getMailboxId());
        assertEquals("same.customer@example.com", configuredTicket.getCustomerEmail());
        assertNotNull(configuredTicket.getCustomerId());
        assertEquals(policy.getId(), configuredTicket.getSlaPolicyId());
        assertNotNull(configuredTicket.getSlaResponseDeadline());
        assertEquals(template.getId(), configuredTicket.getAutoReplyTemplateId());
        assertEquals(group.getId(), configuredTicket.getAssignmentRuleGroupId());
        assertEquals(rule.getId(), configuredTicket.getAssignmentRuleId());
        assertEquals(assignee.getId(), configuredTicket.getAssigneeId());
        assertEquals(TicketBizService.STATUS_PROCESSING, configuredTicket.getStatus());
        verify(mailSendService).sendRawMail(
                configuredMailbox.getId(), "same.customer@example.com",
                "工单 " + configuredTicket.getTicketNo() + " 已创建",
                "主题：P4VIP策略验收",
                "AUTO_REPLY", configuredTicketId, template.getId(), "AUTO_REPLY");

        MailboxEntity emptyStrategyMailbox = insertMailbox(
                enterpriseA.getId(), "p4-empty-" + suffix + "@example.com",
                null, null, null, assignee.getId(), "NONE");
        Long emptyTicketId = ticketBizService.createTicket(
                emptyStrategyMailbox.getId(), "无策略验收", "same.customer@example.com", "策略客户",
                "正文", null, "<p4-empty-" + suffix + "@example.com>",
                null, null, mailAt.plusMinutes(1));
        TicketEntity emptyTicket = ticketMapper.selectById(emptyTicketId);
        assertNull(emptyTicket.getSlaPolicyId());
        assertNull(emptyTicket.getSlaResponseDeadline());
        assertNull(emptyTicket.getAutoReplyTemplateId());
        assertNull(emptyTicket.getAssignmentRuleGroupId());
        assertNull(emptyTicket.getAssignmentRuleId());
        assertNull(emptyTicket.getAssigneeId());
        assertEquals(TicketBizService.STATUS_PENDING_ASSIGN, emptyTicket.getStatus());
        assertTrue(eventMapper.selectCount(new LambdaQueryWrapper<TicketEventEntity>()
                .eq(TicketEventEntity::getTicketId, emptyTicketId)
                .eq(TicketEventEntity::getEventType, TicketBizService.EVENT_AUTO_REPLY_SKIPPED)) > 0);
        assertEquals(configuredTicket.getCustomerId(), emptyTicket.getCustomerId());

        NotificationTemplateEntity enterpriseBTemplate = insertTemplate(enterpriseB.getId(), suffix + "-b");
        MailboxEntity enterpriseBMailbox = insertMailbox(
                enterpriseB.getId(), "p4-enterprise-b-" + suffix + "@example.com",
                enterpriseBTemplate.getId(), null, null, null, "NONE");
        RuleBinding enterpriseBBinding = bindRuleAssigneeToMailbox(enterpriseBMailbox, suffix + "-b");
        Long enterpriseBTicketId = ticketBizService.createTicket(
                enterpriseBMailbox.getId(), "P4VIP跨企业客户验收", "same.customer@example.com", "策略客户",
                "正文", null, "<p4-enterprise-b-" + suffix + "@example.com>",
                null, null, mailAt.plusMinutes(2));
        TicketEntity enterpriseBTicket = ticketMapper.selectById(enterpriseBTicketId);
        assertEquals(enterpriseB.getId(), enterpriseBTicket.getEnterpriseId());
        assertEquals(enterpriseBMailbox.getAutoReplyTemplateId(), enterpriseBTicket.getAutoReplyTemplateId());
        assertNull(enterpriseBTicket.getSlaPolicyId());
        assertEquals(enterpriseBBinding.groupId(), enterpriseBTicket.getAssignmentRuleGroupId());
        assertEquals(enterpriseBBinding.ruleId(), enterpriseBTicket.getAssignmentRuleId());
        assertEquals(enterpriseBBinding.assigneeId(), enterpriseBTicket.getAssigneeId());
        assertEquals(TicketBizService.STATUS_PROCESSING, enterpriseBTicket.getStatus());
        assertNotEquals(configuredTicket.getCustomerId(), enterpriseBTicket.getCustomerId());
        assertEquals(2L, customerMapper.selectCount(new LambdaQueryWrapper<CustomerEntity>()
                .eq(CustomerEntity::getEmail, "same.customer@example.com")));
    }

    private RuleBinding insertGroupWithRuleForAssignee(Long enterpriseId, String suffix) {
        UserEntity assignee = insertAssignee(suffix, enterpriseId);
        AssignmentRuleGroupEntity group = insertGroup(enterpriseId, suffix);
        AssignmentRuleEntity rule = insertRule(group.getId(), assignee.getId(), suffix);
        return new RuleBinding(group.getId(), rule.getId(), assignee.getId());
    }

    private RuleBinding bindRuleAssigneeToMailbox(MailboxEntity mailbox, String suffix) {
        RuleBinding binding = insertGroupWithRuleForAssignee(mailbox.getEnterpriseId(), suffix + "-binding");
        mailbox.setAssignmentRuleGroupId(binding.groupId());
        mailboxMapper.updateById(mailbox);
        return binding;
    }

    private record RuleBinding(Long groupId, Long ruleId, Long assigneeId) {
    }

    private EnterpriseEntity insertEnterprise(String name) {
        EnterpriseEntity enterprise = new EnterpriseEntity();
        enterprise.setEnterpriseName(name);
        enterprise.setEnabled(true);
        enterprise.setCreatedBy("IT");
        enterprise.setUpdatedBy("IT");
        enterpriseMapper.insert(enterprise);
        return enterprise;
    }

    private UserEntity insertAssignee(String suffix, Long enterpriseId) {
        String roleCode = "IT_P4_" + suffix;
        Long roleId = insertAndReturnKey("""
                INSERT INTO mt_role (
                  role_code, role_name, role_desc, is_system, is_enabled, sort_order, created_by, updated_by
                ) VALUES (?, 'P4策略验收角色', '仅供事务回滚集成测试使用', 0, 1, 999, 'IT', 'IT')
                """, roleCode);
        jdbcTemplate.update("""
                INSERT INTO mt_role_permission (role_id, permission_id, created_by, updated_by)
                SELECT ?, id, 'IT', 'IT'
                  FROM mt_permission
                 WHERE permission_code = 'ticket:reply'
                   AND is_enabled = 1
                   AND is_deleted = 0
                """, roleId);

        UserEntity user = new UserEntity();
        user.setAccount("it-p4-" + suffix);
        user.setPasswordHash("IT-NOT-FOR-LOGIN");
        user.setDisplayName("P4策略验收处理人");
        user.setEmail("it-p4-" + suffix + "@example.com");
        user.setRoleCode(roleCode);
        user.setEnabled(true);
        user.setCreatedBy("IT");
        user.setUpdatedBy("IT");
        userMapper.insert(user);
        jdbcTemplate.update("""
                INSERT INTO mt_user_role (user_id, role_id, is_primary, created_by, updated_by)
                VALUES (?, ?, 1, 'IT', 'IT')
                """, user.getId(), roleId);

        UserDataGrantEntity grant = new UserDataGrantEntity();
        grant.setUserId(user.getId());
        grant.setGrantType("ENTERPRISE");
        grant.setEnterpriseId(enterpriseId);
        grant.setEnabled(true);
        grant.setCreatedBy("IT");
        grant.setUpdatedBy("IT");
        grantMapper.insert(grant);
        return user;
    }

    private WorkCalendarEntity insertCalendar(Long enterpriseId, String suffix) {
        WorkCalendarEntity calendar = new WorkCalendarEntity();
        calendar.setEnterpriseId(enterpriseId);
        calendar.setCalendarName("P4验收日历-" + suffix);
        calendar.setTimezone("Asia/Shanghai");
        calendar.setWorkdays("1,2,3,4,5");
        calendar.setWorkStartTime(java.time.LocalTime.parse("09:00"));
        calendar.setWorkEndTime(java.time.LocalTime.parse("18:00"));
        calendar.setDefaultCalendar(false);
        calendar.setCreatedBy("IT");
        calendar.setUpdatedBy("IT");
        calendarMapper.insert(calendar);
        return calendar;
    }

    private SlaPolicyEntity insertPolicy(Long enterpriseId, Long calendarId, String suffix) {
        SlaPolicyEntity policy = new SlaPolicyEntity();
        policy.setEnterpriseId(enterpriseId);
        policy.setPolicyName("P4验收SLA-" + suffix);
        policy.setEnabled(true);
        policy.setDefaultPolicy(false);
        policy.setResponseHours(4);
        policy.setResolveHours(24);
        policy.setWarningRemainHours(1);
        policy.setCalendarId(calendarId);
        policy.setCreatedBy("IT");
        policy.setUpdatedBy("IT");
        slaPolicyMapper.insert(policy);
        return policy;
    }

    private AssignmentRuleGroupEntity insertGroup(Long enterpriseId, String suffix) {
        AssignmentRuleGroupEntity group = new AssignmentRuleGroupEntity();
        group.setEnterpriseId(enterpriseId);
        group.setGroupName("P4验收规则组-" + suffix);
        group.setEnabled(true);
        group.setCreatedBy("IT");
        group.setUpdatedBy("IT");
        groupMapper.insert(group);
        return group;
    }

    private AssignmentRuleEntity insertRule(Long groupId, Long assigneeId, String suffix) {
        AssignmentRuleEntity rule = new AssignmentRuleEntity();
        rule.setGroupId(groupId);
        rule.setRuleName("P4VIP规则-" + suffix);
        rule.setEnabled(true);
        rule.setPriorityOrder(10);
        rule.setDefaultRule(false);
        rule.setMatchType("SUBJECT_KEYWORD");
        rule.setMatchValue("P4VIP");
        rule.setAssigneeId(assigneeId);
        rule.setNotifyEnabled(false);
        rule.setCreatedBy("IT");
        rule.setUpdatedBy("IT");
        ruleMapper.insert(rule);
        return rule;
    }

    private NotificationTemplateEntity insertTemplate(Long enterpriseId, String suffix) {
        NotificationTemplateEntity template = new NotificationTemplateEntity();
        template.setTemplateCode("P4_AUTO_REPLY_" + suffix);
        template.setTemplateType("AUTO_REPLY");
        template.setTemplateName("P4验收自动回复");
        template.setSubjectTpl("工单 {ticket_no} 已创建");
        template.setContentTpl("主题：{subject}");
        template.setEnabled(true);
        template.setCreatedBy("IT");
        template.setUpdatedBy("IT");
        templateMapper.insert(template);
        return template;
    }

    private MailboxEntity insertMailbox(Long enterpriseId, String email, Long templateId, Long policyId,
                                        Long groupId, Long defaultAssigneeId, String fallbackType) {
        MailboxEntity mailbox = new MailboxEntity();
        mailbox.setEnterpriseId(enterpriseId);
        mailbox.setMailboxName("P4验收邮箱-" + email);
        mailbox.setEmailAddress(email);
        mailbox.setEnabled(true);
        mailbox.setDefaultAssigneeId(defaultAssigneeId);
        mailbox.setImapHost("imap.example.com");
        mailbox.setImapPort(993);
        mailbox.setImapSslEnabled(true);
        mailbox.setImapUsername(email);
        mailbox.setImapPasswordEnc("IT-NOT-A-REAL-SECRET");
        mailbox.setImapFolder("INBOX");
        mailbox.setFetchIntervalSec(120);
        mailbox.setSmtpHost("smtp.example.com");
        mailbox.setSmtpPort(587);
        mailbox.setSmtpSslEnabled(true);
        mailbox.setSmtpUsername(email);
        mailbox.setSmtpPasswordEnc("IT-NOT-A-REAL-SECRET");
        mailbox.setAutoReplyEnabled(true);
        mailbox.setAutoReplyTemplateId(templateId);
        mailbox.setSlaPolicyId(policyId);
        mailbox.setAssignmentRuleGroupId(groupId);
        mailbox.setAssignmentFallbackType(fallbackType);
        mailbox.setConnectionStatus("UNKNOWN");
        mailbox.setCreatedBy("IT");
        mailbox.setUpdatedBy("IT");
        mailboxMapper.insert(mailbox);
        return mailbox;
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
