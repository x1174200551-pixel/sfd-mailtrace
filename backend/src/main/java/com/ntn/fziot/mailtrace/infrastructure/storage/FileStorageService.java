package com.ntn.fziot.mailtrace.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageClient storageClient;

    public String upload(String originalFileName, long fileSize, String contentType, InputStream inputStream) {
        return storageClient.upload(originalFileName, fileSize, contentType, inputStream);
    }

    public InputStream download(String objectKey) {
        return storageClient.download(objectKey);
    }

    public void delete(String objectKey) {
        storageClient.delete(objectKey);
    }
}
