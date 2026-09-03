package com.ntn.fziot.mailtrace.infrastructure.feishu;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "mailtrace.feishu.group-bot")
public class FeishuGroupBotProperties {

    private boolean enabled;

    @Min(100)
    @Max(30000)
    private int connectTimeoutMs = 3000;

    @Min(100)
    @Max(60000)
    private int requestTimeoutMs = 5000;

    @Min(1000)
    private long retryFixedDelayMs = 60000;

    @Min(0)
    @Max(20)
    private int maxRetry = 5;

    @NotBlank
    private String ticketBaseUrl = "http://127.0.0.1:5174";
}
