package com.ntn.fziot.mailtrace.application.bizservice.mailsend;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 邮件线程头值对象。数据库中的 Message-ID 不带尖括号，SMTP 头统一带尖括号。
 */
public record MailThreadHeaders(
        String messageId,
        String inReplyTo,
        String references,
        String replyToAddress
) {

    static final int MAX_REFERENCES_LENGTH = 1000;
    private static final Pattern BRACKETED_MESSAGE_ID = Pattern.compile("<([^<>\\s]+)>");
    private static final Pattern REPLY_PREFIX = Pattern.compile(
            "^(?:(?:re|aw|sv|回复)\\s*[:：]\\s*)+", Pattern.CASE_INSENSITIVE);
    private static final Pattern SAFE_DOMAIN = Pattern.compile("^[A-Za-z0-9.-]+$");

    public static MailThreadHeaders forReply(String parentMessageId, String parentReferences,
                                             String replyToAddress) {
        String normalizedParent = normalizeMessageId(parentMessageId);
        return new MailThreadHeaders(null, normalizedParent,
                buildReferences(parentReferences, normalizedParent), normalizeAddress(replyToAddress));
    }

    public MailThreadHeaders withMessageId(String fixedMessageId) {
        return new MailThreadHeaders(normalizeMessageId(fixedMessageId), inReplyTo, references, replyToAddress);
    }

    public static String normalizeMessageId(String raw) {
        if (raw == null || containsLineBreak(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        Matcher matcher = BRACKETED_MESSAGE_ID.matcher(trimmed);
        if (matcher.find()) {
            return normalizeToken(matcher.group(1));
        }
        if (trimmed.contains(" ") || trimmed.contains("\t") || trimmed.contains("<") || trimmed.contains(">")) {
            return null;
        }
        return normalizeToken(trimmed);
    }

    public static List<String> parseMessageIds(String raw) {
        if (raw == null || raw.isBlank() || containsLineBreak(raw)) {
            return List.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        Matcher matcher = BRACKETED_MESSAGE_ID.matcher(raw);
        while (matcher.find()) {
            String id = normalizeToken(matcher.group(1));
            if (id != null) {
                ids.add(id);
            }
        }
        if (!ids.isEmpty()) {
            return List.copyOf(ids);
        }
        for (String token : raw.trim().split("\\s+")) {
            String id = normalizeMessageId(token);
            if (id != null) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    public static String buildReferences(String parentReferences, String parentMessageId) {
        String parent = normalizeMessageId(parentMessageId);
        if (parent == null) {
            return null;
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>(parseMessageIds(parentReferences));
        ids.remove(parent);
        ids.add(parent);
        List<String> ordered = new ArrayList<>(ids);
        String rendered = renderReferences(ordered);
        while (rendered.length() > MAX_REFERENCES_LENGTH && ordered.size() > 1) {
            ordered.remove(0);
            rendered = renderReferences(ordered);
        }
        return rendered;
    }

    public static String normalizeReferences(String raw) {
        List<String> ordered = new ArrayList<>(parseMessageIds(raw));
        String rendered = renderReferences(ordered);
        while (rendered != null && rendered.length() > MAX_REFERENCES_LENGTH && ordered.size() > 1) {
            ordered.remove(0);
            rendered = renderReferences(ordered);
        }
        return rendered;
    }

    public static String buildReplySubject(String originalSubject) {
        String subject = originalSubject == null ? "" : originalSubject.trim();
        subject = REPLY_PREFIX.matcher(subject).replaceFirst("").trim();
        return "Re: " + (subject.isEmpty() ? "(无主题)" : subject);
    }

    public static String generateMessageId(String fromAddress) {
        String domain = "mailtrace.local";
        if (fromAddress != null && !containsLineBreak(fromAddress)) {
            int at = fromAddress.lastIndexOf('@');
            if (at >= 0 && at < fromAddress.length() - 1) {
                String candidate = fromAddress.substring(at + 1).trim().toLowerCase(Locale.ROOT);
                if (SAFE_DOMAIN.matcher(candidate).matches()) {
                    domain = candidate;
                }
            }
        }
        return UUID.randomUUID().toString().replace("-", "") + "@" + domain;
    }

    public static String toHeaderValue(String messageId) {
        String normalized = normalizeMessageId(messageId);
        return normalized == null ? null : "<" + normalized + ">";
    }

    private static String renderReferences(List<String> ids) {
        return ids.stream().map(MailThreadHeaders::toHeaderValue)
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElse(null);
    }

    private static String normalizeToken(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > 255 || containsLineBreak(normalized)
                || normalized.contains("<") || normalized.contains(">")) {
            return null;
        }
        return normalized;
    }

    private static String normalizeAddress(String value) {
        if (value == null || containsLineBreak(value)) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static boolean containsLineBreak(String value) {
        return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
    }
}
