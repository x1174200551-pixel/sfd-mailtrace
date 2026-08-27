package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntn.fziot.mailtrace.application.bizservice.mailfetch.MailFetchLogBizService;
import com.ntn.fziot.mailtrace.application.bizservice.mailsend.MailSendLogBizService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailFetchLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailFetchLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailSendLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailLogScopeServiceTest {

    @Mock
    private MailFetchLogMapper mailFetchLogMapper;
    @Mock
    private MailSendLogMapper mailSendLogMapper;
    @Mock
    private MailboxMapper mailboxMapper;
    @Mock
    private PermissionService permissionService;
    @Mock
    private EnterpriseMailboxAccessService accessService;

    private MailFetchLogBizService fetchLogService;
    private MailSendLogBizService sendLogService;
    private final CurrentUserPrincipal emptyUser = new CurrentUserPrincipal(
            8L, "empty", "空授权", "empty@example.com", "CUSTOM");

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "MailLogScope.Fetch"), MailFetchLogEntity.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "MailLogScope.Send"), MailSendLogEntity.class);
    }

    @BeforeEach
    void setUp() {
        fetchLogService = new MailFetchLogBizService(
                mailFetchLogMapper, mailboxMapper, permissionService, accessService);
        sendLogService = new MailSendLogBizService(
                mailSendLogMapper, permissionService, accessService);
        when(accessService.resolveReadableMailboxIds(emptyUser)).thenReturn(Set.of());
    }

    @Test
    void statsAndPendingCount_whenGrantEmpty_shouldReturnZeroWithoutFullTableQuery() {
        assertEquals(0, fetchLogService.stats(emptyUser).totalCount());
        assertEquals(0, sendLogService.stats(emptyUser).totalCount());
        assertEquals(0, sendLogService.pendingCount(emptyUser));

        verify(mailFetchLogMapper, never()).selectList(any());
        verify(mailSendLogMapper, never()).selectList(any());
        verify(mailSendLogMapper, never()).selectCount(any());
    }

    @Test
    void pages_whenGrantEmpty_shouldAppendExplicitDenyAll() {
        Page<MailFetchLogEntity> fetchPage = Page.of(1, 20);
        fetchPage.setRecords(List.of());
        when(mailFetchLogMapper.selectPage(any(), any())).thenReturn(fetchPage);
        Page<MailSendLogEntity> sendPage = Page.of(1, 20);
        sendPage.setRecords(List.of());
        when(mailSendLogMapper.selectPage(any(), any())).thenReturn(sendPage);

        fetchLogService.pageFetchLogs(emptyUser, null, null, null, null, null, null, 1, 20);
        sendLogService.pageSendLogs(emptyUser, null, null, null, null, null, null, 1, 20);

        ArgumentCaptor<LambdaQueryWrapper<MailFetchLogEntity>> fetchCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        ArgumentCaptor<LambdaQueryWrapper<MailSendLogEntity>> sendCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mailFetchLogMapper).selectPage(any(), fetchCaptor.capture());
        verify(mailSendLogMapper).selectPage(any(), sendCaptor.capture());
        assertTrue(fetchCaptor.getValue().getSqlSegment().contains("1 = 0"));
        assertTrue(sendCaptor.getValue().getSqlSegment().contains("1 = 0"));
    }
}
