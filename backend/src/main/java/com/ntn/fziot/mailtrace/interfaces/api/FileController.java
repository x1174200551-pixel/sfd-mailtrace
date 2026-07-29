package com.ntn.fziot.mailtrace.interfaces.api;

import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.RequirePermission;
import com.ntn.fziot.mailtrace.infrastructure.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传文件（通用，不关联工单）")
    @PostMapping("/upload")
    @RequirePermission(value = "ticket_attachment:upload", message = "无权上传附件")
    public BasicResult<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String objectKey = fileStorageService.upload(
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType(),
                file.getInputStream()
        );
        return BasicResult.ok(Map.of(
                "objectKey", objectKey,
                "fileName", file.getOriginalFilename(),
                "fileSize", file.getSize(),
                "contentType", file.getContentType() != null ? file.getContentType() : "application/octet-stream"
        ));
    }
}
