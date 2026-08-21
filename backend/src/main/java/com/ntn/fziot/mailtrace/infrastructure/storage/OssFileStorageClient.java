package com.ntn.fziot.mailtrace.infrastructure.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mailtrace.storage", name = "type", havingValue = "oss")
public class OssFileStorageClient implements FileStorageClient {

    private final StorageProperties storageProperties;
    private final AliyunOssProperties ossProperties;
    private OSS ossClient;

    @PostConstruct
    public void init() {
        ensureEnabled();
        String endpoint = requireText(ossProperties.normalizedEndpoint(), "aliyun.oss.endpoint");
        String accessKeyId = requireText(ossProperties.getAccessKeyId(), "aliyun.oss.access-key-id");
        String accessKeySecret = requireText(ossProperties.getAccessKeySecret(), "aliyun.oss.access-key-secret");
        String bucketName = requireText(ossProperties.getBucketName(), "aliyun.oss.bucket-name");

        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        if (!ossClient.doesBucketExist(bucketName)) {
            throw new IllegalStateException("OSS bucket 不存在或无权访问: " + bucketName);
        }
        log.info("文件存储初始化完成 type=oss endpoint={} bucket={} dir={}",
                endpoint, bucketName, ossProperties.getDir());
    }

    @Override
    public String upload(String originalFileName, long fileSize, String contentType, InputStream inputStream) {
        String objectKey = StorageObjectKeys.generate(ossProperties.getDir(), originalFileName);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(fileSize);
        metadata.setContentType(contentType != null ? contentType : "application/octet-stream");
        PutObjectRequest request = new PutObjectRequest(
                ossProperties.getBucketName(),
                objectKey,
                inputStream,
                metadata
        );
        ossClient.putObject(request);
        log.info("文件上传成功 type=oss bucket={} key={} size={}", ossProperties.getBucketName(), objectKey, fileSize);
        return objectKey;
    }

    @Override
    public InputStream download(String objectKey) {
        OSSObject object = ossClient.getObject(new GetObjectRequest(ossProperties.getBucketName(), objectKey));
        return object.getObjectContent();
    }

    @Override
    public void delete(String objectKey) {
        ossClient.deleteObject(ossProperties.getBucketName(), objectKey);
        log.info("文件删除成功 type=oss key={}", objectKey);
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    private void ensureEnabled() {
        if (!storageProperties.isEnabled()) {
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
