package com.ntn.fziot.mailtrace.application.bizservice.attachment;

import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.DataScopeService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.infrastructure.storage.FileStorageService;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketAttachmentEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketAttachmentMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketAttachmentServiceTest {

    @Mock
    private TicketAttachmentMapper attachmentMapper;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private PermissionService permissionService;
    @Spy
    private DataScopeService dataScopeService = new DataScopeService();

    @InjectMocks
    private TicketAttachmentService attachmentService;

    private final CurrentUserPrincipal agent = new CurrentUserPrincipal(
            2L, "agent", "处理人", "agent@example.com", "AGENT");

    @BeforeEach
    void setUp() {
        allowAdminAndAgentOperationalPermissions();
    }

    @Test
    void upload_whenAgentOwnsTicket_shouldSaveOperatorFromPrincipal() throws Exception {
        TicketEntity ticket = ticket(100L, 2L);
        when(ticketMapper.selectById(100L)).thenReturn(ticket);
        when(fileStorageService.upload(eq("test.txt"), eq(5L), eq("text/plain"), any()))
                .thenReturn("attachments/test.txt");
        when(fileStorageService.getPresignedUrl("attachments/test.txt")).thenReturn("http://file/test.txt");
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "hello".getBytes());

        attachmentService.upload(100L, null, file, agent);

        ArgumentCaptor<TicketAttachmentEntity> attachmentCaptor = ArgumentCaptor.forClass(TicketAttachmentEntity.class);
        verify(attachmentMapper).insert(attachmentCaptor.capture());
        assertEquals(100L, attachmentCaptor.getValue().getTicketId());
        assertEquals("agent", attachmentCaptor.getValue().getUploadedBy());
    }

    @Test
    void delete_whenAgentDoesNotOwnTicket_shouldRejectBeforeDeletingStorage() {
        when(ticketMapper.selectById(101L)).thenReturn(ticket(101L, 3L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> attachmentService.delete(101L, 201L, agent));

        assertTrue(ex.getMessage().contains("无权操作"));
        verify(fileStorageService, never()).delete(any());
    }

    @Test
    void downloadRaw_whenAttachmentDoesNotBelongToTicket_shouldReject() {
        when(ticketMapper.selectById(102L)).thenReturn(ticket(102L, 2L));
        TicketAttachmentEntity attachment = new TicketAttachmentEntity();
        attachment.setId(202L);
        attachment.setTicketId(999L);
        attachment.setObjectKey("attachments/other.txt");
        when(attachmentMapper.selectById(202L)).thenReturn(attachment);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> attachmentService.downloadRaw(102L, 202L, agent));

        assertTrue(ex.getMessage().contains("附件不存在"));
        verify(fileStorageService, never()).download(any());
    }

    @Test
    void downloadRaw_whenAgentCanViewTicket_shouldDownloadObject() {
        when(ticketMapper.selectById(103L)).thenReturn(ticket(103L, null));
        TicketAttachmentEntity attachment = new TicketAttachmentEntity();
        attachment.setId(203L);
        attachment.setTicketId(103L);
        attachment.setObjectKey("attachments/open.txt");
        when(attachmentMapper.selectById(203L)).thenReturn(attachment);
        when(fileStorageService.download("attachments/open.txt")).thenReturn(new ByteArrayInputStream(new byte[0]));

        attachmentService.downloadRaw(103L, 203L, agent);

        verify(fileStorageService).download("attachments/open.txt");
    }

    private TicketEntity ticket(Long id, Long assigneeId) {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(id);
        ticket.setAssigneeId(assigneeId);
        return ticket;
    }

    private void allowAdminAndAgentOperationalPermissions() {
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            CurrentUserPrincipal principal = invocation.getArgument(0);
            String permissionCode = invocation.getArgument(1);
            String message = invocation.getArgument(2);
            if (principal == null) {
                throw new BusinessException(40302, "未登录");
            }
            if ("ADMIN".equals(principal.roleCode()) || isAgentOperationalPermission(principal, permissionCode)) {
                return null;
            }
            throw new BusinessException(40302, message);
        }).when(permissionService).assertPermission(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private boolean isAgentOperationalPermission(CurrentUserPrincipal principal, String permissionCode) {
        return "AGENT".equals(principal.roleCode())
                && (permissionCode.startsWith("ticket:")
                || permissionCode.startsWith("ticket_attachment:")
                || "customer:read".equals(permissionCode)
                || "dashboard:read".equals(permissionCode));
    }
}
