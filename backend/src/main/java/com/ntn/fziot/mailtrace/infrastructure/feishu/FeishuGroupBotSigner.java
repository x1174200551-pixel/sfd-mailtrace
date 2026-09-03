package com.ntn.fziot.mailtrace.infrastructure.feishu;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class FeishuGroupBotSigner {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    public String sign(long timestamp, String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("飞书签名密钥不能为空");
        }
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return Base64.getEncoder().encodeToString(mac.doFinal(new byte[0]));
        } catch (Exception exception) {
            throw new IllegalStateException("飞书消息签名生成失败", exception);
        }
    }
}
