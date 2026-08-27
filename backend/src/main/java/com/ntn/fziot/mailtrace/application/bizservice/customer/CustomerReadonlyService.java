package com.ntn.fziot.mailtrace.application.bizservice.customer;

import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.application.bizservice.security.EnterpriseMailboxAccessService;
import com.ntn.fziot.mailtrace.application.bizservice.security.PermissionService;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.customer.CustomerPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.customer.CustomerVO;
import com.ntn.fziot.mailtrace.repox.mysql.dto.CustomerReadonlyRow;
import com.ntn.fziot.mailtrace.repox.mysql.entity.EnterpriseEntity;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.CustomerMapper;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.EnterpriseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomerReadonlyService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_NOT_FOUND = 40401;

    private final CustomerMapper customerMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final EnterpriseMailboxAccessService enterpriseMailboxAccessService;
    private final PermissionService permissionService;

    /**
     * 客户只读分页查询。
     */
    public CustomerPageResponse pageCustomers(CurrentUserPrincipal principal, String keyword,
                                              Long enterpriseId, Long mailboxId,
                                              Integer page, Integer size) {
        // 1、客户只读页面仅允许管理员和处理人访问。
        permissionService.assertPermission(principal, "customer:read", "无权查看客户");

        // 2、规范化分页和关键字，客户来源合并客户档案表与历史工单邮箱。
        long currentPage = normalizePage(page);
        long pageSize = normalizeSize(size);
        String normalizedKeyword = normalize(keyword);
        Set<Long> readableMailboxIds = enterpriseMailboxAccessService.resolveReadableMailboxIds(principal);
        if (readableMailboxIds.isEmpty()) {
            return new CustomerPageResponse(List.of(), 0L, currentPage, pageSize, 0L);
        }
        long total = customerMapper.countReadonlyCustomers(
                normalizedKeyword, enterpriseId, mailboxId, readableMailboxIds);
        long pages = total == 0 ? 0 : (long) Math.ceil((double) total / pageSize);
        long offset = (currentPage - 1) * pageSize;

        // 3、查询最近来信、工单数等只读字段。
        List<CustomerVO> records = customerMapper.selectReadonlyCustomers(
                        normalizedKeyword, enterpriseId, mailboxId, offset, pageSize, readableMailboxIds)
                .stream()
                .map(this::toVO)
                .toList();

        // 4、返回分页结果。
        return new CustomerPageResponse(records, total, currentPage, pageSize, pages);
    }

    /**
     * 按邮箱查询客户只读详情。
     */
    public CustomerVO getCustomer(CurrentUserPrincipal principal, Long enterpriseId, String email) {
        // 1、校验权限和邮箱参数。
        permissionService.assertPermission(principal, "customer:read", "无权查看客户");
        String normalizedEmail = normalize(email);
        if (enterpriseId == null || normalizedEmail.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "企业和客户邮箱不能为空");
        }

        // 2、按邮箱合并客户档案和历史工单聚合详情。
        Set<Long> readableMailboxIds = enterpriseMailboxAccessService.resolveReadableMailboxIds(principal);
        if (readableMailboxIds.isEmpty()) {
            throw new BusinessException(CODE_NOT_FOUND, "客户不存在");
        }
        CustomerReadonlyRow row = customerMapper.selectReadonlyCustomerByEmail(
                enterpriseId, normalizedEmail, readableMailboxIds);
        if (row == null) {
            throw new BusinessException(CODE_NOT_FOUND, "客户不存在");
        }

        // 3、返回只读详情。
        return toVO(row);
    }

    private CustomerVO toVO(CustomerReadonlyRow row) {
        return new CustomerVO(
                row.getId(),
                row.getEnterpriseId(),
                resolveEnterpriseName(row.getEnterpriseId()),
                row.getEmail(),
                row.getDisplayName(),
                row.getLastMailAt(),
                row.getTicketCount() == null ? 0L : row.getTicketCount(),
                row.getRemark(),
                row.getCreatedAt()
        );
    }

    private String resolveEnterpriseName(Long enterpriseId) {
        if (enterpriseId == null) return null;
        EnterpriseEntity enterprise = enterpriseMapper.selectById(enterpriseId);
        return enterprise == null ? null : enterprise.getEnterpriseName();
    }

    private long normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
