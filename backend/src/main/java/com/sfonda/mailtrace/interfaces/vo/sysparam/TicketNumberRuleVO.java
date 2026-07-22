package com.sfonda.mailtrace.interfaces.vo.sysparam;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "工单编号规则")
public record TicketNumberRuleVO(
        @Schema(description = "是否启用") Boolean enabled,
        @Schema(description = "工单前缀") String prefix,
        @Schema(description = "日期格式") String dateFormat,
        @Schema(description = "流水位数") Integer seqLength,
        @Schema(description = "分隔符") String separator,
        @Schema(description = "业务说明") String description,
        @Schema(description = "当前日期") String todayDate,
        @Schema(description = "当前日期维度") String dateKey,
        @Schema(description = "今日已用流水") Integer usedSeq,
        @Schema(description = "下一流水") String nextSeq,
        @Schema(description = "下一工单号") String nextTicketNo,
        @Schema(description = "主题匹配样例") String subjectPreview,
        @Schema(description = "最后更新时间") LocalDateTime updatedAt
) {
}
