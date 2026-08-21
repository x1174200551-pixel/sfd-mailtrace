package com.ntn.fziot.mailtrace.infrastructure.storage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

final class StorageObjectKeys {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private StorageObjectKeys() {
    }

    static String generate(String originalFileName) {
        return generate(null, originalFileName);
    }

    static String generate(String prefix, String originalFileName) {
        String ext = "";
        if (originalFileName != null) {
            int dot = originalFileName.lastIndexOf('.');
            if (dot > 0) {
                ext = originalFileName.substring(dot);
            }
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String objectKey = datePath + "/" + fileName;
        String normalizedPrefix = normalizePrefix(prefix);
        return normalizedPrefix.isEmpty() ? objectKey : normalizedPrefix + "/" + objectKey;
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String value = prefix.trim();
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
