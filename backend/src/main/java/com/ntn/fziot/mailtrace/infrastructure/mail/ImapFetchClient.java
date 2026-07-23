package com.ntn.fziot.mailtrace.infrastructure.mail;

import java.util.List;

/**
 * IMAP 拉取客户端，负责连接邮箱并统计待处理邮件数和拉取邮件内容。
 */
public interface ImapFetchClient {

    /**
     * 连接 IMAP 并统计未读邮件数量。
     *
     * @param config IMAP 连接配置
     * @return 未读邮件数
     */
    int countUnseenMessages(ImapFetchConfig config) throws Exception;

    /**
     * 拉取所有未读邮件的完整内容并解析。
     *
     * @param config IMAP 连接配置
     * @return 解析后的邮件列表
     */
    List<ParsedMail> fetchUnseenMessages(ImapFetchConfig config) throws Exception;
}
