package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataScopeServiceTest {

    private final PermissionService permissionService = mock(PermissionService.class);
    private final EnterpriseMailboxAccessService accessService = mock(EnterpriseMailboxAccessService.class);
    private final DataScopeService dataScopeService = new DataScopeService(permissionService, accessService);
    private final CurrentUserPrincipal operator = new CurrentUserPrincipal(
            2L, "operator", "处理人", "operator@example.com", "CUSTOM");

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "DataScopeServiceTest.TicketEntity");
        TableInfoHelper.initTableInfo(assistant, TicketEntity.class);
    }

    @BeforeEach
    void allowFunctionalAccess() {
        when(permissionService.getCurrentPermissions(operator)).thenReturn(
                new PermissionService.PermissionContext(2L, Set.of("CUSTOM"), Set.of("ticket:read"), Map.of()));
    }

    @Test
    void applyTicketScope_shouldUseReadableMailboxIds() {
        when(accessService.resolveReadableMailboxIds(operator)).thenReturn(Set.of(11L, 12L));
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<>();

        dataScopeService.applyTicketScope(wrapper, operator);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("mailbox_id"));
        assertTrue(sql.contains("IN"));
    }

    @Test
    void applyTicketScope_whenGrantEmpty_shouldDenyAllExplicitly() {
        when(accessService.resolveReadableMailboxIds(operator)).thenReturn(Set.of());
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<>();

        dataScopeService.applyTicketScope(wrapper, operator);

        assertTrue(wrapper.getSqlSegment().contains("1 = 0"));
    }

    @Test
    void assertTicketVisible_shouldDelegateReadableMailboxCheck() {
        dataScopeService.assertTicketVisible(operator, ticket(11L));

        verify(accessService).assertMailboxReadable(operator, 11L);
    }

    @Test
    void assertTicketOperable_shouldDelegateOperationalMailboxCheck() {
        dataScopeService.assertTicketOperable(operator, ticket(12L));

        verify(accessService).assertMailboxOperational(operator, 12L);
    }

    private TicketEntity ticket(Long mailboxId) {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(100L);
        ticket.setMailboxId(mailboxId);
        return ticket;
    }
}
