package com.ntn.fziot.mailtrace.infrastructure.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtTokenService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expiresInSeconds;

    public JwtTokenService(
            ObjectMapper objectMapper,
            @Value("${mailtrace.security.jwt.secret:mailtrace-dev-secret-change-me}") String secret,
            @Value("${mailtrace.security.jwt.expires-in-seconds:7200}") long expiresInSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expiresInSeconds = expiresInSeconds;
    }

    public String createToken(UserEntity user) {
        long now = Instant.now().getEpochSecond();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(user.getId()));
        payload.put("id", user.getId());
        payload.put("account", user.getAccount());
        payload.put("displayName", user.getDisplayName());
        payload.put("email", user.getEmail());
        payload.put("roleCode", user.getRoleCode());
        payload.put("iat", now);
        payload.put("exp", now + expiresInSeconds);

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String signingInput = encodedHeader + "." + encodedPayload;
        return signingInput + "." + sign(signingInput);
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public Optional<CurrentUserPrincipal> parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }

            String signingInput = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(signingInput), parts[2])) {
                return Optional.empty();
            }

            Map<String, Object> payload = objectMapper.readValue(decode(parts[1]), MAP_TYPE);
            Number exp = (Number) payload.get("exp");
            if (exp == null || exp.longValue() <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }

            Number id = (Number) payload.get("id");
            if (id == null) {
                return Optional.empty();
            }

            return Optional.of(new CurrentUserPrincipal(
                    id.longValue(),
                    stringValue(payload.get("account")),
                    stringValue(payload.get("displayName")),
                    stringValue(payload.get("email")),
                    stringValue(payload.get("roleCode"))
            ));
        } catch (RuntimeException | java.io.IOException e) {
            return Optional.empty();
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return encode(objectMapper.writeValueAsBytes(value));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("JWT 序列化失败", e);
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return encode(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 签名失败", e);
        }
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
