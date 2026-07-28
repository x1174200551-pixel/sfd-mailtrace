package com.ntn.fziot.mailtrace.application.bizservice.customer;

import com.ntn.fziot.mailtrace.application.bizservice.common.BusinessException;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.customer.CustomerPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.customer.CustomerVO;
import com.ntn.fziot.mailtrace.repox.mysql.dto.CustomerReadonlyRow;
import com.ntn.fziot.mailtrace.repox.mysql.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerReadonlyService {

    private static final int CODE_BAD_REQUEST = 40001;
    private static final int CODE_FORBIDDEN = 40302;
    private static final int CODE_NOT_FOUND = 40401;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_AGENT = "AGENT";

    private final CustomerMapper customerMapper;

    /**
     * 客户只读分页查询。
     */
    public CustomerPageResponse pageCustomers(CurrentUserPrincipal principal, String keyword,
                                              Integer page, Integer size) {
        // 1、客户只读页面仅允许管理员和处理人访问。
        assertAgentOrAdmin(principal);

        // 2、规范化分页和关键字，客户来源合并客户档案表与历史工单邮箱。
        long currentPage = normalizePage(page);
        long pageSize = normalizeSize(size);
        String normalizedKeyword = normalize(keyword);
        long total = customerMapper.countReadonlyCustomers(normalizedKeyword);
        long pages = total == 0 ? 0 : (long) Math.ceil((double) total / pageSize);
        long offset = (currentPage - 1) * pageSize;

        // 3、查询最近来信、工单数等只读字段。
        List<CustomerVO> records = customerMapper.selectReadonlyCustomers(normalizedKeyword, offset, pageSize)
                .stream()
                .map(this::toVO)
                .toList();

        // 4、返回分页结果。
        return new CustomerPageResponse(records, total, currentPage, pageSize, pages);
    }

    /**
     * 按邮箱查询客户只读详情。
     */
    public CustomerVO getCustomer(CurrentUserPrincipal principal, String email) {
        // 1、校验权限和邮箱参数。
        assertAgentOrAdmin(principal);
        String normalizedEmail = normalize(email);
        if (normalizedEmail.isEmpty()) {
            throw new BusinessException(CODE_BAD_REQUEST, "客户邮箱不能为空");
        }

        // 2、按邮箱合并客户档案和历史工单聚合详情。
        CustomerReadonlyRow row = customerMapper.selectReadonlyCustomerByEmail(normalizedEmail);
        if (row == null) {
            throw new BusinessException(CODE_NOT_FOUND, "客户不存在");
        }

        // 3、返回只读详情。
        return toVO(row);
    }

    private CustomerVO toVO(CustomerReadonlyRow row) {
        return new CustomerVO(
                row.getId(),
                row.getEmail(),
                row.getDisplayName(),
                row.getLastMailAt(),
                row.getTicketCount() == null ? 0L : row.getTicketCount(),
                row.getRemark(),
                row.getCreatedAt()
        );
    }

    private void assertAgentOrAdmin(CurrentUserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(CODE_FORBIDDEN, "未登录");
        }
        if (!ROLE_ADMIN.equals(principal.roleCode()) && !ROLE_AGENT.equals(principal.roleCode())) {
            throw new BusinessException(CODE_FORBIDDEN, "仅管理员和处理人可查看客户");
        }
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
