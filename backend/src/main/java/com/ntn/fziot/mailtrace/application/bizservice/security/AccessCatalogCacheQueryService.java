package com.ntn.fziot.mailtrace.application.bizservice.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ntn.fziot.mailtrace.infrastructure.cache.MtRedisCacheable;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailboxEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 企业与邮箱访问目录。缓存内容仅包含 ID 和启用关系，不包含邮箱密码等敏感配置。
 */
@Service
@RequiredArgsConstructor
public class AccessCatalogCacheQueryService {

    private final EnterpriseMapper enterpriseMapper;
    private final MailboxMapper mailboxMapper;

    @MtRedisCacheable(cacheName = "access-catalog", key = "'all'", ttlSeconds = 300)
    public AccessCatalog getAccessCatalog() {
        List<EnterpriseEntity> enterprises = enterpriseMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, EnterpriseEntity> enterpriseById = enterprises.stream()
                .filter(Objects::nonNull)
                .filter(enterprise -> enterprise.getId() != null)
                .collect(Collectors.toMap(
                        EnterpriseEntity::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<MailboxEntity> mailboxes = mailboxMapper.selectList(new LambdaQueryWrapper<>());

        Set<Long> readableMailboxIds = mailboxes.stream()
                .map(MailboxEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> operationalMailboxIds = mailboxes.stream()
                .filter(mailbox -> isOperational(mailbox, enterpriseById))
                .map(MailboxEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new AccessCatalog(
                enterpriseById.keySet(),
                readableMailboxIds,
                operationalMailboxIds
        );
    }

    private boolean isOperational(MailboxEntity mailbox, Map<Long, EnterpriseEntity> enterpriseById) {
        if (mailbox == null || mailbox.getId() == null || !Boolean.TRUE.equals(mailbox.getEnabled())) {
            return false;
        }
        EnterpriseEntity enterprise = enterpriseById.get(mailbox.getEnterpriseId());
        return enterprise != null && Boolean.TRUE.equals(enterprise.getEnabled());
    }

    public record AccessCatalog(
            Set<Long> visibleEnterpriseIds,
            Set<Long> readableMailboxIds,
            Set<Long> operationalMailboxIds
    ) {
        public AccessCatalog {
            visibleEnterpriseIds = Set.copyOf(visibleEnterpriseIds == null ? Set.of() : visibleEnterpriseIds);
            readableMailboxIds = Set.copyOf(readableMailboxIds == null ? Set.of() : readableMailboxIds);
            operationalMailboxIds = Set.copyOf(operationalMailboxIds == null ? Set.of() : operationalMailboxIds);
        }
    }
}
