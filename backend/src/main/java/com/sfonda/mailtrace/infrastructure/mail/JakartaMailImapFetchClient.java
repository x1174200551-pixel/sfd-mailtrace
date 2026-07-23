package com.sfonda.mailtrace.infrastructure.mail;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.FlagTerm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * 基于 Jakarta Mail 的 IMAP 拉取客户端。
 */
@Slf4j
@Component
public class JakartaMailImapFetchClient implements ImapFetchClient {

    @Override
    public int countUnseenMessages(ImapFetchConfig config) throws Exception {
        long start = System.currentTimeMillis();
        log.info("开始 IMAP 拉取 host={} port={} ssl={} username={} folder={}",
                config.host(), config.port(), config.sslEnabled(), config.username(), config.folder());
        // 1、按 SSL 开关组装 IMAP 会话属性并建立连接
        Store store = null;
        Folder folder = null;
        try {
            String protocol = config.sslEnabled() ? "imaps" : "imap";
            Properties properties = new Properties();
            properties.put("mail." + protocol + ".connectiontimeout", "10000");
            properties.put("mail." + protocol + ".timeout", "10000");
            properties.put("mail." + protocol + ".writetimeout", "10000");
            Session session = Session.getInstance(properties);
            store = session.getStore(protocol);
            store.connect(config.host(), config.port(), config.username(), config.password());
            log.info("IMAP 服务器连接成功 host={} port={} username={} 耗时={}ms",
                    config.host(), config.port(), config.username(), System.currentTimeMillis() - start);
            // 网易邮箱要求登录后发送 ID，否则打开文件夹会报 Unsafe Login
            ImapStoreSupport.identifyClient(store);

            // 2、打开目标收件文件夹，不存在则直接失败
            folder = store.getFolder(config.folder());
            if (folder == null || !folder.exists()) {
                throw new IllegalStateException("IMAP 收件文件夹不存在：" + config.folder());
            }
            folder.open(Folder.READ_ONLY);
            log.info("IMAP 文件夹打开成功 host={} folder={} 耗时={}ms",
                    config.host(), config.folder(), System.currentTimeMillis() - start);

            // 3、统计未读邮件数，作为本轮拉取结果（解析建单留给后续任务）
            Message[] unseen = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            int count = unseen == null ? 0 : unseen.length;
            long elapsed = System.currentTimeMillis() - start;
            log.info("IMAP 拉取完成 host={} folder={} username={} 未读数={} 总耗时={}ms",
                    config.host(), config.folder(), config.username(), count, elapsed);
            return count;
        } finally {
            // 4、关闭文件夹与 Store，避免连接泄漏
            if (folder != null && folder.isOpen()) {
                try {
                    folder.close(false);
                } catch (Exception ignored) {
                    log.warn("关闭 IMAP 文件夹异常 host={} folder={}", config.host(), config.folder(), ignored);
                }
            }
            if (store != null && store.isConnected()) {
                try {
                    store.close();
                } catch (Exception ignored) {
                    log.warn("关闭 IMAP Store 异常 host={}", config.host(), ignored);
                }
            }
        }
    }
}
