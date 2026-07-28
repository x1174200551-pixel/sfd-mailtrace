package com.ntn.fziot.mailtrace.application.bizservice.customer;

import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.DataScopeService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.customer.CustomerPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.customer.CustomerVO;
import com.ntn.fziot.mailtrace.repox.mysql.dto.CustomerReadonlyRow;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.CustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerReadonlyServiceTest {

    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private PermissionService permissionService;
    @Spy
    private DataScopeService dataScopeService = new DataScopeService();

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
        allowAdminAndAgentOperationalPermissions();
    }

    @Test
    void pageCustomers_shouldReturnReadonlyRows() {
        when(customerMapper.countReadonlyCustomers("vip", true, 1L)).thenReturn(2L);
        when(customerMapper.selectReadonlyCustomers("vip", 0L, 10L, true, 1L)).thenReturn(List.of(
                row(100L, "vip@example.com", "VIP 客户", 3L),
                row(null, "only-ticket@example.com", null, 1L)
        ));

        CustomerPageResponse response = customerReadonlyService.pageCustomers(admin, " vip ", 1, 10);

        assertEquals(2L, response.total());
        assertEquals(1L, response.page());
        assertEquals(10L, response.size());
        assertEquals(1L, response.pages());
        assertEquals(2, response.records().size());
        assertEquals("vip@example.com", response.records().get(0).email());
        assertEquals(3L, response.records().get(0).ticketCount());
        assertEquals("only-ticket@example.com", response.records().get(1).email());
    }

    @Test
    void pageCustomers_whenAgent_shouldAllowAndNormalizePaging() {
        when(customerMapper.countReadonlyCustomers("", false, 2L)).thenReturn(0L);
        when(customerMapper.selectReadonlyCustomers("", 0L, 20L, false, 2L)).thenReturn(List.of());

        CustomerPageResponse response = customerReadonlyService.pageCustomers(agent, null, 0, 0);

        assertEquals(1L, response.page());
        assertEquals(20L, response.size());
        assertEquals(0L, response.pages());
    }

    @Test
    void pageCustomers_whenNotAgentOrAdmin_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> customerReadonlyService.pageCustomers(customer, null, 1, 20));

        assertTrue(ex.getMessage().contains("无权查看客户"));
    }

    @Test
    void getCustomer_shouldReturnByEmail() {
        when(customerMapper.selectReadonlyCustomerByEmail("vip@example.com", true, 1L))
                .thenReturn(row(100L, "vip@example.com", "VIP 客户", 3L));

        CustomerVO vo = customerReadonlyService.getCustomer(admin, " vip@example.com ");

        assertEquals(100L, vo.id());
        assertEquals("vip@example.com", vo.email());
        assertEquals("VIP 客户", vo.displayName());
        assertEquals(3L, vo.ticketCount());
        verify(customerMapper).selectReadonlyCustomerByEmail("vip@example.com", true, 1L);
    }

    @Test
    void getCustomer_whenAgent_shouldPassScopedMapperArguments() {
        when(customerMapper.selectReadonlyCustomerByEmail("vip@example.com", false, 2L))
                .thenReturn(row(100L, "vip@example.com", "VIP 客户", 1L));

        CustomerVO vo = customerReadonlyService.getCustomer(agent, "vip@example.com");

        assertEquals("vip@example.com", vo.email());
        verify(customerMapper).selectReadonlyCustomerByEmail("vip@example.com", false, 2L);
    }

    @Test
    void getCustomer_whenEmailBlank_shouldReject() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> customerReadonlyService.getCustomer(admin, " "));

        assertTrue(ex.getMessage().contains("客户邮箱不能为空"));
    }

    @Test
    void getCustomer_whenNotExists_shouldReject() {
        when(customerMapper.selectReadonlyCustomerByEmail("missing@example.com", true, 1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> customerReadonlyService.getCustomer(admin, "missing@example.com"));

        assertTrue(ex.getMessage().contains("客户不存在"));
    }

    private CustomerReadonlyRow row(Long id, String email, String displayName, Long ticketCount) {
        CustomerReadonlyRow row = new CustomerReadonlyRow();
        row.setId(id);
        row.setEmail(email);
        row.setDisplayName(displayName);
        row.setLastMailAt(LocalDateTime.parse("2026-07-27T10:00:00"));
        row.setTicketCount(ticketCount);
        row.setRemark("备注");
        row.setCreatedAt(LocalDateTime.parse("2026-07-27T09:00:00"));
        return row;
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
