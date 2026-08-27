package com.ntn.fziot.mailtrace.integration;

import com.ntn.fziot.mailtrace.application.bizservice.attachment.TicketAttachmentService;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.customer.CustomerReadonlyService;
import com.ntn.fziot.mailtrace.application.bizservice.dashboard.DashboardService;
import com.ntn.fziot.mailtrace.application.bizservice.mailbox.MailboxService;
import com.ntn.fziot.mailtrace.application.bizservice.mailfetch.MailFetchLogBizService;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendLogBizService;
import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.ticket.TicketBizService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 仅在显式指定 -Dtest=PermissionCutoverLocalIT 时运行，使用本地 application.yml 数据库并事务回滚。
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
class PermissionCutoverLocalIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EnterpriseMailboxAccessService accessService;
    @Autowired
    private TicketBizService ticketBizService;
    @Autowired
    private TicketAttachmentService ticketAttachmentService;
    @Autowired
    private CustomerReadonlyService customerReadonlyService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private MailboxService mailboxService;
    @Autowired
    private MailFetchLogBizService fetchLogService;
    @Autowired
    private MailSendLogBizService sendLogService;

    @Test
    void twoEnterprisesThreeMailboxes_shouldEnforceReadAndOperationMatrix() {
        String suffix = Long.toUnsignedString(System.nanoTime());
        Long enterpriseA = insertEnterprise("权限验收企业A-" + suffix, true);
        Long enterpriseB = insertEnterprise("权限验收企业B-" + suffix, false);
        Long mailboxA1 = insertMailbox(enterpriseA, "权限验收A启用-" + suffix, "a1-" + suffix + "@example.com", true);
        Long mailboxA2 = insertMailbox(enterpriseA, "权限验收A停用-" + suffix, "a2-" + suffix + "@example.com", false);
        Long mailboxB1 = insertMailbox(enterpriseB, "权限验收B启用-" + suffix, "b1-" + suffix + "@example.com", true);

        long enterpriseGrantUserId = 8_800_001L;
        long mailboxGrantUserId = 8_800_002L;
        long emptyGrantUserId = 8_800_003L;
        String roleCode = "IT_PERMISSION_" + suffix;
        Long roleId = insertAcceptanceRole(roleCode);
        assignRole(enterpriseGrantUserId, roleId);
        assignRole(mailboxGrantUserId, roleId);
        assignRole(emptyGrantUserId, roleId);
        insertEnterpriseGrant(enterpriseGrantUserId, enterpriseA);
        insertMailboxGrant(mailboxGrantUserId, mailboxB1);

        CurrentUserPrincipal enterpriseUser = principal(enterpriseGrantUserId, roleCode);
        CurrentUserPrincipal mailboxUser = principal(mailboxGrantUserId, roleCode);
        CurrentUserPrincipal emptyUser = principal(emptyGrantUserId, roleCode);
        CurrentUserPrincipal admin = principal(8_800_004L, "ADMIN");

        assertEquals(Set.of(mailboxA1, mailboxA2), accessService.resolveReadableMailboxIds(enterpriseUser));
        assertEquals(Set.of(mailboxA1), accessService.resolveOperationalMailboxIds(enterpriseUser));
        assertEquals(Set.of(mailboxB1), accessService.resolveReadableMailboxIds(mailboxUser));
        assertTrue(accessService.resolveOperationalMailboxIds(mailboxUser).isEmpty());
        assertTrue(accessService.resolveReadableMailboxIds(emptyUser).isEmpty());

        assertTrue(accessService.resolveReadableMailboxIds(admin).containsAll(Set.of(mailboxA1, mailboxA2, mailboxB1)));
        assertTrue(accessService.resolveOperationalMailboxIds(admin).contains(mailboxA1));
        assertFalse(accessService.resolveOperationalMailboxIds(admin).contains(mailboxA2));
        assertFalse(accessService.resolveOperationalMailboxIds(admin).contains(mailboxB1));

        Long ticketA1 = insertTicket(enterpriseA, mailboxA1, "A1-" + suffix, "a1-customer@example.com");
        Long ticketA2 = insertTicket(enterpriseA, mailboxA2, "A2-" + suffix, "a2-customer@example.com");
        insertTicket(enterpriseB, mailboxB1, "B1-" + suffix, "b1-customer@example.com");
        insertFetchLog(enterpriseA, mailboxA1);
        insertFetchLog(enterpriseB, mailboxB1);
        insertSendLog(enterpriseA, mailboxA1);
        insertSendLog(enterpriseB, mailboxB1);

        assertEquals(2, ticketBizService.pageTickets(
                enterpriseUser, suffix, null, null, null, null, null, null, null, 1, 20).total());
        assertEquals(1, ticketBizService.pageTickets(
                mailboxUser, suffix, null, null, null, null, null, null, null, 1, 20).total());
        assertEquals(0, ticketBizService.pageTickets(
                emptyUser, suffix, null, null, null, null, null, null, null, 1, 20).total());
        assertEquals(2, customerReadonlyService.pageCustomers(enterpriseUser, null, null, null, 1, 20).total());
        assertEquals(1, customerReadonlyService.pageCustomers(mailboxUser, null, null, null, 1, 20).total());
        assertEquals(0, customerReadonlyService.pageCustomers(emptyUser, null, null, null, 1, 20).total());
        assertEquals(2, mailboxService.pageMailboxes(enterpriseUser, null, suffix, null, null, 1, 20).total());
        assertEquals(1, mailboxService.pageMailboxes(mailboxUser, null, suffix, null, null, 1, 20).total());
        assertEquals(0, mailboxService.pageMailboxes(emptyUser, null, suffix, null, null, 1, 20).total());
        assertEquals(2, dashboardService.summary(enterpriseUser).totalCount());
        assertEquals(1, dashboardService.summary(mailboxUser).totalCount());
        assertEquals(0, dashboardService.summary(emptyUser).totalCount());
        assertEquals(1, fetchLogService.stats(enterpriseUser).totalCount());
        assertEquals(1, fetchLogService.stats(mailboxUser).totalCount());
        assertEquals(0, fetchLogService.stats(emptyUser).totalCount());
        assertEquals(1, sendLogService.stats(enterpriseUser).totalCount());
        assertEquals(1, sendLogService.stats(mailboxUser).totalCount());
        assertEquals(0, sendLogService.stats(emptyUser).totalCount());

        assertEquals(ticketA2, ticketBizService.getTicket(enterpriseUser, ticketA2).id());
        assertThrows(BusinessException.class, () -> ticketBizService.claimTicket(enterpriseUser, ticketA2));
        assertEquals(ticketA2, ticketBizService.getTicket(admin, ticketA2).id());
        assertThrows(BusinessException.class, () -> ticketBizService.claimTicket(admin, ticketA2));
        BusinessException detailDenied = assertThrows(
                BusinessException.class,
                () -> ticketBizService.getTicket(mailboxUser, ticketA1));
        assertEquals(40302, detailDenied.getCode());

        BusinessException attachmentDenied = assertThrows(
                BusinessException.class,
                () -> ticketAttachmentService.download(ticketA1, 9_999_001L, mailboxUser));
        assertEquals(40302, attachmentDenied.getCode());

        BusinessException rawEmlDenied = assertThrows(
                BusinessException.class,
                () -> ticketBizService.downloadRawEml(mailboxUser, ticketA1, 9_999_002L));
        assertEquals(40302, rawEmlDenied.getCode());
    }

    private Long insertEnterprise(String name, boolean enabled) {
        return insertAndReturnKey(
                "INSERT INTO mt_enterprise (enterprise_name, is_enabled, created_by, updated_by) VALUES (?, ?, 'IT', 'IT')",
                name, enabled);
    }

    private Long insertAcceptanceRole(String roleCode) {
        Long roleId = insertAndReturnKey("""
                INSERT INTO mt_role (
                  role_code, role_name, role_desc, is_system, is_enabled, sort_order, created_by, updated_by
                ) VALUES (?, '权限切换本地验收角色', '仅供事务回滚集成测试使用', 0, 1, 999, 'IT', 'IT')
                """, roleCode);
        jdbcTemplate.update("""
                INSERT INTO mt_role_permission (role_id, permission_id, created_by, updated_by)
                SELECT ?, id, 'IT', 'IT'
                  FROM mt_permission
                 WHERE permission_code IN (
                   'dashboard:read', 'ticket:read', 'ticket:claim', 'ticket:reply',
                   'customer:read', 'mailbox:read', 'mail_fetch_log:read', 'mail_send_log:read'
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

    private Long insertMailbox(Long enterpriseId, String name, String email, boolean enabled) {
        return insertAndReturnKey("""
                INSERT INTO mt_mailbox (
                  enterprise_id, mailbox_name, email_address, is_enabled,
                  imap_host, imap_port, imap_ssl_enabled, imap_username, imap_password_enc, imap_folder, fetch_interval_sec,
                  smtp_host, smtp_port, smtp_ssl_enabled, smtp_username, smtp_password_enc,
                  auto_reply_enabled, connection_status, created_by, updated_by
                ) VALUES (?, ?, ?, ?, 'imap.example.com', 993, 1, ?, 'IT', 'INBOX', 120,
                          'smtp.example.com', 587, 1, ?, 'IT', 0, 'UNKNOWN', 'IT', 'IT')
                """, enterpriseId, name, email, enabled, email, email);
    }

    private void insertEnterpriseGrant(long userId, Long enterpriseId) {
        jdbcTemplate.update("""
                INSERT INTO mt_user_data_grant
                  (user_id, grant_type, enterprise_id, mailbox_id, is_enabled, created_by, updated_by)
                VALUES (?, 'ENTERPRISE', ?, NULL, 1, 'IT', 'IT')
                """, userId, enterpriseId);
    }

    private void insertMailboxGrant(long userId, Long mailboxId) {
        jdbcTemplate.update("""
                INSERT INTO mt_user_data_grant
                  (user_id, grant_type, enterprise_id, mailbox_id, is_enabled, created_by, updated_by)
                VALUES (?, 'MAILBOX', NULL, ?, 1, 'IT', 'IT')
                """, userId, mailboxId);
    }

    private Long insertTicket(Long enterpriseId, Long mailboxId, String ticketNo, String customerEmail) {
        return insertAndReturnKey("""
                INSERT INTO mt_ticket (
                  enterprise_id, ticket_no, subject, status, priority, mailbox_id, customer_email,
                  link_suspect, sla_breached, sla_warning_sent, sla_breach_notified, created_by, updated_by
                ) VALUES (?, ?, ?, 'PENDING_ASSIGN', 'NORMAL', ?, ?, 0, 0, 0, 0, 'IT', 'IT')
                """, enterpriseId, ticketNo, "权限验收-" + ticketNo, mailboxId, customerEmail);
    }

    private void insertFetchLog(Long enterpriseId, Long mailboxId) {
        jdbcTemplate.update("""
                INSERT INTO mt_mail_fetch_log (
                  mailbox_id, enterprise_id, trigger_type, started_at, finished_at, success,
                  fetched_count, created_ticket_count, linked_count, created_by, updated_by
                ) VALUES (?, ?, 'MANUAL', NOW(3), NOW(3), 1, 1, 1, 0, 'IT', 'IT')
                """, mailboxId, enterpriseId);
    }

    private void insertSendLog(Long enterpriseId, Long mailboxId) {
        jdbcTemplate.update("""
                INSERT INTO mt_mail_send_log (
                  mailbox_id, enterprise_id, send_type, to_address, subject, send_status,
                  retry_count, max_retry, created_by, updated_by
                ) VALUES (?, ?, 'TEST', 'receiver@example.com', '权限验收', 'SUCCESS', 0, 5, 'IT', 'IT')
                """, mailboxId, enterpriseId);
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

    private CurrentUserPrincipal principal(Long id, String roleCode) {
        return new CurrentUserPrincipal(id, "it-" + id, "本地验收用户", "it-" + id + "@example.com", roleCode);
    }
}
