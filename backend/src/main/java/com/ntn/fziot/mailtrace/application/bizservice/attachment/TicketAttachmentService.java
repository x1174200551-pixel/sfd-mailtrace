package com.ntn.fziot.mailtrace.application.bizservice.attachment;

import com.ntn.fziot.mailtrace.infrastructure.storage.FileStorageService;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketAttachmentVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketAttachmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketAttachmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketAttachmentService {

    private final TicketAttachmentMapper attachmentMapper;
    private final FileStorageService fileStorageService;

    /**
     * 上传附件。
     */
    @Transactional
    public TicketAttachmentVO upload(Long ticketId, Long messageId, MultipartFile file, String operator) throws IOException {
        // 1、上传到 MinIO
        String objectKey = fileStorageService.upload(
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType(),
                file.getInputStream()
        );

        // 2、保存记录
        TicketAttachmentEntity entity = new TicketAttachmentEntity();
        entity.setTicketId(ticketId);
        entity.setMessageId(messageId);
        entity.setFileName(file.getOriginalFilename());
        entity.setFileSize(file.getSize());
        entity.setContentType(file.getContentType());
        entity.setObjectKey(objectKey);
        entity.setUploadedBy(operator);
        entity.setCreatedAt(LocalDateTime.now());
        attachmentMapper.insert(entity);

        log.info("附件上传成功 ticketId={} fileName={} size={}", ticketId, file.getOriginalFilename(), file.getSize());

        return toVO(entity);
    }

    /**
     * 查询工单的所有附件。
     */
    public List<TicketAttachmentVO> listByTicketId(Long ticketId) {
        return attachmentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TicketAttachmentEntity>()
                        .eq(TicketAttachmentEntity::getTicketId, ticketId)
                        .orderByAsc(TicketAttachmentEntity::getCreatedAt)
        ).stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 删除附件。
     */
    @Transactional
    public void delete(Long attachmentId) {
        TicketAttachmentEntity entity = attachmentMapper.selectById(attachmentId);
        if (entity == null) return;
        fileStorageService.delete(entity.getObjectKey());
        attachmentMapper.deleteById(attachmentId);
        log.info("附件删除成功 id={} fileName={}", attachmentId, entity.getFileName());
    }

    /**
     * 获取附件的原始输入流（用于下载代理）。
     */
    public InputStream downloadRaw(Long attachmentId) {
        TicketAttachmentEntity entity = attachmentMapper.selectById(attachmentId);
        if (entity == null) return null;
        return fileStorageService.download(entity.getObjectKey());
    }

    private TicketAttachmentVO toVO(TicketAttachmentEntity e) {
        return new TicketAttachmentVO(
                e.getId(),
                e.getMessageId(),
                e.getFileName(),
                e.getFileSize(),
                e.getContentType(),
                fileStorageService.getPresignedUrl(e.getObjectKey()),
                e.getUploadedBy(),
                e.getCreatedAt()
        );
    }
}
