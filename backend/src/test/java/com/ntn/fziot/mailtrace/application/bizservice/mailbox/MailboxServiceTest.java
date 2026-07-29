package com.ntn.fziot.mailtrace.application.bizservice.mailbox;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.crypto.MailPasswordCipher;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.mailbox.MailboxPageResponse;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailboxServiceTest {

    @Mock
    private MailboxMapper mailboxMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private OperationLogMapper operationLogMapper;
    @Mock
    private MailPasswordCipher mailPasswordCipher;
    @Mock
    private PermissionService permissionService;

    private MailboxService mailboxService;

    private final CurrentUserPrincipal operator = new CurrentUserPrincipal(
            8L, "mail_user", "邮箱用户", "mail_user@example.com", "CUSTOM");

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "MailboxServiceTest.MailboxEntity");
        TableInfoHelper.initTableInfo(assistant, MailboxEntity.class);
        mailboxService = new MailboxService(
                mailboxMapper,
                userMapper,
                operationLogMapper,
                mailPasswordCipher,
                permissionService
        );
    }

    @Test
    void pageMailboxes_whenOnlyMailboxMenuPermission_shouldAllowRead() {
        when(permissionService.hasPermission(operator, "mailbox:read")).thenReturn(false);
        when(permissionService.hasPermission(operator, "menu:mailboxes")).thenReturn(true);
        Page<MailboxEntity> page = Page.of(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(mailboxMapper.selectPage(any(), any())).thenReturn(page);
        when(mailboxMapper.selectCount(any())).thenReturn(0L);

        MailboxPageResponse response = mailboxService.pageMailboxes(operator, null, "ALL", null, 1, 10);

        assertEquals(0, response.total());
        assertEquals(0, response.records().size());
    }

    @Test
    void pageMailboxes_whenMissingMenuAndReadPermission_shouldReject() {
        when(permissionService.hasPermission(operator, "mailbox:read")).thenReturn(false);
        when(permissionService.hasPermission(operator, "menu:mailboxes")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> mailboxService.pageMailboxes(operator, null, "ALL", null, 1, 10));

        assertEquals(40302, ex.getCode());
        verify(mailboxMapper, never()).selectPage(any(), any());
    }
}
