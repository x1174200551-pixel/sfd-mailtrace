package com.ntn.fziot.mailtrace.interfaces.api.ticket;

import com.ntn.fziot.mailtrace.application.bizservice.ticket.TicketBizService;
import com.ntn.fziot.mailtrace.infrastructure.basic.BasicResult;
import com.ntn.fziot.mailtrace.infrastructure.security.CurrentUserPrincipal;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketAssignRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketPageResponse;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketPriorityRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketRemarkRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketReplyRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketStatusRequest;
import com.ntn.fziot.mailtrace.interfaces.vo.ticket.TicketVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "工单管理", description = "工单查询、分配、回复、状态变更")
@RestController
@RequestMapping("/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketBizService ticketBizService;

    @Operation(summary = "工单分页查询")
    @GetMapping
    public BasicResult<TicketPageResponse> pageTickets(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean slaBreached,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long mailboxId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return BasicResult.ok(ticketBizService.pageTickets(
                principal, keyword, status, slaBreached, assigneeId, mailboxId, createdFrom, createdTo, page, size));
    }

    @Operation(summary = "工单详情")
    @GetMapping("/{id}")
    public BasicResult<TicketVO> getTicket(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id) {
        return BasicResult.ok(ticketBizService.getTicket(principal, id));
    }

    @Operation(summary = "工单统计概览")
    @GetMapping("/stats")
    public BasicResult<TicketBizService.TicketStats> stats() {
        return BasicResult.ok(ticketBizService.stats());
    }

    @Operation(summary = "分配处理人")
    @PostMapping("/{id}/assign")
    public BasicResult<TicketVO> assignTicket(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TicketAssignRequest request) {
        return BasicResult.ok(ticketBizService.assignTicket(principal, id, request));
    }

    @Operation(summary = "回复客户 / 内部备注（internal=true 为内部备注）")
    @PostMapping("/{id}/reply")
    public BasicResult<TicketVO> replyTicket(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TicketReplyRequest request) {
        return BasicResult.ok(ticketBizService.replyTicket(principal, id, request));
    }

    @Operation(summary = "变更工单状态")
    @PatchMapping("/{id}/status")
    public BasicResult<TicketVO> updateStatus(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TicketStatusRequest request) {
        return BasicResult.ok(ticketBizService.updateStatus(principal, id, request));
    }

    @Operation(summary = "关闭工单")
    @PostMapping("/{id}/close")
    public BasicResult<TicketVO> closeTicket(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @RequestBody(required = false) TicketStatusRequest request) {
        return BasicResult.ok(ticketBizService.closeTicket(principal, id, request));
    }

    @Operation(summary = "变更工单优先级")
    @PatchMapping("/{id}/priority")
    public BasicResult<TicketVO> updatePriority(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TicketPriorityRequest request) {
        return BasicResult.ok(ticketBizService.updatePriority(principal, id, request));
    }

    @Operation(summary = "更新工单备注")
    @PatchMapping("/{id}/remark")
    public BasicResult<TicketVO> updateRemark(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TicketRemarkRequest request) {
        return BasicResult.ok(ticketBizService.updateRemark(principal, id, request));
    }
}
