package com.ntn.fziot.mailtrace.application.bizservice.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendService;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketReplyTemplateVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.CustomerEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.NotificationTemplateEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.CustomerMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.NotificationTemplateMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理人回复模板的唯一解析与渲染入口。预览和实际发送必须共用该服务。
 */
@Service
@RequiredArgsConstructor
public class AgentReplyTemplateService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final String TEMPLATE_TYPE = "AGENT_REPLY";
    private static final String SOURCE_SELECTED = "SELECTED";
    private static final String SOURCE_MAILBOX_DEFAULT = "MAILBOX_DEFAULT";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[a-zA-Z0-9_]+}");
    private static final Pattern PARAGRAPH_OPEN_PATTERN = Pattern.compile("<p(?:\\s[^>]*)?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern STYLE_ATTRIBUTE_PATTERN = Pattern.compile(
            "\\sstyle\\s*=\\s*([\"'])(.*?)\\1", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern BLOCK_END_PATTERN = Pattern.compile(
            "</(?:p|div|ul|ol|blockquote|h[1-6]|table)>\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPLY_FOLLOWING_BREAK_PATTERN = Pattern.compile(
            "(\\{reply_content})(\\s*<br\\s*/?>)", Pattern.CASE_INSENSITIVE);

    private final NotificationTemplateMapper notificationTemplateMapper;
    private final CustomerMapper customerMapper;
    private final UserMapper userMapper;
    private final CustomerTicketAccessService customerTicketAccessService;

    public List<TicketReplyTemplateVO> listAvailableTemplates(MailboxEntity mailbox) {
        Long defaultTemplateId = mailbox == null ? null : mailbox.getAgentReplyTemplateId();
        return notificationTemplateMapper.selectList(
                        new LambdaQueryWrapper<NotificationTemplateEntity>()
                                .eq(NotificationTemplateEntity::getTemplateType, TEMPLATE_TYPE)
                                .eq(NotificationTemplateEntity::getEnabled, true)
                                .orderByAsc(NotificationTemplateEntity::getTemplateName)
                                .orderByAsc(NotificationTemplateEntity::getId))
                .stream()
                .map(template -> new TicketReplyTemplateVO(
                        template.getId(),
                        template.getTemplateName(),
                        template.getId().equals(defaultTemplateId)))
                .toList();
    }

    public RenderedReply render(TicketEntity ticket, MailboxEntity mailbox, Long selectedTemplateId,
                                String replyText, String replyHtml, String subject) {
        NotificationTemplateEntity template = resolveTemplate(mailbox, selectedTemplateId);
        Map<String, String> textVariables = buildVariables(ticket, mailbox, replyText);
        String contentText = renderTemplate(template.getContentTpl(), textVariables);

        boolean htmlReply = replyHtml != null && !replyHtml.isBlank();
        boolean htmlTemplate = looksLikeHtml(template.getContentTpl());
        String contentHtml = null;
        String contentType = MailSendService.CONTENT_TYPE_TEXT;
        if (htmlReply || htmlTemplate) {
            Map<String, String> htmlVariables = escapeVariables(textVariables);
            String renderedReplyHtml = htmlReply ? normalizeReplyHtmlBlock(replyHtml) : toHtml(replyText);
            htmlVariables.put("{reply_content}", renderedReplyHtml);
            String htmlTemplateContent = htmlTemplate
                    ? template.getContentTpl()
                    : toHtml(template.getContentTpl());
            htmlTemplateContent = normalizeReplyFollowingSpacing(htmlTemplateContent, renderedReplyHtml);
            contentHtml = renderTemplate(htmlTemplateContent, htmlVariables);
            contentType = MailSendService.CONTENT_TYPE_HTML;
        }

        return new RenderedReply(
                template.getId(),
                template.getTemplateName(),
                selectedTemplateId == null ? SOURCE_MAILBOX_DEFAULT : SOURCE_SELECTED,
                TEMPLATE_TYPE,
                subject,
                contentText,
                contentHtml,
                contentType);
    }

    private NotificationTemplateEntity resolveTemplate(MailboxEntity mailbox, Long selectedTemplateId) {
        Long templateId = selectedTemplateId != null
                ? selectedTemplateId
                : mailbox == null ? null : mailbox.getAgentReplyTemplateId();
        if (templateId == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "邮箱未配置可用的处理人回复模板");
        }
        NotificationTemplateEntity template = notificationTemplateMapper.selectById(templateId);
        String label = selectedTemplateId == null ? "邮箱默认处理人回复模板" : "所选处理人回复模板";
        if (template == null) {
            throw new BusinessException(CODE_BAD_REQUEST, label + "不存在，请重新选择");
        }
        if (!Boolean.TRUE.equals(template.getEnabled())) {
            throw new BusinessException(CODE_BAD_REQUEST, label + "已停用，请重新选择");
        }
        if (!TEMPLATE_TYPE.equals(normalize(template.getTemplateType()).toUpperCase())) {
            throw new BusinessException(CODE_BAD_REQUEST, label + "类型不匹配，请重新选择");
        }
        if (!normalize(template.getContentTpl()).contains("{reply_content}")) {
            throw new BusinessException(CODE_BAD_REQUEST, label + "未包含回复内容变量，请联系管理员维护");
        }
        return template;
    }

    private Map<String, String> buildVariables(TicketEntity ticket, MailboxEntity mailbox, String replyText) {
        CustomerEntity customer = ticket.getCustomerId() == null ? null : customerMapper.selectById(ticket.getCustomerId());
        UserEntity assignee = ticket.getAssigneeId() == null ? null : userMapper.selectById(ticket.getAssigneeId());
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("{ticket_no}", safe(ticket.getTicketNo()));
        variables.put("{subject}", safe(ticket.getSubject()));
        variables.put("{customer_email}", safe(ticket.getCustomerEmail()));
        variables.put("{customer_name}", customer == null ? "" : safe(customer.getDisplayName()));
        variables.put("{assignee_name}", assignee == null ? "" : safe(assignee.getDisplayName()));
        variables.put("{mailbox_email}", resolveFromAddress(mailbox));
        variables.put("{ticket_link}", "请登录系统查看工单详情");
        variables.put("{customer_ticket_url}", customerTicketAccessService.buildTicketUrl(ticket));
        variables.put("{customer_ticket_expires_at}",
                customerTicketAccessService.formatExpiresAt(ticket.getCustomerAccessExpiresAt()));
        variables.put("{reply_content}", safe(replyText));
        return variables;
    }

    private Map<String, String> escapeVariables(Map<String, String> variables) {
        Map<String, String> escaped = new LinkedHashMap<>();
        variables.forEach((key, value) -> escaped.put(key, HtmlUtils.htmlEscape(safe(value))));
        return escaped;
    }

    private String renderTemplate(String template, Map<String, String> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(normalize(template));
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String replacement = variables.getOrDefault(matcher.group(), matcher.group());
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private String resolveFromAddress(MailboxEntity mailbox) {
        if (mailbox == null) {
            return "";
        }
        if (mailbox.getSmtpUsername() != null && !mailbox.getSmtpUsername().isBlank()) {
            return mailbox.getSmtpUsername();
        }
        return safe(mailbox.getEmailAddress());
    }

    private boolean looksLikeHtml(String value) {
        String content = normalize(value);
        return content.stripLeading().startsWith("<") || content.contains("</") || content.contains("<br");
    }

    private String toHtml(String value) {
        return HtmlUtils.htmlEscape(safe(value))
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "<br/>");
    }

    /**
     * 富文本编辑器会用带浏览器默认外边距的 p 标签包裹正文。模板在 reply_content
     * 前后已经保留空行，因此只清除首尾段落的外边距，避免预览、会话和实际邮件出现重复空行。
     */
    private String normalizeReplyHtmlBlock(String value) {
        String html = normalize(value);
        Matcher matcher = PARAGRAPH_OPEN_PATTERN.matcher(html);
        int firstStart = -1;
        int firstEnd = -1;
        int lastStart = -1;
        int lastEnd = -1;
        while (matcher.find()) {
            if (firstStart < 0) {
                firstStart = matcher.start();
                firstEnd = matcher.end();
            }
            lastStart = matcher.start();
            lastEnd = matcher.end();
        }
        if (firstStart < 0) {
            return html;
        }

        if (lastStart != firstStart) {
            html = replaceRange(html, lastStart, lastEnd,
                    withZeroMargin(html.substring(lastStart, lastEnd)));
        }
        return replaceRange(html, firstStart, firstEnd,
                withZeroMargin(html.substring(firstStart, firstEnd)));
    }

    /**
     * 块级正文结束标签本身已经换行。模板在 reply_content 后的第一个换行只负责
     * 从正文块退出，不应再生成空白行；移除它后，剩余换行数才与模板编辑器一致。
     */
    private String normalizeReplyFollowingSpacing(String htmlTemplate, String replyHtml) {
        if (!BLOCK_END_PATTERN.matcher(replyHtml).find()) {
            return htmlTemplate;
        }
        return REPLY_FOLLOWING_BREAK_PATTERN.matcher(htmlTemplate).replaceAll("$1");
    }

    private String withZeroMargin(String paragraphOpenTag) {
        Matcher styleMatcher = STYLE_ATTRIBUTE_PATTERN.matcher(paragraphOpenTag);
        if (styleMatcher.find()) {
            String currentStyle = styleMatcher.group(2);
            String nextStyle = currentStyle.isBlank() ? "margin:0" : currentStyle + ";margin:0";
            return replaceRange(paragraphOpenTag, styleMatcher.start(2), styleMatcher.end(2), nextStyle);
        }
        return paragraphOpenTag.substring(0, paragraphOpenTag.length() - 1) + " style=\"margin:0\">";
    }

    private String replaceRange(String value, int start, int end, String replacement) {
        return value.substring(0, start) + replacement + value.substring(end);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record RenderedReply(
            Long templateId,
            String templateName,
            String templateSource,
            String templateType,
            String subject,
            String contentText,
            String contentHtml,
            String contentType
    ) {
        public String sendContent() {
            return contentHtml == null || contentHtml.isBlank() ? contentText : contentHtml;
        }
    }
}
