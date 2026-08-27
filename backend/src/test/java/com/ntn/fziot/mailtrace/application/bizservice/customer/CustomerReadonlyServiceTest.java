package com.ntn.fziot.mailtrace.application.bizservice.customer;

import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.customer.CustomerPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.customer.CustomerVO;
import com.ntn.fziot.mailtrace.repox.mysql.dto.CustomerReadonlyRow;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.CustomerMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerReadonlyServiceTest {

    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private EnterpriseMapper enterpriseMapper;
    @Mock
    private EnterpriseMailboxAccessService enterpriseMailboxAccessService;
    @Mock
    private PermissionService permissionService;
    @InjectMocks
    private CustomerReadonlyService customerReadonlyService;

    private final CurrentUserPrincipal admin = new CurrentUserPrincipal(
            1L, "admin", "系统管理员", "admin@example.com", "ADMIN");
    private final CurrentUserPrincipal agent = new CurrentUserPrincipal(
            2L, "agent", "处理人", "agent@example.com", "AGENT");
    private final CurrentUserPrincipal customer = new CurrentUserPrincipal(
            3L, "customer", "客户", "customer@example.com", "CUSTOMER");

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            CurrentUserPrincipal principal = invocation.getArgument(0);
            String message = invocation.getArgument(2);
            if (principal == null || "CUSTOMER".equals(principal.roleCode())) {
                throw new BusinessException(40302, principal == null ? "未登录" : message);
            }
            return null;
        }).when(permissionService).assertPermission(any(), any(), any());
        org.mockito.Mockito.lenient().when(enterpriseMailboxAccessService.resolveReadableMailboxIds(admin))
                .thenReturn(Set.of(11L, 12L, 13L));
        org.mockito.Mockito.lenient().when(enterpriseMailboxAccessService.resolveReadableMailboxIds(agent))
                .thenReturn(Set.of(12L));
    }

    @Test
    void pageCustomers_shouldPassReadableMailboxScope() {
        Set<Long> scope = Set.of(11L, 12L, 13L);
        when(customerMapper.countReadonlyCustomers("vip", null, null, scope)).thenReturn(2L);
        when(customerMapper.selectReadonlyCustomers("vip", null, null, 0L, 10L, scope)).thenReturn(List.of(
                row(100L, "vip@example.com", "VIP 客户", 3L),
                row(null, "only-ticket@example.com", null, 1L)
        ));

        CustomerPageResponse response = customerReadonlyService.pageCustomers(admin, " vip ", null, null, 1, 10);

        assertEquals(2L, response.total());
        assertEquals(2, response.records().size());
        verify(customerMapper).countReadonlyCustomers("vip", null, null, scope);
        verify(customerMapper).selectReadonlyCustomers("vip", null, null, 0L, 10L, scope);
    }

    @Test
    void pageCustomers_whenAgent_shouldUseOnlyGrantedMailbox() {
        Set<Long> scope = Set.of(12L);
        when(customerMapper.countReadonlyCustomers("", null, null, scope)).thenReturn(0L);
        when(customerMapper.selectReadonlyCustomers("", null, null, 0L, 20L, scope)).thenReturn(List.of());

        CustomerPageResponse response = customerReadonlyService.pageCustomers(agent, null, null, null, 0, 0);

        assertEquals(1L, response.page());
        assertEquals(20L, response.size());
        verify(customerMapper).countReadonlyCustomers("", null, null, scope);
    }

    @Test
    void pageCustomers_whenGrantEmpty_shouldReturnEmptyWithoutQuery() {
        CurrentUserPrincipal emptyScopeUser = new CurrentUserPrincipal(
                8L, "scoped", "空范围用户", "scoped@example.com", "CUSTOM");
        when(enterpriseMailboxAccessService.resolveReadableMailboxIds(emptyScopeUser)).thenReturn(Set.of());

        CustomerPageResponse response = customerReadonlyService.pageCustomers(emptyScopeUser, null, null, null, 1, 20);

        assertEquals(0, response.total());
        assertTrue(response.records().isEmpty());
        verify(customerMapper, never()).countReadonlyCustomers(any(), any(), any(), any());
        verify(customerMapper, never()).selectReadonlyCustomers(any(), any(), any(), anyLong(), anyLong(), any());
    }

    @Test
    void getCustomer_shouldPassReadableMailboxScope() {
        Set<Long> scope = Set.of(12L);
        when(customerMapper.selectReadonlyCustomerByEmail(1L, "vip@example.com", scope))
                .thenReturn(row(100L, "vip@example.com", "VIP 客户", 1L));

        CustomerVO vo = customerReadonlyService.getCustomer(agent, 1L, " vip@example.com ");

        assertEquals("vip@example.com", vo.email());
        verify(customerMapper).selectReadonlyCustomerByEmail(1L, "vip@example.com", scope);
    }

    @Test
    void getCustomer_whenGrantEmpty_shouldReturnNotFoundWithoutQuery() {
        CurrentUserPrincipal emptyScopeUser = new CurrentUserPrincipal(
                8L, "scoped", "空范围用户", "scoped@example.com", "CUSTOM");
        when(enterpriseMailboxAccessService.resolveReadableMailboxIds(emptyScopeUser)).thenReturn(Set.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> customerReadonlyService.getCustomer(emptyScopeUser, 1L, "vip@example.com"));

        assertEquals(40401, ex.getCode());
        verify(customerMapper, never()).selectReadonlyCustomerByEmail(any(), any(), any());
    }

    @Test
    void pageCustomers_whenFunctionalPermissionMissing_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> customerReadonlyService.pageCustomers(customer, null, null, null, 1, 20));

        assertTrue(ex.getMessage().contains("无权查看客户"));
    }

    @Test
    void getCustomer_whenEmailBlank_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> customerReadonlyService.getCustomer(admin, 1L, " "));

        assertTrue(ex.getMessage().contains("企业和客户邮箱不能为空"));
    }

    private CustomerReadonlyRow row(Long id, String email, String displayName, Long ticketCount) {
        CustomerReadonlyRow row = new CustomerReadonlyRow();
        row.setId(id);
        row.setEnterpriseId(1L);
        row.setEmail(email);
        row.setDisplayName(displayName);
        row.setLastMailAt(LocalDateTime.parse("2026-07-27T10:00:00"));
        row.setTicketCount(ticketCount);
        row.setRemark("备注");
        row.setCreatedAt(LocalDateTime.parse("2026-07-27T09:00:00"));
        return row;
    }
}
