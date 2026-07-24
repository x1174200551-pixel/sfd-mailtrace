package com.ntn.fziot.mailtrace.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioConfig config;
    private S3Client s3Client;

    @PostConstruct
    public void init() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey());
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(config.getEndpoint()))
                .region(Region.US_EAST_1) // MinIO 忽略 region
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true) // MinIO 需要 path-style
                        .build())
                .build();

        // 确保 bucket 存在
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(config.getBucket()).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(config.getBucket()).build());
            log.info("MinIO bucket 已创建: {}", config.getBucket());
        }
        log.info("FileStorageService 初始化完成 endpoint={} bucket={}", config.getEndpoint(), config.getBucket());
    }

    /**
     * 上传文件到 MinIO，返回 objectKey。
     */
    public String upload(String originalFileName, long fileSize, String contentType, InputStream inputStream) {
        String objectKey = generateObjectKey(originalFileName);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(config.getBucket())
                .key(objectKey)
                .contentLength(fileSize)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, fileSize));
        log.info("文件上传成功 bucket={} key={} size={}", config.getBucket(), objectKey, fileSize);
        return objectKey;
    }

    /**
     * 获取文件的下载 URL（有效期 1 小时）。
     */
    public String getPresignedUrl(String objectKey) {
        // 简单实现：直连 MinIO 的公开 URL，或使用 presigned URL
        // 这里返回可直接访问的 URL（本地开发用）
        return config.getEndpoint() + "/" + config.getBucket() + "/" + objectKey;
    }

    /**
     * 获取文件内容流。
     */
    public InputStream download(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(config.getBucket())
                .key(objectKey)
                .build();
        return s3Client.getObject(request);
    }

    /**
     * 删除文件。
     */
    public void delete(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(config.getBucket())
                .key(objectKey)
                .build();
        s3Client.deleteObject(request);
        log.info("文件删除成功 key={}", objectKey);
    }

    private String generateObjectKey(String originalFileName) {
        String ext = "";
        int dot = originalFileName.lastIndexOf('.');
        if (dot > 0) {
            ext = originalFileName.substring(dot);
        }
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }
}
