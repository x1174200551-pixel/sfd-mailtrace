package com.sfonda.mailtrace.infrastructure.mail;

/**
 * IMAP 拉取客户端，负责连接邮箱并统计待处理邮件数。
 * 邮件解析与建单由后续任务负责。
 */
public interface ImapFetchClient {

    /**
     * 连接 IMAP 并统计未读邮件数量。
     *
     * @param config IMAP 连接配置
     * @return 未读邮件数
     */
    int countUnseenMessages(ImapFetchConfig config) throws Exception;
}
