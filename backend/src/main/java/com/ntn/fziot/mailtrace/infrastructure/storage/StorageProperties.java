package com.ntn.fziot.mailtrace.infrastructure.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mailtrace.storage")
public class StorageProperties {

    private boolean enabled = true;
    private String type = "minio";
    private Minio minio = new Minio();

    /**
     * Legacy flat MinIO properties. Keep them for current local/Nacos compatibility.
     */
    private String endpoint;
    private String publicEndpoint;
    private String region = "us-east-1";
    private String accessKey;
    private String secretKey;
    private String bucket;

    public String getMinioEndpoint() {
        return firstText(minio.getEndpoint(), endpoint);
    }

    public String getMinioPublicEndpoint() {
        return firstText(minio.getPublicEndpoint(), publicEndpoint);
    }

    public String getMinioRegion() {
        return firstText(minio.getRegion(), region, "us-east-1");
    }

    public String getMinioAccessKey() {
        return firstText(minio.getAccessKey(), accessKey);
    }

    public String getMinioSecretKey() {
        return firstText(minio.getSecretKey(), secretKey);
    }

    public String getMinioBucket() {
        return firstText(minio.getBucket(), bucket);
    }

    private static String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @Data
    public static class Minio {
        private String endpoint;
        private String publicEndpoint;
        private String region;
        private String accessKey;
        private String secretKey;
        private String bucket;
    }
}
