package com.sfonda.mailtrace.infrastructure.mail;

import jakarta.mail.Store;
import org.eclipse.angus.mail.imap.IMAPStore;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * IMAP 连接辅助：网易 163/126 等要求登录后发送 ID，否则 EXAMINE 会报 Unsafe Login。
 */
public final class ImapStoreSupport {

    private ImapStoreSupport() {
    }

    /**
     * 在 connect 成功后发送客户端标识；非 IMAPStore 时忽略。
     */
    public static void identifyClient(Store store) throws Exception {
        if (!(store instanceof IMAPStore imapStore)) {
            return;
        }
        Map<String, String> clientId = new LinkedHashMap<>();
        clientId.put("name", "MailTrace");
        clientId.put("version", "1.0.0");
        clientId.put("vendor", "sfonda");
        clientId.put("support-email", "support@sfonda.local");
        imapStore.id(clientId);
    }
}
