package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailboxAccessShadowServiceTest {

    @Mock
    private EnterpriseMailboxAccessService enterpriseMailboxAccessService;
    @Mock
    private MailboxMapper mailboxMapper;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private DataScopeService dataScopeService;

    private MailboxAccessShadowService service;
    private final CurrentUserPrincipal principal =
            new CurrentUserPrincipal(2L, "agent", "处理人", "agent@example.com", "AGENT");

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(configuration, "MailboxAccessShadowServiceTest.TicketEntity"),
                TicketEntity.class);
    }

    @BeforeEach
    void setUp() {
        service = new MailboxAccessShadowService(
                enterpriseMailboxAccessService,
                mailboxMapper,
                ticketMapper,
                dataScopeService
        );
    }

    @Test
    void compareMailboxListScope_shouldReturnOnlyCountsWithoutChangingSourceSets() {
        when(mailboxMapper.selectList(any())).thenReturn(List.of(mailbox(10L), mailbox(11L), mailbox(12L)));
        when(enterpriseMailboxAccessService.resolveReadableMailboxIds(principal)).thenReturn(Set.of(10L, 12L, 13L));

        MailboxAccessShadowService.ShadowComparison comparison = service.compareMailboxListScope(principal);

        assertEquals(3, comparison.legacyCount());
        assertEquals(3, comparison.targetCount());
        assertEquals(1, comparison.legacyOnlyCount());
        assertEquals(1, comparison.targetOnlyCount());
        assertFalse(comparison.matches());
    }

    @Test
    void safeComparison_shouldSwallowShadowFailure() {
        when(mailboxMapper.selectList(any())).thenThrow(new IllegalStateException("shadow unavailable"));

        assertDoesNotThrow(() -> service.compareMailboxListScopeSafely(principal));
    }

    @Test
    void compareTicketScope_shouldUseLegacyDataScopeWithoutChangingIt() {
        when(ticketMapper.selectObjs(any())).thenReturn(List.of(10L, 11L));
        when(enterpriseMailboxAccessService.resolveReadableMailboxIds(principal)).thenReturn(Set.of(10L, 12L));

        MailboxAccessShadowService.ShadowComparison comparison = service.compareTicketScope(principal);

        assertEquals(2, comparison.legacyCount());
        assertEquals(2, comparison.targetCount());
        assertEquals(1, comparison.legacyOnlyCount());
        assertEquals(1, comparison.targetOnlyCount());
        verify(dataScopeService).applyTicketScope(any(), org.mockito.Mockito.eq(principal));
    }

    private static MailboxEntity mailbox(Long id) {
        MailboxEntity entity = new MailboxEntity();
        entity.setId(id);
        return entity;
    }
}
