package com.ntn.fziot.mailtrace.application.bizservice.enterprise;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseOptionVO;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseSaveRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.enterprise.EnterpriseVO;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.OperationLogMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.MailboxMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.TicketMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnterpriseServiceTest {

    @Mock
    private EnterpriseMapper enterpriseMapper;
    @Mock
    private OperationLogMapper operationLogMapper;
    @Mock
    private PermissionService permissionService;
    @Mock
    private EnterpriseMailboxAccessService accessService;
    @Mock
    private MailboxMapper mailboxMapper;
    @Mock
    private TicketMapper ticketMapper;

    @InjectMocks
    private EnterpriseService enterpriseService;

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "系统管理员", "admin@example.com", "ADMIN");

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "EnterpriseServiceTest"),
                EnterpriseEntity.class);
    }

    @Test
    void createEnterprise_shouldNormalizeAndPersist() {
        when(enterpriseMapper.selectCount(any())).thenReturn(0L);
        when(enterpriseMapper.insert(any())).thenAnswer(invocation -> {
            EnterpriseEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        });
        when(enterpriseMapper.selectById(100L)).thenReturn(enterprise(100L, "示例企业", true));

        EnterpriseSaveRequest request = new EnterpriseSaveRequest();
        request.setEnterpriseName(" 示例企业 ");
        request.setContactEmail(" SALES@EXAMPLE.COM ");
        EnterpriseVO result = enterpriseService.createEnterprise(admin, request);

        ArgumentCaptor<EnterpriseEntity> captor = ArgumentCaptor.forClass(EnterpriseEntity.class);
        verify(enterpriseMapper).insert(captor.capture());
        assertEquals("示例企业", captor.getValue().getEnterpriseName());
        assertEquals("sales@example.com", captor.getValue().getContactEmail());
        assertEquals("admin", captor.getValue().getCreatedBy());
        assertEquals(100L, result.id());
    }

    @Test
    void createEnterprise_whenNameExists_shouldReject() {
        when(enterpriseMapper.selectCount(any())).thenReturn(1L);
        EnterpriseSaveRequest request = new EnterpriseSaveRequest();
        request.setEnterpriseName("重复企业");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> enterpriseService.createEnterprise(admin, request));

        assertTrue(exception.getMessage().contains("企业名称已存在"));
        verify(enterpriseMapper, never()).insert(any());
    }

    @Test
    void listVisibleOptions_whenGrantEmpty_shouldNotQueryEnterpriseTable() {
        when(accessService.resolveVisibleEnterpriseIds(admin)).thenReturn(Set.of());

        List<EnterpriseOptionVO> result = enterpriseService.listVisibleOptions(admin, null);

        assertTrue(result.isEmpty());
        verify(enterpriseMapper, never()).selectList(any());
    }

    @Test
    void listVisibleOptions_shouldReturnOnlyAccessServiceScope() {
        when(accessService.resolveVisibleEnterpriseIds(admin)).thenReturn(Set.of(10L));
        when(enterpriseMapper.selectList(any())).thenReturn(List.of(enterprise(10L, "授权企业", true)));

        List<EnterpriseOptionVO> result = enterpriseService.listVisibleOptions(admin, true);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).id());
    }

    @Test
    void listEnterprises_shouldReturnPagedRecordsAndGlobalSummary() {
        when(enterpriseMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<EnterpriseEntity> page = invocation.getArgument(0);
            page.setRecords(List.of(enterprise(10L, "分页企业", true)));
            page.setTotal(12L);
            return page;
        });
        when(enterpriseMapper.selectCount(any())).thenReturn(20L, 15L);
        when(mailboxMapper.selectCount(any())).thenReturn(2L);
        when(ticketMapper.selectCount(any())).thenReturn(8L);

        var result = enterpriseService.listEnterprises(admin, null, null, 2, 10);

        assertEquals(1, result.records().size());
        assertEquals(12L, result.total());
        assertEquals(2L, result.page());
        assertEquals(10L, result.size());
        assertEquals(2L, result.pages());
        assertEquals(20L, result.totalCount());
        assertEquals(15L, result.enabledCount());
    }

    private EnterpriseEntity enterprise(Long id, String name, boolean enabled) {
        EnterpriseEntity entity = new EnterpriseEntity();
        entity.setId(id);
        entity.setEnterpriseName(name);
        entity.setEnabled(enabled);
        return entity;
    }
}
