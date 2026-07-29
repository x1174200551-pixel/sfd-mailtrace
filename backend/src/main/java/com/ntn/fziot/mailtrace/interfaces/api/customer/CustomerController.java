package com.ntn.fziot.mailtrace.interfaces.api.customer;

import com.ntn.fziot.mailtrace.application.bizservice.customer.CustomerReadonlyService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.infrastructure.security.RequirePermission;
import com.ntn.fziot.mailtrace.interfaces.vo.customer.CustomerPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.customer.CustomerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "客户", description = "客户只读查询")
@RestController
@RequestMapping("/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerReadonlyService customerReadonlyService;

    @Operation(summary = "客户分页查询")
    @GetMapping
    @RequirePermission(value = "customer:read", message = "无权查看客户")
    public BasicResult<CustomerPageResponse> pageCustomers(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return BasicResult.ok(customerReadonlyService.pageCustomers(principal, keyword, page, size));
    }

    @Operation(summary = "客户详情")
    @GetMapping("/{email}")
    @RequirePermission(value = "customer:read", message = "无权查看客户")
    public BasicResult<CustomerVO> getCustomer(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable String email) {
        return BasicResult.ok(customerReadonlyService.getCustomer(principal, email));
    }
}
