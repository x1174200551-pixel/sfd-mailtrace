package com.ntn.fziot.mailtrace.interfaces.vo.sysparam;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "工单编号规则")
public record TicketNumberRuleVO(
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "工单前缀") String prefix,
        @Schema(description = "日期格式") String dateFormat,
        @Schema(description = "随机数位数") Integer seqLength,
        @Schema(description = "分隔符") String separator,
        @Schema(description = "业务说明") String description,
        @Schema(description = "当前日期") String todayDate,
        @Schema(description = "日期片段预览") String dateKey,
        @Schema(description = "保留字段，随机数规则下固定为0") Integer usedSeq,
        @Schema(description = "随机数预览") String nextSeq,
        @Schema(description = "工单号预览") String nextTicketNo,
        @Schema(description = "主题匹配样例") String subjectPreview,
        @Schema(description = "最后更新时间") LocalDateTime updatedAt
) {
}
