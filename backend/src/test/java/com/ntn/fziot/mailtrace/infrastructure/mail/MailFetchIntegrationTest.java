package com.ntn.fziot.mailtrace.infrastructure.mail;

import com.ntn.fziot.mailtrace.infrastructure.crypto.MailPasswordCipher;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 邮件解析集成测试。
 * 从数据库读取第一个已启用邮箱，连接 IMAP 拉取并解析邮件。
 * <p>
 * 依赖：
 * - MySQL 数据库运行中且已应用 Flyway 迁移
 * - 存在至少一个启用中的邮箱配置
 * - 该邮箱的 IMAP 连接可达
 * <p>
 * 运行方式：
 * mvn test -Dtest="MailFetchIntegrationTest" -DfailIfNoTests=false \
 *   -Dspring.profiles.active=local \
 *   -Dit.mail=true
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "it.mail", matches = "true")
class MailFetchIntegrationTest {

    @Autowired
    private MailboxMapper mailboxMapper;

    @Autowired
    private MailPasswordCipher mailPasswordCipher;

    @Autowired
    private JakartaMailImapFetchClient imapFetchClient;

    private static MailboxEntity mailbox;
    private static String decryptedPassword;

    @BeforeAll
    static void check() {
        // 不加条件检查，Spring 注入失败自然跳过
    }

    @Test
    void testFetchAndParseRealMail() throws Exception {
        // 1、查找第一个已启用的邮箱
        List<MailboxEntity> mailboxes = mailboxMapper.selectList(null);
        MailboxEntity mailbox = mailboxes.stream()
                .filter(m -> Boolean.TRUE.equals(m.getEnabled()))
                .findFirst()
                .orElse(null);
        assertNotNull(mailbox, "没有找到已启用的邮箱配置，请在系统里先配置一个邮箱");
        this.mailbox = mailbox;

        // 2、解密密码
        decryptedPassword = mailPasswordCipher.decrypt(mailbox.getImapPasswordEnc());
        assertNotNull(decryptedPassword, "IMAP 密码解密失败");

        System.out.println("=" .repeat(80));
        System.out.println("邮箱: " + mailbox.getEmailAddress()
                + " | IMAP: " + mailbox.getImapHost() + ":" + mailbox.getImapPort());
        System.out.println("=" .repeat(80));

        // 3、构建 IMAP 配置
        ImapFetchConfig config = new ImapFetchConfig(
                mailbox.getImapHost(),
                mailbox.getImapPort() == null ? 993 : mailbox.getImapPort(),
                mailbox.getImapSslEnabled() == null || mailbox.getImapSslEnabled(),
                mailbox.getImapUsername(),
                decryptedPassword,
                mailbox.getImapFolder() == null || mailbox.getImapFolder().isBlank() ? "INBOX" : mailbox.getImapFolder()
        );

        // 4、拉取并解析
        List<ParsedMail> parsedMails = imapFetchClient.fetchUnseenMessages(config);

        System.out.println("拉取邮件数: " + parsedMails.size());

        if (parsedMails.isEmpty()) {
            System.out.println("没有未读邮件，请先给 " + mailbox.getEmailAddress() + " 发一封测试邮件");
            return;
        }

        // 5、逐封打印解析结果
        for (int i = 0; i < parsedMails.size(); i++) {
            ParsedMail mail = parsedMails.get(i);
            System.out.println("-".repeat(80));
            System.out.println("【第 " + (i + 1) + " 封】");
            System.out.println("  Message-ID: " + mail.messageId());
            System.out.println("  In-Reply-To: " + mail.inReplyTo());
            System.out.println("  References: " + mail.references());
            System.out.println("  发件人: " + mail.fromAddress() + " (" + mail.fromPersonal() + ")");
            System.out.println("  收件人: " + mail.toAddresses());
            System.out.println("  抄送: " + mail.ccAddresses());
            System.out.println("  主题: " + mail.subject());
            System.out.println("  发送时间: " + mail.sentAt());
            System.out.println("  邮件大小: " + mail.size() + " bytes");
            System.out.println("  纯文本正文 (" + (mail.contentText() != null ? mail.contentText().length() : 0) + " chars):");
            if (mail.contentText() != null) {
                System.out.println("    " + mail.contentText().substring(0, Math.min(mail.contentText().length(), 500)));
            }
            System.out.println("  HTML 正文 (" + (mail.contentHtml() != null ? mail.contentHtml().length() : 0) + " chars)");
            System.out.println("  附件数: " + mail.attachments().size());
            for (int j = 0; j < mail.attachments().size(); j++) {
                AttachmentInfo att = mail.attachments().get(j);
                System.out.println("    [" + (j + 1) + "] " + att.fileName()
                        + " (" + att.contentType() + ", " + att.size() + " bytes"
                        + (att.isInline() ? ", 内嵌" : ", 附件") + ")");
            }
        }

        System.out.println("=".repeat(80));
        System.out.println("解析完成，共 " + parsedMails.size() + " 封邮件");

        // 验证关键字段不为空
        assertFalse(parsedMails.isEmpty());
        for (ParsedMail mail : parsedMails) {
            assertNotNull(mail.messageId(), "messageId 不应为空");
            assertNotNull(mail.fromAddress(), "fromAddress 不应为空");
        }
    }
}
