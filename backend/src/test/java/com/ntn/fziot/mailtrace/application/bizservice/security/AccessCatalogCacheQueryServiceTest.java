package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.ntn.fziot.mailtrace.infrastructure.cache.MtRedisCacheable;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessCatalogCacheQueryServiceTest {

    @Mock
    private EnterpriseMapper enterpriseMapper;
    @Mock
    private MailboxMapper mailboxMapper;
    @InjectMocks
    private AccessCatalogCacheQueryService service;

    @Test
    void getAccessCatalog_shouldSeparateReadableAndOperationalIds() throws Exception {
        when(enterpriseMapper.selectList(any())).thenReturn(List.of(
                enterprise(100L, true),
                enterprise(200L, false)
        ));
        when(mailboxMapper.selectList(any())).thenReturn(List.of(
                mailbox(10L, 100L, true),
                mailbox(11L, 100L, false),
                mailbox(12L, 200L, true)
        ));

        AccessCatalogCacheQueryService.AccessCatalog result = service.getAccessCatalog();

        assertEquals(Set.of(100L, 200L), result.visibleEnterpriseIds());
        assertEquals(Set.of(10L, 11L, 12L), result.readableMailboxIds());
        assertEquals(Set.of(10L), result.operationalMailboxIds());
        Method method = AccessCatalogCacheQueryService.class.getMethod("getAccessCatalog");
        MtRedisCacheable annotation = method.getAnnotation(MtRedisCacheable.class);
        assertEquals("access-catalog", annotation.cacheName());
        assertEquals(300, annotation.ttlSeconds());
    }

    private EnterpriseEntity enterprise(Long id, boolean enabled) {
        EnterpriseEntity entity = new EnterpriseEntity();
        entity.setId(id);
        entity.setEnabled(enabled);
        return entity;
    }

    private MailboxEntity mailbox(Long id, Long enterpriseId, boolean enabled) {
        MailboxEntity entity = new MailboxEntity();
        entity.setId(id);
        entity.setEnterpriseId(enterpriseId);
        entity.setEnabled(enabled);
        return entity;
    }
}
