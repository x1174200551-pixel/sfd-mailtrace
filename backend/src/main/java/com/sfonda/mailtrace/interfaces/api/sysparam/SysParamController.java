package com.sfonda.mailtrace.interfaces.api.sysparam;

import com.sfonda.mailtrace.application.bizservice.sysparam.TicketNumberRuleService;
import com.sfonda.mailtrace.infrastructure.basic.BasicResult;
import com.sfonda.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.sfonda.mailtrace.interfaces.vo.sysparam.TicketNumberRuleRequest;
import com.sfonda.mailtrace.interfaces.vo.sysparam.TicketNumberRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统参数", description = "业务可见编号规则配置")
@RestController
@RequestMapping("/v1/sys-params")
@RequiredArgsConstructor
public class SysParamController {

    private final TicketNumberRuleService ticketNumberRuleService;

    @Operation(summary = "查询工单编号规则")
    @GetMapping("/ticket-number-rule")
    public BasicResult<TicketNumberRuleVO> getTicketNumberRule(
            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return BasicResult.ok(ticketNumberRuleService.getRule(principal));
    }

    @Operation(summary = "预览工单编号规则")
    @PostMapping("/ticket-number-rule/preview")
    public BasicResult<TicketNumberRuleVO> previewTicketNumberRule(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody TicketNumberRuleRequest request) {
        return BasicResult.ok(ticketNumberRuleService.previewRule(principal, request));
    }

    @Operation(summary = "保存工单编号规则")
    @PutMapping("/ticket-number-rule")
    public BasicResult<TicketNumberRuleVO> updateTicketNumberRule(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody TicketNumberRuleRequest request) {
        return BasicResult.ok(ticketNumberRuleService.updateRule(principal, request));
    }
}
