package com.ntn.fziot.mailtrace.infrastructure.storage;

import java.io.InputStream;

public interface FileStorageClient {

    String upload(String originalFileName, long fileSize, String contentType, InputStream inputStream);

    InputStream download(String objectKey);

    void delete(String objectKey);
}
