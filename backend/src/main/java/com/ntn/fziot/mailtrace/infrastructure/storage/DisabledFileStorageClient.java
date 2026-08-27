package com.ntn.fziot.mailtrace.infrastructure.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 本地联调可关闭文件存储；仅在真正调用附件能力时明确报错，不阻断应用启动。
 */
@Component
@ConditionalOnProperty(prefix = "mailtrace.storage", name = "enabled", havingValue = "false")
public class DisabledFileStorageClient implements FileStorageClient {

    @Override
    public String upload(String originalFileName, long fileSize, String contentType, InputStream inputStream) {
        throw disabled();
    }

    @Override
    public InputStream download(String objectKey) {
        throw disabled();
    }

    @Override
    public void delete(String objectKey) {
        throw disabled();
    }

    private IllegalStateException disabled() {
        return new IllegalStateException("文件存储已禁用: mailtrace.storage.enabled=false");
    }
}
