package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserDataGrantEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.UserEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserDataGrantMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnterpriseMailboxAccessServiceTest {

    @Mock
    private PermissionService permissionService;
    @Mock
    private UserDataGrantMapper userDataGrantMapper;
    @Mock
    private EnterpriseMapper enterpriseMapper;
    @Mock
    private MailboxMapper mailboxMapper;
    @Mock
    private UserMapper userMapper;

    private EnterpriseMailboxAccessService service;

    private final CurrentUserPrincipal admin = principal(1L, "ADMIN");
    private final CurrentUserPrincipal agent = principal(2L, "AGENT");

    @BeforeEach
    void setUp() {
        service = new EnterpriseMailboxAccessService(
                permissionService,
                userDataGrantMapper,
                enterpriseMapper,
                mailboxMapper,
                userMapper
        );
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void admin_shouldReadAllMailboxes_butOperateOnlyEnabledMailboxInEnabledEnterprise() {
        mockRoles(1L, Set.of("ADMIN"));
        when(enterpriseMapper.selectList(any())).thenReturn(List.of(
                enterprise(100L, true),
                enterprise(200L, false)
        ));
        when(mailboxMapper.selectList(any())).thenReturn(List.of(
                mailbox(10L, 100L, true),
                mailbox(11L, 100L, false),
                mailbox(12L, 200L, true)
        ));

        assertTrue(service.isAdmin(admin));
        assertEquals(Set.of(100L, 200L), service.resolveVisibleEnterpriseIds(admin));
        assertEquals(Set.of(10L, 11L, 12L), service.resolveReadableMailboxIds(admin));
        assertEquals(Set.of(10L), service.resolveOperationalMailboxIds(admin));
    }

    @Test
    void enterpriseAndMailboxGrant_shouldResolveUnion_andKeepDisabledMailboxReadable() {
        mockRoles(2L, Set.of("AGENT"));
        when(userDataGrantMapper.selectList(any())).thenReturn(List.of(
                enterpriseGrant(1L, 2L, 100L),
                mailboxGrant(2L, 2L, 12L)
        ));
        when(mailboxMapper.selectList(any())).thenReturn(List.of(
                mailbox(10L, 100L, true),
                mailbox(11L, 100L, false),
                mailbox(12L, 200L, true)
        ));
        when(enterpriseMapper.selectBatchIds(any())).thenReturn(List.of(
                enterprise(100L, true),
                enterprise(200L, true)
        ));

        assertFalse(service.isAdmin(agent));
        assertEquals(Set.of(100L, 200L), service.resolveVisibleEnterpriseIds(agent));
        assertEquals(Set.of(10L, 11L, 12L), service.resolveReadableMailboxIds(agent));
        assertEquals(Set.of(10L, 12L), service.resolveOperationalMailboxIds(agent));
        assertDoesNotThrow(() -> service.assertMailboxReadable(agent, 11L));
        assertThrows(BusinessException.class, () -> service.assertMailboxOperational(agent, 11L));
    }

    @Test
    void disabledEnterprise_shouldKeepHistoryReadable_butBlockOperations() {
        mockRoles(2L, Set.of("AGENT"));
        when(userDataGrantMapper.selectList(any())).thenReturn(List.of(enterpriseGrant(1L, 2L, 200L)));
        when(mailboxMapper.selectList(any())).thenReturn(List.of(mailbox(12L, 200L, true)));
        when(enterpriseMapper.selectBatchIds(any())).thenReturn(List.of(enterprise(200L, false)));

        assertEquals(Set.of(12L), service.resolveReadableMailboxIds(agent));
        assertEquals(Set.of(), service.resolveOperationalMailboxIds(agent));
    }

    @Test
    void emptyGrant_shouldReturnEmptyScope_withoutQueryingBusinessTables() {
        mockRoles(2L, Set.of("AGENT"));
        when(userDataGrantMapper.selectList(any())).thenReturn(List.of());

        assertEquals(Set.of(), service.resolveVisibleEnterpriseIds(agent));
        assertEquals(Set.of(), service.resolveReadableMailboxIds(agent));
        assertEquals(Set.of(), service.resolveOperationalMailboxIds(agent));
        verify(mailboxMapper, never()).selectList(any());
        verify(enterpriseMapper, never()).selectBatchIds(any());
    }

    @Test
    void disabledGrant_shouldBeIgnoredEvenWhenReturnedByMapper() {
        mockRoles(2L, Set.of("AGENT"));
        UserDataGrantEntity disabled = enterpriseGrant(1L, 2L, 100L);
        disabled.setEnabled(false);
        when(userDataGrantMapper.selectList(any())).thenReturn(List.of(disabled));

        assertEquals(Set.of(), service.resolveReadableMailboxIds(agent));
        verify(mailboxMapper, never()).selectList(any());
    }

    @Test
    void invalidGrantTarget_shouldBeRejected_andInvalidStoredGrantIgnored() {
        BusinessException enterpriseError = assertThrows(BusinessException.class,
                () -> service.validateGrantTarget("ENTERPRISE", 100L, 10L));
        BusinessException mailboxError = assertThrows(BusinessException.class,
                () -> service.validateGrantTarget("MAILBOX", null, null));
        assertEquals(40001, enterpriseError.getCode());
        assertEquals(40001, mailboxError.getCode());

        mockRoles(2L, Set.of("AGENT"));
        UserDataGrantEntity invalid = enterpriseGrant(9L, 2L, 100L);
        invalid.setMailboxId(10L);
        when(userDataGrantMapper.selectList(any())).thenReturn(List.of(invalid));
        assertEquals(Set.of(), service.resolveReadableMailboxIds(agent));
    }

    @Test
    void assigneeCheck_shouldAllowGrantedMailbox_andRejectCrossMailbox() {
        UserEntity assignee = new UserEntity();
        assignee.setId(2L);
        assignee.setRoleCode("AGENT");
        assignee.setEnabled(true);
        when(userMapper.selectById(2L)).thenReturn(assignee);
        mockRoles(2L, Set.of("AGENT"));
        when(userDataGrantMapper.selectList(any())).thenReturn(List.of(mailboxGrant(1L, 2L, 10L)));
        when(mailboxMapper.selectList(any())).thenReturn(List.of(mailbox(10L, 100L, true)));
        when(enterpriseMapper.selectBatchIds(any())).thenReturn(List.of(enterprise(100L, true)));

        assertDoesNotThrow(() -> service.assertAssigneeCanAccessMailbox(2L, 10L));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertAssigneeCanAccessMailbox(2L, 11L));
        assertEquals(40302, ex.getCode());
    }

    @Test
    void assigneeEnterpriseCheck_shouldRequireExplicitEnterpriseGrant() {
        UserEntity assignee = new UserEntity();
        assignee.setId(2L);
        assignee.setRoleCode("AGENT");
        assignee.setEnabled(true);
        when(userMapper.selectById(2L)).thenReturn(assignee);
        mockRoles(2L, Set.of("AGENT"));
        when(userDataGrantMapper.selectList(any())).thenReturn(List.of(enterpriseGrant(1L, 2L, 100L)));
        when(mailboxMapper.selectList(any())).thenReturn(List.of(mailbox(10L, 100L, true)));
        when(enterpriseMapper.selectBatchIds(any())).thenReturn(List.of(enterprise(100L, true)));
        when(userDataGrantMapper.selectCount(any())).thenReturn(1L);

        assertDoesNotThrow(() -> service.assertAssigneeCanAccessEnterprise(2L, 100L));
    }

    @Test
    void assigneeCheck_whenTicketReplyPermissionMissing_shouldReject() {
        UserEntity assignee = new UserEntity();
        assignee.setId(2L);
        assignee.setRoleCode("AUTHQA_VIEWER");
        assignee.setEnabled(true);
        when(userMapper.selectById(2L)).thenReturn(assignee);
        when(permissionService.getUserPermissions(2L, "AUTHQA_VIEWER")).thenReturn(
                new PermissionService.PermissionContext(2L, Set.of("AUTHQA_VIEWER"), Set.of("ticket:read"), Map.of()));
        when(userDataGrantMapper.selectList(any())).thenReturn(List.of(mailboxGrant(1L, 2L, 10L)));
        when(mailboxMapper.selectList(any())).thenReturn(List.of(mailbox(10L, 100L, true)));
        when(enterpriseMapper.selectBatchIds(any())).thenReturn(List.of(enterprise(100L, true)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertAssigneeCanAccessMailbox(2L, 10L));

        assertEquals(40302, ex.getCode());
        assertTrue(ex.getMessage().contains("工单回复权限"));
    }

    @Test
    void requestScope_shouldCacheResolvedAccessAcrossMultipleCalls() {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
        mockRoles(2L, Set.of("AGENT"));
        when(userDataGrantMapper.selectList(any())).thenReturn(List.of(enterpriseGrant(1L, 2L, 100L)));
        when(mailboxMapper.selectList(any())).thenReturn(List.of(mailbox(10L, 100L, true)));
        when(enterpriseMapper.selectBatchIds(any())).thenReturn(List.of(enterprise(100L, true)));

        service.resolveVisibleEnterpriseIds(agent);
        service.resolveReadableMailboxIds(agent);
        service.resolveOperationalMailboxIds(agent);

        verify(permissionService, times(1)).getUserPermissions(2L, "AGENT");
        verify(userDataGrantMapper, times(1)).selectList(any());
        verify(mailboxMapper, times(1)).selectList(any());
        verify(enterpriseMapper, times(1)).selectBatchIds(any());
    }

    private void mockRoles(Long userId, Set<String> roles) {
        when(permissionService.getUserPermissions(eq(userId), any())).thenReturn(
                new PermissionService.PermissionContext(userId, roles, Set.of("ticket:reply"), Map.of()));
    }

    private static CurrentUserPrincipal principal(Long id, String roleCode) {
        return new CurrentUserPrincipal(id, "u" + id, "用户" + id, "u" + id + "@example.com", roleCode);
    }

    private static EnterpriseEntity enterprise(Long id, boolean enabled) {
        EnterpriseEntity entity = new EnterpriseEntity();
        entity.setId(id);
        entity.setEnterpriseName("企业" + id);
        entity.setEnabled(enabled);
        return entity;
    }

    private static MailboxEntity mailbox(Long id, Long enterpriseId, boolean enabled) {
        MailboxEntity entity = new MailboxEntity();
        entity.setId(id);
        entity.setEnterpriseId(enterpriseId);
        entity.setEnabled(enabled);
        return entity;
    }

    private static UserDataGrantEntity enterpriseGrant(Long id, Long userId, Long enterpriseId) {
        UserDataGrantEntity entity = new UserDataGrantEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setGrantType("ENTERPRISE");
        entity.setEnterpriseId(enterpriseId);
        entity.setEnabled(true);
        return entity;
    }

    private static UserDataGrantEntity mailboxGrant(Long id, Long userId, Long mailboxId) {
        UserDataGrantEntity entity = new UserDataGrantEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setGrantType("MAILBOX");
        entity.setMailboxId(mailboxId);
        entity.setEnabled(true);
        return entity;
    }
}
