package com.ntn.fziot.mailtrace.application.bizservice.attachment;

import com.ntn.fziot.mailtrace.infrastructure.storage.FileStorageService;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.DataScopeService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketAttachmentVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketAttachmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketAttachmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
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

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_NOT_FOUND = 40401;

    private final TicketAttachmentMapper attachmentMapper;
    private final TicketMapper ticketMapper;
    private final FileStorageService fileStorageService;
    private final DataScopeService dataScopeService;
    private final PermissionService permissionService;

    /**
     * 上传附件。
     */
    @Transactional
    public TicketAttachmentVO upload(Long ticketId, Long messageId, MultipartFile file, CurrentUserPrincipal principal) throws IOException {
        TicketEntity ticket = requireTicket(ticketId);
        permissionService.assertPermission(principal, "ticket_attachment:upload", "无权上传工单附件");
        dataScopeService.assertTicketOperable(principal, ticket);

        // 1、上传到当前配置的文件存储
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
        entity.setIsInline(false);
        entity.setUploadedBy(principal.account());
        entity.setCreatedAt(LocalDateTime.now());
        attachmentMapper.insert(entity);

        log.info("附件上传成功 ticketId={} fileName={} size={}", ticketId, file.getOriginalFilename(), file.getSize());

        return toVO(entity);
    }

    /**
     * 查询工单的所有附件。
     */
    public List<TicketAttachmentVO> listByTicketId(Long ticketId, CurrentUserPrincipal principal) {
        TicketEntity ticket = requireTicket(ticketId);
        permissionService.assertPermission(principal, "ticket_attachment:read", "无权查看工单附件");
        dataScopeService.assertTicketVisible(principal, ticket);
        return attachmentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TicketAttachmentEntity>()
                        .eq(TicketAttachmentEntity::getTicketId, ticketId)
                        .and(wrapper -> wrapper.eq(TicketAttachmentEntity::getIsInline, false)
                                .or()
                                .isNull(TicketAttachmentEntity::getIsInline))
                        .orderByAsc(TicketAttachmentEntity::getCreatedAt)
        ).stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 删除附件。
     */
    @Transactional
    public void delete(Long ticketId, Long attachmentId, CurrentUserPrincipal principal) {
        TicketEntity ticket = requireTicket(ticketId);
        permissionService.assertPermission(principal, "ticket_attachment:delete", "无权删除工单附件");
        dataScopeService.assertTicketOperable(principal, ticket);
        TicketAttachmentEntity entity = requireAttachment(ticketId, attachmentId);
        fileStorageService.delete(entity.getObjectKey());
        attachmentMapper.deleteById(attachmentId);
        log.info("附件删除成功 id={} fileName={}", attachmentId, entity.getFileName());
    }

    /**
     * 获取附件的原始输入流（用于下载代理）。
     */
    public InputStream downloadRaw(Long ticketId, Long attachmentId, CurrentUserPrincipal principal) {
        return download(ticketId, attachmentId, principal).inputStream();
    }

    public AttachmentDownload download(Long ticketId, Long attachmentId, CurrentUserPrincipal principal) {
        TicketEntity ticket = requireTicket(ticketId);
        dataScopeService.assertTicketVisible(principal, ticket);
        TicketAttachmentEntity entity = requireAttachment(ticketId, attachmentId);
        if (Boolean.TRUE.equals(entity.getIsInline())) {
            permissionService.assertPermission(principal, "ticket:read", "无权查看工单");
        } else {
            permissionService.assertPermission(principal, "ticket_attachment:download", "无权下载工单附件");
        }
        return new AttachmentDownload(toVO(entity), fileStorageService.download(entity.getObjectKey()));
    }

    private TicketEntity requireTicket(Long ticketId) {
        if (ticketId == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "工单ID不能为空");
        }
        TicketEntity ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException(CODE_NOT_FOUND, "工单不存在");
        }
        return ticket;
    }

    private TicketAttachmentEntity requireAttachment(Long ticketId, Long attachmentId) {
        if (attachmentId == null) {
            throw new BusinessException(CODE_BAD_REQUEST, "附件ID不能为空");
        }
        TicketAttachmentEntity entity = attachmentMapper.selectById(attachmentId);
        if (entity == null || !ticketId.equals(entity.getTicketId())) {
            throw new BusinessException(CODE_NOT_FOUND, "附件不存在");
        }
        return entity;
    }

    private TicketAttachmentVO toVO(TicketAttachmentEntity e) {
        return new TicketAttachmentVO(
                e.getId(),
                e.getMessageId(),
                e.getFileName(),
                e.getFileSize(),
                e.getContentType(),
                buildDownloadUrl(e),
                e.getIsInline(),
                e.getContentId(),
                e.getUploadedBy(),
                e.getCreatedAt()
        );
    }

    private String buildDownloadUrl(TicketAttachmentEntity entity) {
        return "/api/v1/tickets/" + entity.getTicketId() + "/attachments/" + entity.getId() + "/download";
    }

    public record AttachmentDownload(TicketAttachmentVO attachment, InputStream inputStream) {
    }
}
