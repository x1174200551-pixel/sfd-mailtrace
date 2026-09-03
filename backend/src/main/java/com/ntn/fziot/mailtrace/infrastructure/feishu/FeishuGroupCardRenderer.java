package com.ntn.fziot.mailtrace.infrastructure.feishu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class FeishuGroupCardRenderer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "([A-Za-z0-9._%+-]{1,64})@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_CONTENT_LENGTH = 1200;

    private final ObjectMapper objectMapper;
    private final FeishuGroupBotProperties properties;

    public RenderedCard render(String sendType, Long ticketId, String ticketNo, String priority,
                               String title, String content, String assigneeName) {
        String safeTitle = truncate(toPlainText(title), MAX_TITLE_LENGTH);
        String safeContent = truncate(maskEmails(toPlainText(content)), MAX_CONTENT_LENGTH);

        ObjectNode card = objectMapper.createObjectNode();
        ObjectNode config = card.putObject("config");
        config.put("wide_screen_mode", true);
        ObjectNode header = card.putObject("header");
        header.put("template", headerTemplate(sendType));
        header.putObject("title").put("tag", "plain_text").put("content", safeTitle);

        ArrayNode elements = card.putArray("elements");
        elements.addObject().put("tag", "div").putObject("text")
                .put("tag", "lark_md")
                .put("content", cardBody(assigneeName, ticketNo, priority, safeContent));
        if (ticketId != null) {
            ObjectNode action = elements.addObject();
            action.put("tag", "action");
            ObjectNode button = action.putArray("actions").addObject();
            button.put("tag", "button");
            button.put("type", "primary");
            button.put("url", ticketUrl(ticketId));
            button.putObject("text").put("tag", "plain_text").put("content", "查看工单");
        }
        try {
            return new RenderedCard(safeTitle, safeContent, objectMapper.writeValueAsString(card));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("飞书群卡片生成失败", exception);
        }
    }

    private String cardBody(String assigneeName, String ticketNo, String priority, String content) {
        StringBuilder body = new StringBuilder();
        appendField(body, "处理人", toPlainText(assigneeName));
        appendField(body, "工单", toPlainText(ticketNo));
        appendField(body, "优先级", safe(priority).isBlank() ? "" : priorityLabel(priority));
        if (!content.isBlank()) {
            if (!body.isEmpty()) {
                body.append("\n\n");
            }
            body.append(content);
        }
        return body.toString();
    }

    private void appendField(StringBuilder body, String label, String value) {
        if (value.isBlank()) {
            return;
        }
        if (!body.isEmpty()) {
            body.append('\n');
        }
        body.append("**").append(label).append("：**").append(value);
    }

    private String priorityLabel(String priority) {
        return switch (safe(priority).toUpperCase()) {
            case "URGENT" -> "紧急";
            case "HIGH" -> "高";
            case "LOW" -> "低";
            default -> "普通";
        };
    }

    private String headerTemplate(String sendType) {
        return switch (safe(sendType)) {
            case "SLA_WARNING", "SLA_RESPONSE_WARNING", "SLA_RESOLVE_WARNING" -> "orange";
            case "SLA_BREACH", "SLA_ESCALATION", "SLA_RESPONSE_BREACH", "SLA_RESPONSE_ESCALATION",
                    "SLA_RESOLVE_BREACH", "SLA_RESOLVE_ESCALATION" -> "red";
            case "TEST" -> "green";
            default -> "blue";
        };
    }

    private String ticketUrl(Long ticketId) {
        String baseUrl = properties.getTicketBaseUrl().trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/tickets/" + ticketId;
    }

    private String toPlainText(String value) {
        return safe(value)
                .replaceAll("(?is)<at\\b[^>]*>.*?</at>", "")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?is)<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String maskEmails(String value) {
        Matcher matcher = EMAIL_PATTERN.matcher(value);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String local = matcher.group(1);
            String maskedLocal = local.length() <= 2
                    ? local.charAt(0) + "*"
                    : local.charAt(0) + "*".repeat(Math.min(local.length() - 2, 6)) + local.charAt(local.length() - 1);
            matcher.appendReplacement(output, Matcher.quoteReplacement(maskedLocal + "@" + matcher.group(2)));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record RenderedCard(String title, String content, String cardJson) {
    }
}
