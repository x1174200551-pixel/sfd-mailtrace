package com.ntn.fziot.mailtrace.application.bizservice.mailbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.crypto.MailPasswordCipher;
import com.ntn.fziot.mailtrace.infrastructure.mail.ImapStoreSupport;
import com.ntn.fziot.mailtrace.infrastructure.cache.MtRedisCacheDoubleDelete;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxConnectionTestRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxConnectionTestResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxOptionVO;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxSummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.AssignmentRuleGroupEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailFetchLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.NotificationTemplateEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.SlaPolicyEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.AssignmentRuleGroupMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailFetchLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.SlaPolicyMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import jakarta.mail.Folder;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailboxService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_NOT_FOUND = 40401;
    private static final int CODE_CONFLICT = 40901;
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String STATUS_OK = "OK";
    private static final String STATUS_ERROR = "ERROR";
    private static final String TEST_ALL = "ALL";
    private static final String TEST_IMAP = "IMAP";
    private static final String TEST_SMTP = "SMTP";
    private static final String FALLBACK_NONE = "NONE";
    private static final String FALLBACK_DEFAULT_ASSIGNEE = "DEFAULT_ASSIGNEE";
    private static final String TEMPLATE_AUTO_REPLY = "AUTO_REPLY";
    private static final String TEMPLATE_ASSIGN_NOTIFY = "ASSIGN_NOTIFY";
    private static final String TEMPLATE_AGENT_REPLY = "AGENT_REPLY";
    private static final String TEMPLATE_SLA_WARNING = "SLA_WARNING";
    private static final String TEMPLATE_SLA_BREACH = "SLA_BREACH";

    private final MailboxMapper mailboxMapper;
    private final MailFetchLogMapper mailFetchLogMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final NotificationTemplateMapper notificationTemplateMapper;
    private final SlaPolicyMapper slaPolicyMapper;
    private final AssignmentRuleGroupMapper assignmentRuleGroupMapper;
    private final UserMapper userMapper;
    private final OperationLogMapper operationLogMapper;
    private final MailPasswordCipher mailPasswordCipher;
    private final PermissionService permissionService;
    private final EnterpriseMailboxAccessService enterpriseMailboxAccessService;

    /**
     * 分页查询邮箱配置列表。
     */
    public MailboxPageResponse pageMailboxes(CurrentUserPrincipal principal, Long enterpriseId, String keyword,
                                             String status, Boolean enabled, Integer page, Integer size) {
        // 1、校验当前用户具备邮箱配置页面入口或查看权限
        assertMailboxReadable(principal);
        // 2、规范化分页参数并按关键字、启用状态、连接状态构建查询条件
        long currentPage = normalizePage(page);
        long pageSize = normalizeSize(size);
        Set<Long> readableMailboxIds = enterpriseMailboxAccessService.resolveReadableMailboxIds(principal);
        LambdaQueryWrapper<MailboxEntity> wrapper = buildQuery(enterpriseId, keyword, status, enabled)
                .orderByDesc(MailboxEntity::getUpdatedAt)
                .orderByDesc(MailboxEntity::getId);
        applyMailboxScope(wrapper, readableMailboxIds);
        // 3、执行分页查询并转换成页面可用 VO
        Page<MailboxEntity> result = mailboxMapper.selectPage(Page.of(currentPage, pageSize), wrapper);
        // 4、汇总邮箱统计摘要并返回列表响应
        return new MailboxPageResponse(
                result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getPages(),
                buildSummary(readableMailboxIds)
        );
    }

    public List<MailboxOptionVO> listVisibleOptions(CurrentUserPrincipal principal, Long enterpriseId,
                                                     Boolean operationalOnly) {
        Set<Long> mailboxIds = Boolean.TRUE.equals(operationalOnly)
                ? enterpriseMailboxAccessService.resolveOperationalMailboxIds(principal)
                : enterpriseMailboxAccessService.resolveReadableMailboxIds(principal);
        if (mailboxIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<MailboxEntity> wrapper = new LambdaQueryWrapper<MailboxEntity>()
                .in(MailboxEntity::getId, mailboxIds)
                .orderByAsc(MailboxEntity::getMailboxName)
                .orderByAsc(MailboxEntity::getId);
        if (enterpriseId != null) {
            wrapper.eq(MailboxEntity::getEnterpriseId, enterpriseId);
        }
        return mailboxMapper.selectList(wrapper).stream()
                .map(mailbox -> new MailboxOptionVO(mailbox.getId(), mailbox.getEnterpriseId(),
                        mailbox.getMailboxName(), mailbox.getEmailAddress(), mailbox.getEnabled()))
                .toList();
    }

    /**
     * 新建邮箱配置，密码或授权码加密后入库。
     */
    @Transactional
    @MtRedisCacheDoubleDelete(cacheName = "access-catalog", key = "'all'", delayMillis = 500)
    public MailboxVO createMailbox(CurrentUserPrincipal principal, MailboxSaveRequest request) {
        // 1、校验当前用户具备邮箱新建权限
        permissionService.assertPermission(principal, "mailbox:create", "无权新建邮箱配置");
        // 2、校验邮箱地址唯一性和默认处理人合法性
        String emailAddress = normalizeLower(request.getEmailAddress());
        ensureEmailUnique(emailAddress, null);
        validateMailboxRelations(principal, request);
        // 3、校验新建时必须填写 IMAP/SMTP 密码或授权码
        assertCreatePasswordPresent(request);
        // 4、填充邮箱基础信息、IMAP/SMTP 配置，并加密保存密码或授权码
        MailboxEntity mailbox = new MailboxEntity();
        fillMailbox(mailbox, request, principal.account(), true);
        mailbox.setEmailAddress(emailAddress);
        mailbox.setConnectionStatus(STATUS_UNKNOWN);
        mailbox.setCreatedBy(principal.account());
        mailbox.setUpdatedBy(principal.account());
        mailboxMapper.insert(mailbox);
        assertDefaultAssigneeCanAccessMailbox(request.getDefaultAssigneeId(), mailbox.getId());
        // 5、写入操作日志并返回新建后的邮箱详情
        recordLog(principal, "CREATE", mailbox.getId(), "新建邮箱配置：" + mailbox.getEmailAddress());
        return toVO(mailboxMapper.selectById(mailbox.getId()));
    }

    /**
     * 编辑邮箱配置，密码为空时沿用原密文。
     */
    @Transactional
    @MtRedisCacheDoubleDelete(cacheName = "access-catalog", key = "'all'", delayMillis = 500)
    public MailboxVO updateMailbox(CurrentUserPrincipal principal, Long id, MailboxSaveRequest request) {
        // 1、校验当前用户具备邮箱编辑权限
        permissionService.assertPermission(principal, "mailbox:update", "无权编辑邮箱配置");
        // 2、查询目标邮箱并校验邮箱地址唯一性
        MailboxEntity existing = requireMailbox(id);
        enterpriseMailboxAccessService.assertMailboxConfigurable(principal, id);
        String emailAddress = normalizeLower(request.getEmailAddress());
        ensureEmailUnique(emailAddress, id);
        validateMailboxRelations(principal, request);
        // 3、更新基础信息、IMAP/SMTP 配置；未填写密码时保留原密文
        MailboxEntity next = new MailboxEntity();
        next.setId(id);
        next.setImapPasswordEnc(existing.getImapPasswordEnc());
        next.setSmtpPasswordEnc(existing.getSmtpPasswordEnc());
        fillMailbox(next, request, principal.account(), false);
        next.setEmailAddress(emailAddress);
        mailboxMapper.updateById(next);
        assertDefaultAssigneeCanAccessMailbox(request.getDefaultAssigneeId(), id);
        // 4、写入操作日志并返回最新邮箱详情
        recordLog(principal, "UPDATE", id, "编辑邮箱配置：" + emailAddress);
        return toVO(mailboxMapper.selectById(id));
    }

    /**
     * 启用或停用邮箱配置。
     */
    @Transactional
    @MtRedisCacheDoubleDelete(cacheName = "access-catalog", key = "'all'", delayMillis = 500)
    public MailboxVO updateEnabled(CurrentUserPrincipal principal, Long id, MailboxEnabledRequest request) {
        // 1、校验当前用户具备邮箱启停权限
        permissionService.assertPermission(principal, "mailbox:enable", "无权启停邮箱配置");
        // 2、查询目标邮箱是否存在
        MailboxEntity existing = requireMailbox(id);
        enterpriseMailboxAccessService.assertMailboxConfigurable(principal, id);
        // 3、更新启用状态和更新人
        mailboxMapper.update(null, new LambdaUpdateWrapper<MailboxEntity>()
                .eq(MailboxEntity::getId, id)
                .set(MailboxEntity::getEnabled, request.getEnabled())
                .set(MailboxEntity::getUpdatedBy, principal.account()));
        // 4、写入操作日志并返回最新邮箱详情
        recordLog(principal, Boolean.TRUE.equals(request.getEnabled()) ? "ENABLE" : "DISABLE", id,
                (Boolean.TRUE.equals(request.getEnabled()) ? "启用邮箱：" : "停用邮箱：") + existing.getEmailAddress());
        return toVO(mailboxMapper.selectById(id));
    }

    /**
     * 删除邮箱配置，历史邮件、工单和发送记录保留。
     */
    @Transactional
    @MtRedisCacheDoubleDelete(cacheName = "access-catalog", key = "'all'", delayMillis = 500)
    public void deleteMailbox(CurrentUserPrincipal principal, Long id) {
        // 1、校验当前用户具备邮箱删除权限
        permissionService.assertPermission(principal, "mailbox:delete", "无权删除邮箱配置");
        // 2、查询目标邮箱是否存在
        MailboxEntity existing = requireMailbox(id);
        enterpriseMailboxAccessService.assertMailboxConfigurable(principal, id);
        // 3、执行逻辑删除，保留历史引用数据
        mailboxMapper.deleteById(id);
        // 4、写入删除操作日志
        recordLog(principal, "DELETE", id, "删除邮箱配置：" + existing.getEmailAddress());
    }

    /**
     * 对已保存邮箱执行连接测试，并回写连接状态。
     */
    @Transactional
    public MailboxConnectionTestResponse testSavedMailbox(CurrentUserPrincipal principal, Long id, String testType) {
        long start = System.currentTimeMillis();
        // 1、校验当前用户具备邮箱连接测试权限
        permissionService.assertPermission(principal, "mailbox:test_connection", "无权测试邮箱连接");
        // 2、读取已保存邮箱并解密 IMAP/SMTP 密码
        MailboxEntity mailbox = requireMailbox(id);
        enterpriseMailboxAccessService.assertMailboxConfigurable(principal, id);
        log.info("开始已保存邮箱连接测试 mailboxId={} email={} testType={} operator={}",
                id, mailbox.getEmailAddress(), testType, principal.account());
        // 3、按测试类型分别执行 IMAP 和 SMTP 连接测试
        MailboxConnectionTestResponse response = testConnection(
                toConnectionConfig(mailbox, mailPasswordCipher.decrypt(mailbox.getImapPasswordEnc()),
                        mailPasswordCipher.decrypt(mailbox.getSmtpPasswordEnc())),
                normalizeTestType(testType)
        );
        // 4、回写连接状态并记录测试日志
        mailboxMapper.update(null, new LambdaUpdateWrapper<MailboxEntity>()
                .eq(MailboxEntity::getId, id)
                .set(MailboxEntity::getConnectionStatus, response.success() ? STATUS_OK : STATUS_ERROR)
                .set(MailboxEntity::getUpdatedBy, principal.account()));
        long elapsed = System.currentTimeMillis() - start;
        log.info("已保存邮箱连接测试完成 mailboxId={} email={} success={} imapOk={} smtpOk={} 耗时={}ms",
                id, mailbox.getEmailAddress(), response.success(), response.imapSuccess(), response.smtpSuccess(), elapsed);
        recordLog(principal, "TEST_CONNECTION", id,
                "测试邮箱连接：" + mailbox.getEmailAddress() + "，结果：" + response.connectionStatus());
        // 5、返回连接测试结果
        return response;
    }

    /**
     * 对页面草稿配置执行连接测试，不写入数据库。
     */
    public MailboxConnectionTestResponse testDraftMailbox(CurrentUserPrincipal principal, MailboxConnectionTestRequest request) {
        long start = System.currentTimeMillis();
        // 1、校验当前用户具备邮箱连接测试权限
        permissionService.assertPermission(principal, "mailbox:test_connection", "无权测试邮箱连接");
        // 2、规范化连接测试类型
        String testType = normalizeTestType(request.getTestType());
        log.info("开始草稿邮箱连接测试 testType={} imap={}:{} smtp={}:{} operator={}",
                testType,
                request.getImapHost(), request.getImapPort(),
                request.getSmtpHost(), request.getSmtpPort(),
                principal.account());
        // 3、规范化草稿配置，并按测试类型要求输入对应明文密码或授权码
        MailboxConnectionConfig config = toConnectionConfig(request, testType);
        // 4、按测试类型分别执行 IMAP 和 SMTP 连接测试
        MailboxConnectionTestResponse response = testConnection(config, testType);
        long elapsed = System.currentTimeMillis() - start;
        log.info("草稿邮箱连接测试完成 testType={} success={} imapOk={} smtpOk={} 耗时={}ms",
                testType, response.success(), response.imapSuccess(), response.smtpSuccess(), elapsed);
        return response;
    }

    private LambdaQueryWrapper<MailboxEntity> buildQuery(Long enterpriseId, String keyword, String status,
                                                          Boolean enabled) {
        String normalizedKeyword = normalize(keyword);
        String normalizedStatus = normalize(status).toUpperCase();
        LambdaQueryWrapper<MailboxEntity> wrapper = new LambdaQueryWrapper<>();
        if (enterpriseId != null) {
            wrapper.eq(MailboxEntity::getEnterpriseId, enterpriseId);
        }
        if (!normalizedKeyword.isEmpty()) {
            wrapper.and(query -> query
                    .like(MailboxEntity::getMailboxName, normalizedKeyword)
                    .or()
                    .like(MailboxEntity::getEmailAddress, normalizedKeyword)
                    .or()
                    .like(MailboxEntity::getImapHost, normalizedKeyword)
                    .or()
                    .like(MailboxEntity::getSmtpHost, normalizedKeyword));
        }
        if (enabled != null) {
            wrapper.eq(MailboxEntity::getEnabled, enabled);
        }
        if (!normalizedStatus.isEmpty() && !"ALL".equals(normalizedStatus) && !"DISABLED".equals(normalizedStatus)) {
            wrapper.eq(MailboxEntity::getConnectionStatus, normalizedStatus);
        }
        if ("DISABLED".equals(normalizedStatus)) {
            wrapper.eq(MailboxEntity::getEnabled, false);
        }
        return wrapper;
    }

    private void assertMailboxReadable(CurrentUserPrincipal principal) {
        permissionService.assertPermission(principal, "mailbox:read", "无权查看邮箱配置");
    }

    private MailboxSummaryVO buildSummary(Set<Long> readableMailboxIds) {
        if (readableMailboxIds.isEmpty()) {
            return new MailboxSummaryVO(0, 0, 0, 0, 0, 0, 0, 0);
        }
        long total = mailboxMapper.selectCount(scopedMailboxQuery(readableMailboxIds));
        long enabled = mailboxMapper.selectCount(scopedMailboxQuery(readableMailboxIds)
                .eq(MailboxEntity::getEnabled, true));
        long ok = mailboxMapper.selectCount(scopedMailboxQuery(readableMailboxIds)
                .eq(MailboxEntity::getConnectionStatus, STATUS_OK));
        long error = mailboxMapper.selectCount(scopedMailboxQuery(readableMailboxIds)
                .eq(MailboxEntity::getConnectionStatus, STATUS_ERROR));
        long unknown = mailboxMapper.selectCount(scopedMailboxQuery(readableMailboxIds)
                .eq(MailboxEntity::getConnectionStatus, STATUS_UNKNOWN));
        LocalDate today = LocalDate.now();
        List<MailFetchLogEntity> todayFetchLogs = mailFetchLogMapper.selectList(
                new LambdaQueryWrapper<MailFetchLogEntity>()
                        .in(MailFetchLogEntity::getMailboxId, readableMailboxIds)
                        .ge(MailFetchLogEntity::getStartedAt, today.atStartOfDay())
                        .lt(MailFetchLogEntity::getStartedAt, today.plusDays(1).atStartOfDay()));
        long todayReceivedMailCount = todayFetchLogs.stream()
                .mapToLong(log -> safeCount(log.getFetchedCount()))
                .sum();
        long todayCreatedTicketCount = todayFetchLogs.stream()
                .mapToLong(log -> safeCount(log.getCreatedTicketCount()))
                .sum();
        return new MailboxSummaryVO(total, enabled, total - enabled, ok, error, unknown,
                todayReceivedMailCount, todayCreatedTicketCount);
    }

    private long safeCount(Integer value) {
        return value == null ? 0L : Math.max(0, value);
    }

    private LambdaQueryWrapper<MailboxEntity> scopedMailboxQuery(Set<Long> mailboxIds) {
        return new LambdaQueryWrapper<MailboxEntity>().in(MailboxEntity::getId, mailboxIds);
    }

    private void applyMailboxScope(LambdaQueryWrapper<MailboxEntity> wrapper, Set<Long> mailboxIds) {
        if (mailboxIds.isEmpty()) {
            wrapper.apply("1 = 0");
            return;
        }
        wrapper.in(MailboxEntity::getId, mailboxIds);
    }

    private void fillMailbox(MailboxEntity mailbox, MailboxSaveRequest request, String operator, boolean create) {
        mailbox.setEnterpriseId(request.getEnterpriseId());
        mailbox.setMailboxName(normalize(request.getMailboxName()));
        mailbox.setEnabled(request.getEnabled() == null || request.getEnabled());
        mailbox.setDefaultAssigneeId(request.getDefaultAssigneeId());
        mailbox.setImapHost(normalize(request.getImapHost()));
        mailbox.setImapPort(normalizePort(request.getImapPort(), 993));
        mailbox.setImapSslEnabled(request.getImapSslEnabled() == null || request.getImapSslEnabled());
        mailbox.setImapUsername(normalize(request.getImapUsername()));
        mailbox.setImapFolder(normalizeFolder(request.getImapFolder()));
        mailbox.setFetchIntervalSec(normalizeFetchInterval(request.getFetchIntervalSec()));
        mailbox.setSmtpHost(normalize(request.getSmtpHost()));
        mailbox.setSmtpPort(normalizePort(request.getSmtpPort(), 587));
        mailbox.setSmtpSslEnabled(request.getSmtpSslEnabled() == null || request.getSmtpSslEnabled());
        mailbox.setSmtpUsername(normalize(request.getSmtpUsername()));
        mailbox.setSmtpFromName(normalizeNullable(request.getSmtpFromName()));
        mailbox.setAutoReplyEnabled(request.getAutoReplyEnabled() == null || request.getAutoReplyEnabled());
        mailbox.setAutoReplyTemplateId(request.getAutoReplyTemplateId());
        mailbox.setAssignmentNotifyTemplateId(request.getAssignmentNotifyTemplateId());
        mailbox.setAgentReplyTemplateId(request.getAgentReplyTemplateId());
        mailbox.setSlaWarningTemplateId(request.getSlaWarningTemplateId());
        mailbox.setSlaBreachTemplateId(request.getSlaBreachTemplateId());
        mailbox.setSlaPolicyId(request.getSlaPolicyId());
        mailbox.setAssignmentRuleGroupId(request.getAssignmentRuleGroupId());
        mailbox.setAssignmentFallbackType(normalizeFallbackType(request.getAssignmentFallbackType()));
        mailbox.setUpdatedBy(operator);
        if (create || !normalize(request.getImapPassword()).isEmpty()) {
            mailbox.setImapPasswordEnc(mailPasswordCipher.encrypt(request.getImapPassword()));
        }
        if (create || !normalize(request.getSmtpPassword()).isEmpty()) {
            mailbox.setSmtpPasswordEnc(mailPasswordCipher.encrypt(request.getSmtpPassword()));
        }
    }

    private void assertCreatePasswordPresent(MailboxSaveRequest request) {
        if (normalize(request.getImapPassword()).isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "新建邮箱时请输入 IMAP 密码或授权码");
        }
        if (normalize(request.getSmtpPassword()).isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "新建邮箱时请输入 SMTP 密码或授权码");
        }
    }

    private MailboxConnectionTestResponse testConnection(MailboxConnectionConfig config, String testType) {
        TestPartResult imap = shouldTestImap(testType) ? testImap(config) : skipped("未执行 IMAP 测试");
        TestPartResult smtp = shouldTestSmtp(testType) ? testSmtp(config) : skipped("未执行 SMTP 测试");
        boolean success = (!shouldTestImap(testType) || imap.success()) && (!shouldTestSmtp(testType) || smtp.success());
        return new MailboxConnectionTestResponse(
                success,
                success ? STATUS_OK : STATUS_ERROR,
                imap.success(),
                imap.message(),
                smtp.success(),
                smtp.message(),
                LocalDateTime.now()
        );
    }

    private TestPartResult testImap(MailboxConnectionConfig config) {
        long start = System.currentTimeMillis();
        log.info("开始 IMAP 连接测试 host={} port={} ssl={} username={} folder={}",
                config.imapHost(), config.imapPort(), config.imapSslEnabled(), config.imapUsername(), config.imapFolder());
        Store store = null;
        Folder folder = null;
        try {
            Properties properties = baseMailProperties("mail." + (config.imapSslEnabled() ? "imaps" : "imap"));
            Session session = Session.getInstance(properties);
            store = session.getStore(config.imapSslEnabled() ? "imaps" : "imap");
            store.connect(config.imapHost(), config.imapPort(), config.imapUsername(), config.imapPassword());
            log.info("IMAP 服务器连接成功 host={} port={} username={} 耗时={}ms",
                    config.imapHost(), config.imapPort(), config.imapUsername(), System.currentTimeMillis() - start);
            // 网易等邮箱要求登录后发送 ID，否则后续打开文件夹会失败
            ImapStoreSupport.identifyClient(store);
            folder = store.getFolder(config.imapFolder());
            if (!folder.exists()) {
                log.warn("IMAP 连接成功但文件夹不存在 host={} folder={} username={}",
                        config.imapHost(), config.imapFolder(), config.imapUsername());
                return fail("IMAP 连接成功，但收件文件夹不存在：" + config.imapFolder());
            }
            // 真正打开文件夹，与定时拉信路径一致，避免"测试通过、拉取失败"
            folder.open(Folder.READ_ONLY);
            long elapsed = System.currentTimeMillis() - start;
            log.info("IMAP 测试通过 host={} folder={} 总耗时={}ms",
                    config.imapHost(), config.imapFolder(), elapsed);
            return ok("IMAP 连接成功，收件文件夹可访问");
        } catch (Exception exception) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("IMAP 测试失败 host={} port={} username={} folder={} 耗时={}ms error={}",
                    config.imapHost(), config.imapPort(), config.imapUsername(), config.imapFolder(),
                    elapsed, normalizeException(exception), exception);
            return fail("IMAP 连接失败：" + normalizeException(exception));
        } finally {
            closeFolder(folder);
            closeStore(store);
        }
    }

    private TestPartResult testSmtp(MailboxConnectionConfig config) {
        long start = System.currentTimeMillis();
        log.info("开始 SMTP 连接测试 host={} port={} ssl={} username={}",
                config.smtpHost(), config.smtpPort(), config.smtpSslEnabled(), config.smtpUsername());
        Transport transport = null;
        try {
            // 1、465 + SSL 走隐式 SSL（smtps）；其余端口在开启 SSL 时走 STARTTLS（smtp）
            boolean implicitSsl = Boolean.TRUE.equals(config.smtpSslEnabled()) && config.smtpPort() == 465;
            String protocol = implicitSsl ? "smtps" : "smtp";
            String prefix = "mail." + protocol;
            Properties properties = baseMailProperties(prefix);
            properties.put(prefix + ".auth", "true");
        properties.put(prefix + ".starttls.enable", String.valueOf(Boolean.TRUE.equals(config.smtpSslEnabled()) && !implicitSsl));
        properties.put(prefix + ".ssl.enable", String.valueOf(implicitSsl));
        // smtp.163.com:587 的 STARTTLS 存在兼容性问题（连接后无响应），
        // 如遇到 15 秒超时 "Exception reading response"，请改用 465 端口 + SSL
        log.debug("SMTP 协议选择 protocol={} implicitSsl={} starttls={} ssl={}",
                protocol, implicitSsl,
                Boolean.TRUE.equals(config.smtpSslEnabled()) && !implicitSsl,
                implicitSsl);
            // 2、建立会话并使用账号/授权码连接 SMTP
            Session session = Session.getInstance(properties);
            transport = session.getTransport(protocol);
            transport.connect(config.smtpHost(), config.smtpPort(), config.smtpUsername(), config.smtpPassword());
            long elapsed = System.currentTimeMillis() - start;
            log.info("SMTP 测试通过 host={} port={} username={} 耗时={}ms",
                    config.smtpHost(), config.smtpPort(), config.smtpUsername(), elapsed);
            return ok("SMTP 连接成功，账号认证通过");
        } catch (Exception exception) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("SMTP 测试失败 host={} port={} ssl={} username={} 耗时={}ms error={}",
                    config.smtpHost(), config.smtpPort(), config.smtpSslEnabled(), config.smtpUsername(),
                    elapsed, normalizeException(exception), exception);
            return fail("SMTP 连接失败：" + normalizeException(exception));
        } finally {
            closeTransport(transport);
        }
    }

    private Properties baseMailProperties(String prefix) {
        Properties properties = new Properties();
        properties.put(prefix + ".connectiontimeout", "15000");
        properties.put(prefix + ".timeout", "15000");
        properties.put(prefix + ".writetimeout", "15000");
        return properties;
    }

    private MailboxConnectionConfig toConnectionConfig(MailboxEntity mailbox, String imapPassword, String smtpPassword) {
        return new MailboxConnectionConfig(
                mailbox.getImapHost(),
                mailbox.getImapPort(),
                mailbox.getImapSslEnabled(),
                mailbox.getImapUsername(),
                imapPassword,
                mailbox.getImapFolder(),
                mailbox.getSmtpHost(),
                mailbox.getSmtpPort(),
                mailbox.getSmtpSslEnabled(),
                mailbox.getSmtpUsername(),
                smtpPassword
        );
    }

    private MailboxConnectionConfig toConnectionConfig(MailboxConnectionTestRequest request, String testType) {
        if (shouldTestImap(testType) && normalize(request.getImapPassword()).isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "测试连接前请输入 IMAP 密码或授权码");
        }
        if (shouldTestSmtp(testType) && normalize(request.getSmtpPassword()).isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "测试连接前请输入 SMTP 密码或授权码");
        }
        return new MailboxConnectionConfig(
                normalize(request.getImapHost()),
                normalizePort(request.getImapPort(), 993),
                request.getImapSslEnabled() == null || request.getImapSslEnabled(),
                normalize(request.getImapUsername()),
                normalize(request.getImapPassword()),
                normalizeFolder(request.getImapFolder()),
                normalize(request.getSmtpHost()),
                normalizePort(request.getSmtpPort(), 587),
                request.getSmtpSslEnabled() == null || request.getSmtpSslEnabled(),
                normalize(request.getSmtpUsername()),
                normalize(request.getSmtpPassword())
        );
    }

    private MailboxEntity requireMailbox(Long id) {
        if (id == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "邮箱ID不能为空");
        }
        MailboxEntity mailbox = mailboxMapper.selectById(id);
        if (mailbox == null) {
            throw new BusinessException(CODE_NOT_FOUND, "邮箱配置不存在");
        }
        return mailbox;
    }

    private void ensureEmailUnique(String emailAddress, Long excludeId) {
        LambdaQueryWrapper<MailboxEntity> wrapper = new LambdaQueryWrapper<MailboxEntity>()
                .eq(MailboxEntity::getEmailAddress, emailAddress);
        if (excludeId != null) {
            wrapper.ne(MailboxEntity::getId, excludeId);
        }
        Long count = mailboxMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(CODE_CONFLICT, "邮箱地址已存在，请更换邮箱地址");
        }
    }

    private void validateMailboxRelations(CurrentUserPrincipal principal, MailboxSaveRequest request) {
        Long enterpriseId = request.getEnterpriseId();
        EnterpriseEntity enterprise = enterpriseId == null ? null : enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "所属企业不存在");
        }
        if (!Boolean.TRUE.equals(enterprise.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, "所属企业已停用，不能用于邮箱配置");
        }
        enterpriseMailboxAccessService.assertEnterpriseVisible(principal, enterpriseId);
        if (Boolean.TRUE.equals(request.getAutoReplyEnabled()) && request.getAutoReplyTemplateId() == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "启用自动回复前必须选择自动回复模板");
        }
        if (request.getAssignmentNotifyTemplateId() == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "请选择分配通知模板");
        }
        if (request.getAgentReplyTemplateId() == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "请选择处理人回复模板");
        }
        if (request.getSlaPolicyId() != null
                && (request.getSlaWarningTemplateId() == null || request.getSlaBreachTemplateId() == null)) {
            throw new BusinessException(CODE_BAD_REQUEST, "绑定 SLA 策略时必须同时选择预警和超时模板");
        }
        assertTemplate(request.getAutoReplyTemplateId(), TEMPLATE_AUTO_REPLY, "自动回复模板");
        assertTemplate(request.getAssignmentNotifyTemplateId(), TEMPLATE_ASSIGN_NOTIFY, "分配通知模板");
        assertTemplate(request.getAgentReplyTemplateId(), TEMPLATE_AGENT_REPLY, "处理人回复模板");
        assertTemplate(request.getSlaWarningTemplateId(), TEMPLATE_SLA_WARNING, "SLA 预警模板");
        assertTemplate(request.getSlaBreachTemplateId(), TEMPLATE_SLA_BREACH, "SLA 超时模板");
        assertSameEnterpriseSlaPolicy(request.getSlaPolicyId(), enterpriseId);
        assertSameEnterpriseRuleGroup(request.getAssignmentRuleGroupId(), enterpriseId);
        String fallbackType = normalizeFallbackType(request.getAssignmentFallbackType());
        if (FALLBACK_DEFAULT_ASSIGNEE.equals(fallbackType) && request.getDefaultAssigneeId() == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "规则未命中使用默认处理人时必须选择默认处理人");
        }
    }

    private void assertTemplate(Long templateId, String expectedType, String label) {
        if (templateId == null) {
            return;
        }
        NotificationTemplateEntity template = notificationTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(CODE_BAD_REQUEST, label + "不存在");
        }
        if (!expectedType.equals(normalize(template.getTemplateType()).toUpperCase())) {
            throw new BusinessException(CODE_BAD_REQUEST, label + "类型不匹配");
        }
        if (!Boolean.TRUE.equals(template.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, label + "已停用");
        }
    }

    private void assertSameEnterpriseSlaPolicy(Long policyId, Long enterpriseId) {
        if (policyId == null) {
            return;
        }
        SlaPolicyEntity policy = slaPolicyMapper.selectById(policyId);
        if (policy == null || !enterpriseId.equals(policy.getEnterpriseId())) {
            throw new BusinessException(CODE_BAD_REQUEST, "SLA 策略与邮箱不属于同一企业");
        }
        if (!Boolean.TRUE.equals(policy.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, "SLA 策略已停用");
        }
    }

    private void assertSameEnterpriseRuleGroup(Long groupId, Long enterpriseId) {
        if (groupId == null) {
            return;
        }
        AssignmentRuleGroupEntity group = assignmentRuleGroupMapper.selectById(groupId);
        if (group == null || !enterpriseId.equals(group.getEnterpriseId())) {
            throw new BusinessException(CODE_BAD_REQUEST, "分配规则组与邮箱不属于同一企业");
        }
        if (!Boolean.TRUE.equals(group.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, "分配规则组已停用");
        }
    }

    private void assertDefaultAssigneeCanAccessMailbox(Long assigneeId, Long mailboxId) {
        if (assigneeId != null) {
            enterpriseMailboxAccessService.assertAssigneeCanAccessMailbox(assigneeId, mailboxId);
        }
    }

    private void recordLog(CurrentUserPrincipal principal, String actionCode, Long bizId, String content) {
        OperationLogEntity log = new OperationLogEntity();
        log.setOperator(principal.account());
        log.setModuleCode("MAILBOX");
        log.setActionCode(actionCode);
        log.setBizId(String.valueOf(bizId));
        log.setContent(content);
        log.setCreatedBy(principal.account());
        log.setUpdatedBy(principal.account());
        operationLogMapper.insert(log);
    }

    private MailboxVO toVO(MailboxEntity mailbox) {
        UserEntity assignee = mailbox.getDefaultAssigneeId() == null ? null : userMapper.selectById(mailbox.getDefaultAssigneeId());
        EnterpriseEntity enterprise = mailbox.getEnterpriseId() == null ? null : enterpriseMapper.selectById(mailbox.getEnterpriseId());
        return new MailboxVO(
                mailbox.getId(),
                mailbox.getEnterpriseId(),
                enterprise == null ? null : enterprise.getEnterpriseName(),
                mailbox.getMailboxName(),
                mailbox.getEmailAddress(),
                mailbox.getEnabled(),
                mailbox.getDefaultAssigneeId(),
                assignee == null ? null : assignee.getDisplayName(),
                mailbox.getImapHost(),
                mailbox.getImapPort(),
                mailbox.getImapSslEnabled(),
                mailbox.getImapUsername(),
                mailbox.getImapFolder(),
                mailbox.getFetchIntervalSec(),
                mailbox.getSmtpHost(),
                mailbox.getSmtpPort(),
                mailbox.getSmtpSslEnabled(),
                mailbox.getSmtpUsername(),
                mailbox.getSmtpFromName(),
                mailbox.getAutoReplyEnabled(),
                mailbox.getAutoReplyTemplateId(),
                mailbox.getAssignmentNotifyTemplateId(),
                mailbox.getAgentReplyTemplateId(),
                mailbox.getSlaWarningTemplateId(),
                mailbox.getSlaBreachTemplateId(),
                mailbox.getSlaPolicyId(),
                mailbox.getAssignmentRuleGroupId(),
                mailbox.getAssignmentFallbackType(),
                mailbox.getLastFetchAt(),
                mailbox.getConnectionStatus(),
                mailbox.getCreatedAt(),
                mailbox.getUpdatedAt()
        );
    }

    private boolean shouldTestImap(String testType) {
        return TEST_ALL.equals(testType) || TEST_IMAP.equals(testType);
    }

    private boolean shouldTestSmtp(String testType) {
        return TEST_ALL.equals(testType) || TEST_SMTP.equals(testType);
    }

    private String normalizeTestType(String value) {
        String testType = normalize(value).toUpperCase();
        if (testType.isEmpty()) {
            return TEST_ALL;
        }
        if (!TEST_ALL.equals(testType) && !TEST_IMAP.equals(testType) && !TEST_SMTP.equals(testType)) {
            throw new BusinessException(CODE_BAD_REQUEST, "连接测试类型仅支持 ALL、IMAP 或 SMTP");
        }
        return testType;
    }

    private String normalizeFallbackType(String value) {
        String normalized = normalize(value).toUpperCase();
        if (normalized.isEmpty()) {
            return FALLBACK_NONE;
        }
        if (!FALLBACK_NONE.equals(normalized) && !FALLBACK_DEFAULT_ASSIGNEE.equals(normalized)) {
            throw new BusinessException(CODE_BAD_REQUEST,
                    "规则未命中处理方式仅支持 NONE 或 DEFAULT_ASSIGNEE");
        }
        return normalized;
    }

    private long normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private Integer normalizePort(Integer value, int defaultValue) {
        int port = value == null ? defaultValue : value;
        if (port < 1 || port > 65535) {
            throw new BusinessException(CODE_BAD_REQUEST, "端口范围需为 1-65535");
        }
        return port;
    }

    private Integer normalizeFetchInterval(Integer value) {
        int interval = value == null ? 120 : value;
        if (interval < 60 || interval > 1800) {
            throw new BusinessException(CODE_BAD_REQUEST, "拉取频率需为 60-1800 秒");
        }
        return interval;
    }

    private String normalizeFolder(String value) {
        String folder = normalize(value);
        return folder.isEmpty() ? "INBOX" : folder;
    }

    private String normalizeLower(String value) {
        return normalize(value).toLowerCase();
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeException(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private void closeFolder(Folder folder) {
        if (folder == null || !folder.isOpen()) {
            return;
        }
        try {
            folder.close(false);
        } catch (Exception ignored) {
            // 忽略关闭异常，保留主要连接测试结果。
        }
    }

    private void closeStore(Store store) {
        if (store == null || !store.isConnected()) {
            return;
        }
        try {
            store.close();
        } catch (Exception ignored) {
            // 忽略关闭异常，保留主要连接测试结果。
        }
    }

    private void closeTransport(Transport transport) {
        if (transport == null || !transport.isConnected()) {
            return;
        }
        try {
            transport.close();
        } catch (Exception ignored) {
            // 忽略关闭异常，保留主要连接测试结果。
        }
    }

    private record MailboxConnectionConfig(
            String imapHost,
            Integer imapPort,
            Boolean imapSslEnabled,
            String imapUsername,
            String imapPassword,
            String imapFolder,
            String smtpHost,
            Integer smtpPort,
            Boolean smtpSslEnabled,
            String smtpUsername,
            String smtpPassword
    ) {
    }

    private static TestPartResult ok(String message) {
        return new TestPartResult(true, message);
    }

    private static TestPartResult fail(String message) {
        return new TestPartResult(false, message);
    }

    private static TestPartResult skipped(String message) {
        return new TestPartResult(false, message);
    }

    private record TestPartResult(boolean success, String message) {
    }
}
