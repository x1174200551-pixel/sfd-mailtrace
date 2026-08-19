package com.ntn.fziot.mailtrace.infrastructure.mail;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class IncomingMailParserTest {

    private static final Session SESSION = Session.getInstance(new Properties());

    private static MimeMessage newMessage() throws Exception {
        MimeMessage msg = new MimeMessage(SESSION);
        msg.setSentDate(new Date());
        return msg;
    }

    /** 在 saveChanges 之后设置 Message-ID，避免被自动生成覆盖 */
    private static void setId(MimeMessage msg, String id) throws Exception {
        msg.setHeader("Message-ID", id);
    }

    @Test
    void parse_plainText_success() throws Exception {
        MimeMessage msg = newMessage();
        msg.setFrom(new InternetAddress("sender@test.com", "张三"));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress("to@test.com"));
        msg.setSubject("测试邮件主题");
        msg.setText("这是一封纯文本测试邮件。", "UTF-8");
        msg.setHeader("In-Reply-To", "<parent-001@test.com>");
        msg.saveChanges();
        setId(msg, "<test-001@test.com>");

        ParsedMail result = IncomingMailParser.parse(msg);

        assertEquals("<test-001@test.com>", result.messageId());
        assertEquals("<parent-001@test.com>", result.inReplyTo());
        assertEquals("sender@test.com", result.fromAddress());
        assertEquals("张三", result.fromPersonal());
        assertEquals(List.of("to@test.com"), result.toAddresses());
        assertEquals("测试邮件主题", result.subject());
        assertEquals("这是一封纯文本测试邮件。", result.contentText());
        assertNull(result.contentHtml());
        assertTrue(result.attachments().isEmpty());
        assertNotNull(result.sentAt());
    }

    @Test
    void parse_htmlOnly_shouldKeepHtml() throws Exception {
        MimeMessage msg = newMessage();
        msg.setFrom(new InternetAddress("html@test.com"));
        msg.setSubject("HTML Only");
        msg.setContent("<html><body><p>Hello <b>World</b></p></body></html>", "text/html; charset=UTF-8");
        msg.saveChanges();
        setId(msg, "<html-001@test.com>");

        ParsedMail result = IncomingMailParser.parse(msg);

        assertEquals("Hello World", result.contentText());
        assertEquals("<html><body><p>Hello <b>World</b></p></body></html>", result.contentHtml());
        assertNotNull(result.rawHeaders());
        assertTrue(result.rawHeaders().contains("Message-ID: <html-001@test.com>"));
        assertNotNull(result.rawEml());
        assertTrue(result.rawEml().length > result.contentHtml().length());
    }

    @Test
    void parse_multipartAlternative_shouldExtractPlainAndHtml() throws Exception {
        MimeMessage msg = newMessage();
        msg.setFrom(new InternetAddress("multi@test.com"));
        msg.setSubject("Multipart Test");
        MimeMultipart multipart = new MimeMultipart("alternative");
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("纯文本版本", "UTF-8");
        multipart.addBodyPart(textPart);
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent("<html><body><p>HTML版本</p></body></html>", "text/html; charset=UTF-8");
        multipart.addBodyPart(htmlPart);
        msg.setContent(multipart);
        msg.saveChanges();
        setId(msg, "<multi-001@test.com>");

        ParsedMail result = IncomingMailParser.parse(msg);

        assertEquals("纯文本版本", result.contentText());
        assertEquals("<html><body><p>HTML版本</p></body></html>", result.contentHtml());
    }

    @Test
    void parse_withAttachment_shouldExtractAttachment() throws Exception {
        MimeMessage msg = newMessage();
        msg.setFrom(new InternetAddress("attach@test.com"));
        msg.setSubject("With Attachment");

        MimeMultipart multipart = new MimeMultipart("mixed");
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("请看附件", "UTF-8");
        multipart.addBodyPart(textPart);

        MimeBodyPart attachPart = new MimeBodyPart();
        attachPart.setText("fake-pdf-content");
        attachPart.setFileName("报告.pdf");
        attachPart.setDisposition(MimeBodyPart.ATTACHMENT);
        multipart.addBodyPart(attachPart);

        MimeBodyPart imgPart = new MimeBodyPart();
        imgPart.setText("fake-image-content");
        imgPart.setFileName("logo.png");
        imgPart.setDisposition(MimeBodyPart.INLINE);
        imgPart.setHeader("Content-ID", "<logo@test.com>");
        multipart.addBodyPart(imgPart);

        msg.setContent(multipart);
        msg.saveChanges();
        setId(msg, "<attach-001@test.com>");

        ParsedMail result = IncomingMailParser.parse(msg);

        assertEquals("请看附件", result.contentText());
        assertEquals(2, result.attachments().size());

        AttachmentInfo pdf = result.attachments().get(0);
        assertEquals("报告.pdf", pdf.fileName());
        assertFalse(pdf.isInline());

        AttachmentInfo img = result.attachments().get(1);
        assertEquals("logo.png", img.fileName());
        assertTrue(img.isInline());
        assertEquals("logo@test.com", img.contentId());
    }

    @Test
    void parse_noMessageId_shouldGenerateFallback() throws Exception {
        MimeMessage msg = newMessage();
        msg.setFrom(new InternetAddress("noid@test.com"));
        msg.setSubject("No MsgID");
        msg.setText("No Message-ID header");

        ParsedMail result = IncomingMailParser.parse(msg);

        assertNotNull(result.messageId());
        assertTrue(result.messageId().contains("@generated.mailtrace.local")
                || result.messageId().contains("@mailtrace.local"));
    }

    @Test
    void parse_gbkEncoding_shouldNotGarbled() throws Exception {
        MimeMessage msg = newMessage();
        msg.setFrom(new InternetAddress("gbk@test.com"));
        msg.setSubject("中文GBK编码测试", "GBK");
        msg.setText("中文GBK编码测试", "GBK");
        msg.saveChanges();
        setId(msg, "<gbk-001@test.com>");

        ParsedMail result = IncomingMailParser.parse(msg);

        assertEquals("中文GBK编码测试", result.subject());
        assertEquals("中文GBK编码测试", result.contentText());
    }

    @Test
    void parse_withCc_shouldExtractCc() throws Exception {
        MimeMessage msg = newMessage();
        msg.setFrom(new InternetAddress("from@test.com"));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress("to@test.com"));
        msg.setRecipient(Message.RecipientType.CC, new InternetAddress("cc1@test.com"));
        msg.addRecipient(Message.RecipientType.CC, new InternetAddress("cc2@test.com"));
        msg.setRecipient(Message.RecipientType.BCC, new InternetAddress("bcc@test.com"));
        msg.setSubject("CC Test");
        msg.setText("Test");
        msg.saveChanges();
        setId(msg, "<cc-001@test.com>");

        ParsedMail result = IncomingMailParser.parse(msg);

        assertEquals(List.of("cc1@test.com", "cc2@test.com"), result.ccAddresses());
        assertEquals(List.of("bcc@test.com"), result.bccAddresses());
    }

    @Test
    void parse_stripHtml_shouldRemoveTags() {
        assertEquals("Hello World", IncomingMailParser.stripHtml("<html><body><p>Hello <b>World</b></p></body></html>"));
        assertEquals("a < b", IncomingMailParser.stripHtml("a &lt; b"));
        assertEquals("", IncomingMailParser.stripHtml(null));
        assertEquals("", IncomingMailParser.stripHtml(""));
    }

    @Test
    void parse_withReferences_shouldExtract() throws Exception {
        MimeMessage msg = newMessage();
        msg.setFrom(new InternetAddress("ref@test.com"));
        msg.setSubject("References Test");
        msg.setText("Test");
        msg.setHeader("References", "<parent-001@test.com> <parent-002@test.com>");
        msg.saveChanges();
        setId(msg, "<ref-001@test.com>");

        ParsedMail result = IncomingMailParser.parse(msg);

        assertEquals("<parent-001@test.com> <parent-002@test.com>", result.references());
    }

    @Test
    void parse_multipartMixedOnlyPlainText() throws Exception {
        MimeMessage msg = newMessage();
        msg.setFrom(new InternetAddress("mixed@test.com"));
        msg.setSubject("Mixed Simple");

        MimeMultipart multipart = new MimeMultipart("mixed");
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Only text in mixed", "UTF-8");
        multipart.addBodyPart(textPart);
        msg.setContent(multipart);
        msg.saveChanges();
        setId(msg, "<mixed-001@test.com>");

        ParsedMail result = IncomingMailParser.parse(msg);

        assertEquals("Only text in mixed", result.contentText());
        assertNull(result.contentHtml());
        assertTrue(result.attachments().isEmpty());
    }
}
