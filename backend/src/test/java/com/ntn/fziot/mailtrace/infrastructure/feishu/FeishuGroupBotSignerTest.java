package com.ntn.fziot.mailtrace.infrastructure.feishu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeishuGroupBotSignerTest {

    private final FeishuGroupBotSigner signer = new FeishuGroupBotSigner();

    @Test
    void sign_shouldMatchFeishuHmacSha256Vector() {
        assertEquals("cgtpJzI2j6bUDggbdGYjskCK3FPgKpTkwosfWsMzKqM=",
                signer.sign(1599360473L, "abc"));
    }

    @Test
    void sign_shouldRejectBlankSecret() {
        assertThrows(IllegalArgumentException.class, () -> signer.sign(1599360473L, " "));
    }
}
