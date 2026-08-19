package com.ntn.fziot.mailtrace.infrastructure.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mailtrace.storage")
public class MinioConfig {
    private boolean enabled = true;
    private String endpoint;
    private String publicEndpoint;
    private String region = "us-east-1";
    private String accessKey;
    private String secretKey;
    private String bucket;
}
