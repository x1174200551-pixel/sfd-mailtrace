package com.ntn.fziot.mailtrace.interfaces.vo.assignment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "分配规则排序请求")
public record AssignmentRuleSortRequest(
        @Valid
        @NotEmpty(message = "排序规则不能为空")
        @Schema(description = "排序项列表") List<AssignmentRuleSortItem> rules
) {
}
