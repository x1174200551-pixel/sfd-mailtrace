package com.ntn.fziot.mailtrace.infrastructure.mail;

/**
 * IMAP 拉取客户端配置。
 */
public record ImapFetchConfig(
        String host,
        int port,
        boolean sslEnabled,
        String username,
        String password,
        String folder
) {
}
