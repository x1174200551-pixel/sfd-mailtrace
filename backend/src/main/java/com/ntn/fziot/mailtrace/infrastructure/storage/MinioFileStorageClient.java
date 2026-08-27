package com.ntn.fziot.mailtrace.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${mailtrace.storage.enabled:true}' == 'true' && '${mailtrace.storage.type:oss}' == 'minio'")
public class MinioFileStorageClient implements FileStorageClient {

    private final StorageProperties properties;
    private S3Client s3Client;

    @PostConstruct
    public void init() {
        ensureEnabled();
        String endpoint = requireText(properties.getMinioEndpoint(), "mailtrace.storage.minio.endpoint");
        String accessKey = requireText(properties.getMinioAccessKey(), "mailtrace.storage.minio.access-key");
        String secretKey = requireText(properties.getMinioSecretKey(), "mailtrace.storage.minio.secret-key");
        String bucket = requireText(properties.getMinioBucket(), "mailtrace.storage.minio.bucket");

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(properties.getMinioRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();

        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("MinIO bucket 已创建: {}", bucket);
        }
        log.info("文件存储初始化完成 type=minio endpoint={} bucket={}", endpoint, bucket);
    }

    @Override
    public String upload(String originalFileName, long fileSize, String contentType, InputStream inputStream) {
        String objectKey = StorageObjectKeys.generate(originalFileName);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getMinioBucket())
                .key(objectKey)
                .contentLength(fileSize)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, fileSize));
        log.info("文件上传成功 type=minio bucket={} key={} size={}", properties.getMinioBucket(), objectKey, fileSize);
        return objectKey;
    }

    @Override
    public InputStream download(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.getMinioBucket())
                .key(objectKey)
                .build();
        return s3Client.getObject(request);
    }

    @Override
    public void delete(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.getMinioBucket())
                .key(objectKey)
                .build();
        s3Client.deleteObject(request);
        log.info("文件删除成功 type=minio key={}", objectKey);
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("文件存储已禁用: mailtrace.storage.enabled=false");
        }
    }

    private static String requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少文件存储配置: " + propertyName);
        }
        return value;
    }
}
