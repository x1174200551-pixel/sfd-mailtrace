package com.ntn.fziot.mailtrace.infrastructure.mail;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单封 IMAP 拉取邮件的解析结果。
 * 纯数据模型，不含业务逻辑，供后续建单、关联、附件存储使用。
 *
 * @param messageId     Message-ID 头（用于去重与线程关联）
 * @param inReplyTo     In-Reply-To 头
 * @param references    References 头
 * @param fromAddress   发件人邮箱
 * @param fromPersonal  发件人显示名
 * @param toAddresses   收件人列表
 * @param ccAddresses   抄送列表
 * @param bccAddresses  密送列表
 * @param subject       主题（已解码）
 * @param contentText   纯文本正文
 * @param contentHtml   HTML 正文
 * @param rawHeaders    原始邮件头
 * @param rawEml        原始 EML 内容
 * @param sentAt        邮件原始发送时间
 * @param receivedAt    IMAP 拉取时间
 * @param attachments   附件列表（超限附件 content 为 null）
 * @param size          邮件大小（字节）
 */
public record ParsedMail(
        String messageId,
        String inReplyTo,
        String references,
        String fromAddress,
        String fromPersonal,
        List<String> toAddresses,
        List<String> ccAddresses,
        List<String> bccAddresses,
        String subject,
        String contentText,
        String contentHtml,
        String rawHeaders,
        byte[] rawEml,
        LocalDateTime sentAt,
        LocalDateTime receivedAt,
        List<AttachmentInfo> attachments,
        long size
) {
}
