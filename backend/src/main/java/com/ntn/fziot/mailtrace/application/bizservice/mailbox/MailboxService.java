package com.ntn.fziot.mailtrace.application.bizservice.mailbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.crypto.MailPasswordCipher;
import com.ntn.fziot.mailtrace.infrastructure.mail.ImapStoreSupport;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxConnectionTestRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxConnectionTestResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxEnabledRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxSummaryVO;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.OperationLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import jakarta.mail.Folder;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Properties;

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

    private final MailboxMapper mailboxMapper;
    private final UserMapper userMapper;
    private final OperationLogMapper operationLogMapper;
    private final MailPasswordCipher mailPasswordCipher;
    private final PermissionService permissionService;

    /**
     * 分页查询邮箱配置列表。
     */
    public MailboxPageResponse pageMailboxes(CurrentUserPrincipal principal, String keyword, String status,
                                             Boolean enabled, Integer page, Integer size) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "mailbox:read", "无权查看邮箱配置");
        // 2、规范化分页参数并按关键字、启用状态、连接状态构建查询条件
        long currentPage = normalizePage(page);
        long pageSize = normalizeSize(size);
        LambdaQueryWrapper<MailboxEntity> wrapper = buildQuery(keyword, status, enabled)
                .orderByDesc(MailboxEntity::getUpdatedAt)
                .orderByDesc(MailboxEntity::getId);
        // 3、执行分页查询并转换成页面可用 VO
        Page<MailboxEntity> result = mailboxMapper.selectPage(Page.of(currentPage, pageSize), wrapper);
        // 4、汇总邮箱统计摘要并返回列表响应
        return new MailboxPageResponse(
                result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getPages(),
                buildSummary()
        );
    }

    /**
     * 新建邮箱配置，密码或授权码加密后入库。
     */
    @Transactional
    public MailboxVO createMailbox(CurrentUserPrincipal principal, MailboxSaveRequest request) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "mailbox:create", "无权新建邮箱配置");
        // 2、校验邮箱地址唯一性和默认处理人合法性
        String emailAddress = normalizeLower(request.getEmailAddress());
        ensureEmailUnique(emailAddress, null);
        assertAssigneeExists(request.getDefaultAssigneeId());
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
        // 5、写入操作日志并返回新建后的邮箱详情
        recordLog(principal, "CREATE", mailbox.getId(), "新建邮箱配置：" + mailbox.getEmailAddress());
        return toVO(mailboxMapper.selectById(mailbox.getId()));
    }

    /**
     * 编辑邮箱配置，密码为空时沿用原密文。
     */
    @Transactional
    public MailboxVO updateMailbox(CurrentUserPrincipal principal, Long id, MailboxSaveRequest request) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "mailbox:update", "无权编辑邮箱配置");
        // 2、查询目标邮箱并校验邮箱地址唯一性
        MailboxEntity existing = requireMailbox(id);
        String emailAddress = normalizeLower(request.getEmailAddress());
        ensureEmailUnique(emailAddress, id);
        assertAssigneeExists(request.getDefaultAssigneeId());
        // 3、更新基础信息、IMAP/SMTP 配置；未填写密码时保留原密文
        MailboxEntity next = new MailboxEntity();
        next.setId(id);
        next.setImapPasswordEnc(existing.getImapPasswordEnc());
        next.setSmtpPasswordEnc(existing.getSmtpPasswordEnc());
        fillMailbox(next, request, principal.account(), false);
        next.setEmailAddress(emailAddress);
        mailboxMapper.updateById(next);
        // 4、写入操作日志并返回最新邮箱详情
        recordLog(principal, "UPDATE", id, "编辑邮箱配置：" + emailAddress);
        return toVO(mailboxMapper.selectById(id));
    }

    /**
     * 启用或停用邮箱配置。
     */
    @Transactional
    public MailboxVO updateEnabled(CurrentUserPrincipal principal, Long id, MailboxEnabledRequest request) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "mailbox:enable", "无权启停邮箱配置");
        // 2、查询目标邮箱是否存在
        MailboxEntity existing = requireMailbox(id);
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
    public void deleteMailbox(CurrentUserPrincipal principal, Long id) {
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "mailbox:delete", "无权删除邮箱配置");
        // 2、查询目标邮箱是否存在
        MailboxEntity existing = requireMailbox(id);
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
        // 1、校验当前用户具备管理员权限
        permissionService.assertPermission(principal, "mailbox:test_connection", "无权测试邮箱连接");
        // 2、读取已保存邮箱并解密 IMAP/SMTP 密码
        MailboxEntity mailbox = requireMailbox(id);
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
        // 1、校验当前用户具备管理员权限
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

    private LambdaQueryWrapper<MailboxEntity> buildQuery(String keyword, String status, Boolean enabled) {
        String normalizedKeyword = normalize(keyword);
        String normalizedStatus = normalize(status).toUpperCase();
        LambdaQueryWrapper<MailboxEntity> wrapper = new LambdaQueryWrapper<>();
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

    private MailboxSummaryVO buildSummary() {
        long total = mailboxMapper.selectCount(new LambdaQueryWrapper<>());
        long enabled = mailboxMapper.selectCount(new LambdaQueryWrapper<MailboxEntity>().eq(MailboxEntity::getEnabled, true));
        long ok = mailboxMapper.selectCount(new LambdaQueryWrapper<MailboxEntity>().eq(MailboxEntity::getConnectionStatus, STATUS_OK));
        long error = mailboxMapper.selectCount(new LambdaQueryWrapper<MailboxEntity>().eq(MailboxEntity::getConnectionStatus, STATUS_ERROR));
        long unknown = mailboxMapper.selectCount(new LambdaQueryWrapper<MailboxEntity>().eq(MailboxEntity::getConnectionStatus, STATUS_UNKNOWN));
        return new MailboxSummaryVO(total, enabled, total - enabled, ok, error, unknown);
    }

    private void fillMailbox(MailboxEntity mailbox, MailboxSaveRequest request, String operator, boolean create) {
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

    private void assertAssigneeExists(Long assigneeId) {
        if (assigneeId == null) {
            return;
        }
        UserEntity user = userMapper.selectById(assigneeId);
        if (user == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "默认处理人不存在");
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
        return new MailboxVO(
                mailbox.getId(),
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
