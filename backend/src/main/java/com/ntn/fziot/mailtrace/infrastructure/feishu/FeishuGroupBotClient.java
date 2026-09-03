package com.ntn.fziot.mailtrace.infrastructure.feishu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

@Component
public class FeishuGroupBotClient {

    private static final Set<String> ALLOWED_HOSTS = Set.of("open.feishu.cn", "open.larksuite.com");

    private final ObjectMapper objectMapper;
    private final FeishuGroupBotProperties properties;
    private final FeishuGroupBotSigner signer;
    private final HttpClient httpClient;

    public FeishuGroupBotClient(ObjectMapper objectMapper, FeishuGroupBotProperties properties,
                                FeishuGroupBotSigner signer) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.signer = signer;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
    }

    public SendResult send(String webhookUrl, String signingSecret, String cardJson) {
        URI uri;
        try {
            uri = validateWebhook(webhookUrl);
            long timestamp = Instant.now().getEpochSecond();
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("timestamp", String.valueOf(timestamp));
            payload.put("sign", signer.sign(timestamp, signingSecret));
            payload.put("msg_type", "interactive");
            payload.set("card", objectMapper.readTree(cardJson));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(properties.getRequestTimeoutMs()))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 429 || status >= 500) {
                return SendResult.failed(true, "HTTP_" + status, "飞书服务暂时不可用");
            }
            if (status < 200 || status >= 300) {
                return SendResult.failed(false, "HTTP_" + status, "飞书拒绝了群消息请求");
            }
            JsonNode body = objectMapper.readTree(response.body());
            int code = body.path("code").asInt(body.path("StatusCode").asInt(0));
            String message = body.path("msg").asText(body.path("StatusMessage").asText("success"));
            if (code == 0) {
                return SendResult.ok(String.valueOf(code), "success");
            }
            return SendResult.failed(false, String.valueOf(code), sanitize(message));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return SendResult.failed(true, "INTERRUPTED", "飞书群消息发送被中断");
        } catch (IllegalArgumentException exception) {
            return SendResult.failed(false, "CONFIG_ERROR", sanitize(exception.getMessage()));
        } catch (Exception exception) {
            return SendResult.failed(true, "CLIENT_ERROR", "飞书群消息请求失败");
        }
    }

    public URI validateWebhook(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new IllegalArgumentException("飞书群机器人 Webhook 未配置");
        }
        URI uri = URI.create(webhookUrl.trim());
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !ALLOWED_HOSTS.contains(host)
                || uri.getRawUserInfo() != null || uri.getFragment() != null
                || uri.getPath() == null || !uri.getPath().startsWith("/open-apis/bot/v2/hook/")) {
            throw new IllegalArgumentException("飞书群机器人 Webhook 地址格式不正确");
        }
        return uri;
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "飞书群消息发送失败";
        }
        String sanitized = value.replaceAll("https://[^\\s]+", "[REDACTED]");
        return sanitized.length() <= 500 ? sanitized : sanitized.substring(0, 500);
    }

    public record SendResult(boolean success, boolean retryable, String code, String message) {
        public static SendResult ok(String code, String message) {
            return new SendResult(true, false, code, message);
        }

        public static SendResult failed(boolean retryable, String code, String message) {
            return new SendResult(false, retryable, code, message);
        }
    }
}
