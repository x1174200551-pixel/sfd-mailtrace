package com.ntn.fziot.mailtrace.infrastructure.crypto;

import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class MailPasswordCipher {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final String PREFIX = "v1";

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public MailPasswordCipher(@Value("${mailtrace.security.jwt.secret}") String secret) {
        this.secretKey = new SecretKeySpec(sha256(secret), ALGORITHM);
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new BusinessException(CODE_BAD_REQUEST, "请输入邮箱密码或授权码");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return PREFIX + ":" + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception exception) {
            throw new BusinessException(CODE_BAD_REQUEST, "邮箱密码加密失败");
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            throw new BusinessException(CODE_BAD_REQUEST, "邮箱密码未配置");
        }
        try {
            String[] parts = cipherText.split(":");
            if (parts.length != 3 || !PREFIX.equals(parts[0])) {
                throw new IllegalArgumentException("unsupported cipher");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] encrypted = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new BusinessException(CODE_BAD_REQUEST, "邮箱密码解密失败，请重新保存密码或授权码");
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("无法初始化邮箱密码密钥", exception);
        }
    }
}
