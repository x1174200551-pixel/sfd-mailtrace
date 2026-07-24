package com.ntn.fziot.mailtrace.infrastructure.mail;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;

/**
 * IMAP 拉取邮件的解析器。
 * 输入 Jakarta Mail Message，输出 ParsedMail。
 * <p>
 * 处理策略：
 * - 嵌套 multipart 递归解析，最大深度 10
 * - 编码：MimeUtility + Content-Type charset，UTF-8/GBK 兼容
 * - 附件：Content-Disposition 优先，无 disposition 但有文件名也提取
 * - 超大附件（>5MB）只记元数据，不读内容
 * - 内嵌图片（Content-Disposition=inline + Content-ID）标记 isInline
 * - message/rfc822 转发邮件不递归解析内层
 * - 空 Message-ID 用 SHA1(from + subject + sentAt) 替代
 * - 日期解析失败返回 null
 */
@Slf4j
public final class IncomingMailParser {

    private static final int MAX_MULTIPART_DEPTH = 10;
    private static final long MAX_ATTACHMENT_SIZE = 5L * 1024 * 1024;
    private static final int MAX_READ_SIZE = 10 * 1024 * 1024; // 10MB read limit

    private IncomingMailParser() {
    }

    /**
     * 解析一封邮件。
     */
    public static ParsedMail parse(Message msg) {
        long start = System.currentTimeMillis();

        try {
            // 1、头部
            String messageId = readMessageId(msg);
            String inReplyTo = readHeader(msg, "In-Reply-To");
            String references = readHeader(msg, "References");

            // 2、地址
            String fromAddress = null;
            String fromPersonal = null;
            Address[] froms = msg.getFrom();
            if (froms != null && froms.length > 0 && froms[0] instanceof InternetAddress ia) {
                fromAddress = ia.getAddress();
                fromPersonal = ia.getPersonal();
                if (fromPersonal != null) {
                    fromPersonal = MimeUtility.decodeText(fromPersonal);
                }
            }
            List<String> toAddresses = parseAddresses(msg.getRecipients(Message.RecipientType.TO));
            List<String> ccAddresses = parseAddresses(msg.getRecipients(Message.RecipientType.CC));

            // 3、主题
            String subject = MimeUtility.decodeText(msg.getSubject());
            if (subject == null) subject = "";

            // 4、时间
            LocalDateTime sentAt = parseSentDate(msg);
            LocalDateTime receivedAt = LocalDateTime.now();

            // 5、正文 + 附件
            ContentResult content = new ContentResult();
            List<AttachmentInfo> attachments = new ArrayList<>();
            if (msg instanceof MimeMessage mimeMsg) {
                parsePart(mimeMsg, content, attachments, 0);
            }

            // 6、降级：有 text/html 但无 text/plain 时，用空字符串
            String contentText = content.text;
            String contentHtml = content.html;
            if (contentText == null && contentHtml != null) {
                contentText = stripHtml(contentHtml);
            }

            // 7、大小
            long size = Math.max(0, msg.getSize());

            log.debug("邮件解析完成 messageId={} from={} subject={} size={} attachments={} 耗时={}ms",
                    messageId, fromAddress, truncateSubject(subject), size, attachments.size(),
                    System.currentTimeMillis() - start);

            return new ParsedMail(
                    messageId, inReplyTo, references,
                    fromAddress, fromPersonal,
                    toAddresses, ccAddresses,
                    subject, contentText, contentHtml,
                    sentAt, receivedAt,
                    attachments, size
            );
        } catch (Exception exception) {
            log.warn("邮件解析异常 messageId={} error={}", readHeaderSafe(msg, "Message-ID"), exception.getMessage());
            // 异常时返回最小信息，不影响建单
            return new ParsedMail(
                    null, null, null,
                    null, null,
                    List.of(), List.of(),
                    "", null, null,
                    null, LocalDateTime.now(),
                    List.of(), 0
            );
        }
    }

    // ==================== 正文+附件递归解析 ====================

    private static void parsePart(Part part, ContentResult content, List<AttachmentInfo> attachments, int depth)
            throws MessagingException, IOException {
        if (depth > MAX_MULTIPART_DEPTH) {
            log.warn("multipart 递归超限 depth={} contentType={}", depth, part.getContentType());
            return;
        }

        // 正文（排除附件和内嵌图片）
        boolean isAttachmentOrInline = Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())
                || Part.INLINE.equalsIgnoreCase(part.getDisposition());

        if (part.isMimeType("text/plain") && !isAttachmentOrInline) {
            String text = readTextContent(part);
            if (text != null && content.text == null) {
                content.text = text;
            }
            return;
        }

        if (part.isMimeType("text/html") && !isAttachmentOrInline) {
            String html = readTextContent(part);
            if (html != null && content.html == null) {
                content.html = html;
            }
            return;
        }

        // multipart：递归每个 body part
        if (part.isMimeType("multipart/*")) {
            Object contentObj = part.getContent();
            if (contentObj instanceof Multipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    parsePart(multipart.getBodyPart(i), content, attachments, depth + 1);
                }
            }
            return;
        }

        // message/rfc822：转发邮件，当前不递归，只记附件名
        if (part.isMimeType("message/rfc822")) {
            String fileName = part.getFileName();
            if (fileName == null) fileName = "forwarded-message.eml";
            attachments.add(new AttachmentInfo(
                    MimeUtility.decodeText(fileName),
                    "message/rfc822",
                    Math.max(0, part.getSize()),
                    false, null, null
            ));
            return;
        }

        // 附件判断
        if (isAttachment(part)) {
            extractAttachment(part, attachments);
        }
    }

    // ==================== 附件处理 ====================

    private static boolean isAttachment(Part part) throws MessagingException {
        String disposition = part.getDisposition();
        String fileName = part.getFileName();

        // 有 disposition 的按 disposition 判断
        if (disposition != null) {
            return Part.ATTACHMENT.equalsIgnoreCase(disposition)
                    || Part.INLINE.equalsIgnoreCase(disposition);
        }

        // 没有 disposition 但有文件名，且不是正文类型 → 也视为附件
        if (fileName != null && !fileName.isBlank()) {
            try {
                String decodedFileName = MimeUtility.decodeText(fileName);
                return !decodedFileName.isEmpty();
            } catch (Exception e) {
                return true;
            }
        }

        return false;
    }

    private static void extractAttachment(Part part, List<AttachmentInfo> attachments)
            throws MessagingException, IOException {
        try {
            String fileName = MimeUtility.decodeText(part.getFileName());
            if (fileName == null || fileName.isBlank()) {
                fileName = "unnamed";
            }

            String contentType = part.getContentType();
            if (contentType != null && contentType.contains(";")) {
                contentType = contentType.substring(0, contentType.indexOf(';')).trim();
            }

            // getSize() 可能返回 -1（未保存的消息或流式内容），此时从流读取并计算
            long size = Math.max(0, part.getSize());
            boolean isInline = Part.INLINE.equalsIgnoreCase(part.getDisposition());
            String contentId = readContentId(part);

            // 超限附件只记元数据
            byte[] content = null;
            if (size <= MAX_ATTACHMENT_SIZE && size >= 0) {
                content = readAllBytes(part.getInputStream());
                // 如果 size 为 -1（或 0），用实际读取的大小
                if (size == 0 && content != null) {
                    size = content.length;
                }
            } else if (size < 0) {
                // size 为 -1 时尝试从流读取部分内容来判断大小
                InputStream is = part.getInputStream();
                if (is != null) {
                    byte[] buf = new byte[8192];
                    int total = 0;
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        total += len;
                        if (total > MAX_ATTACHMENT_SIZE + 1) {
                            // 超限，放弃读取
                            size = total;
                            content = null;
                            is.close();
                            attachments.add(new AttachmentInfo(fileName, contentType, total, isInline, null, contentId));
                            log.debug("超大附件（流式检测） name={} type={} size={}", fileName, contentType, total);
                            return;
                        }
                    }
                    size = total;
                    // 未超限，重新读取一次
                    is = part.getInputStream();
                    if (is != null) {
                        content = readAllBytes(is);
                    }
                }
            }

            attachments.add(new AttachmentInfo(fileName, contentType, size, isInline, content, contentId));
            log.debug("提取附件 name={} type={} size={} inline={} contentId={}",
                    fileName, contentType, size, isInline, contentId);
        } catch (Exception exception) {
            log.warn("附件提取失败 fileName={} error={}", part.getFileName(), exception.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    private static String readMessageId(Message msg) throws MessagingException {
        String messageId = readHeader(msg, "Message-ID");
        if (messageId != null && !messageId.isBlank()) {
            return messageId;
        }
        // 没有 Message-ID 时，用 SHA1(from + subject + sentAt) 生成
        Address[] froms = msg.getFrom();
        String from = froms != null && froms.length > 0 ? froms[0].toString() : "";
        String subject = msg.getSubject() != null ? msg.getSubject() : "";
        String sentAt = msg.getSentDate() != null ? String.valueOf(msg.getSentDate().getTime()) : "";
        String raw = from + "|" + subject + "|" + sentAt;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(hash);
            return "<sha1-" + hex + "@generated.mailtrace.local>";
        } catch (NoSuchAlgorithmException e) {
            return "<generated-" + System.currentTimeMillis() + "@mailtrace.local>";
        }
    }

    private static String readHeader(Message msg, String name) throws MessagingException {
        String[] values = msg.getHeader(name);
        if (values == null || values.length == 0) return null;
        return values[0].trim();
    }

    private static String readHeaderSafe(Message msg, String name) {
        try {
            return readHeader(msg, name);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> parseAddresses(Address[] addresses) {
        if (addresses == null || addresses.length == 0) return List.of();
        List<String> result = new ArrayList<>();
        for (Address addr : addresses) {
            if (addr instanceof InternetAddress ia) {
                result.add(ia.getAddress());
            } else {
                result.add(addr.toString());
            }
        }
        return result;
    }

    private static String readTextContent(Part part) throws MessagingException, IOException {
        Object content = part.getContent();
        if (content instanceof String text) {
            return text;
        }
        // 非 String 内容（如 InputStream）也尝试读取
        if (content instanceof InputStream is) {
            byte[] bytes = readAllBytes(is);
            // 从 Content-Type 取 charset，默认 UTF-8
            String contentType = part.getContentType();
            String charset = extractCharset(contentType);
            return new String(bytes, java.nio.charset.Charset.forName(charset));
        }
        return null;
    }

    private static String extractCharset(String contentType) {
        if (contentType == null) return "UTF-8";
        String lower = contentType.toLowerCase();
        int charsetIdx = lower.indexOf("charset=");
        if (charsetIdx < 0) return "UTF-8";
        String charset = contentType.substring(charsetIdx + 8).trim();
        if (charset.startsWith("\"") && charset.endsWith("\"")) {
            charset = charset.substring(1, charset.length() - 1);
        }
        int semi = charset.indexOf(';');
        if (semi > 0) charset = charset.substring(0, semi).trim();
        return charset.isBlank() ? "UTF-8" : charset;
    }

    private static String readContentId(Part part) throws MessagingException {
        String[] values = part.getHeader("Content-ID");
        if (values == null || values.length == 0) return null;
        String cid = values[0].trim();
        if (cid.startsWith("<") && cid.endsWith(">")) {
            cid = cid.substring(1, cid.length() - 1);
        }
        return cid;
    }

    private static LocalDateTime parseSentDate(Message msg) {
        try {
            Date date = msg.getSentDate();
            if (date == null) return null;
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * HTML → 纯文本：将块级标签转成换行，去掉其余标签，保留段落结构。
     */
    static String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        return html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</div>", "\n")
                .replaceAll("(?i)</li>", "\n")
                .replaceAll("(?i)</(h\\d|blockquote|tr|th)>", "\n")
                .replaceAll("(?s)<[^>]*>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n[ \\t]+", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int totalRead = 0;
        int len;
        while ((len = is.read(buf)) != -1) {
            totalRead += len;
            if (totalRead > MAX_READ_SIZE) {
                log.warn("附件读取超限 {} bytes，截断", MAX_READ_SIZE);
                break;
            }
            buffer.write(buf, 0, len);
        }
        return buffer.toByteArray();
    }

    private static String truncateSubject(String subject) {
        if (subject == null) return "";
        return subject.length() > 80 ? subject.substring(0, 80) + "..." : subject;
    }

    /**
     * 递归解析过程中的中间结果。
     */
    private static class ContentResult {
        String text;
        String html;
    }
}
