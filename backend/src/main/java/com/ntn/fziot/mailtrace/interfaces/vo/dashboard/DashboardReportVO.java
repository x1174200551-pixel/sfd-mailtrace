package com.ntn.fziot.mailtrace.interfaces.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "工作台报表")
public record DashboardReportVO(
        @Schema(description = "今日处理效率") Efficiency efficiency,
        @Schema(description = "待办优先级分布") PriorityDistribution priorityDistribution,
        @Schema(description = "SLA 健康度") SlaHealth slaHealth,
        @Schema(description = "邮件处理链路") MailFlow mailFlow,
        @Schema(description = "处理建议") ActionPanel actionPanel,
        @Schema(description = "处理人负载") List<AssigneeLoad> assigneeLoads,
        @Schema(description = "数据质量检查") List<QualityCheck> qualityChecks
) {

    @Schema(description = "效率报表")
    public record Efficiency(
            @Schema(description = "完成率") int completionRate,
            @Schema(description = "效率指标") List<MetricItem> items
    ) {
    }

    @Schema(description = "优先级分布")
    public record PriorityDistribution(
            @Schema(description = "最大值，用于前端图表比例") long maxValue,
            @Schema(description = "分布项") List<ChartItem> items
    ) {
    }

    @Schema(description = "SLA 健康度")
    public record SlaHealth(
            @Schema(description = "状态文案") String statusText,
            @Schema(description = "状态色调") String tone,
            @Schema(description = "SLA 指标") List<MetricItem> items
    ) {
    }

    @Schema(description = "邮件处理链路")
    public record MailFlow(
            @Schema(description = "状态文案") String statusText,
            @Schema(description = "状态色调") String tone,
            @Schema(description = "链路指标") List<FlowItem> items
    ) {
    }

    @Schema(description = "处理建议面板")
    public record ActionPanel(
            @Schema(description = "标签文案") String tagText,
            @Schema(description = "标签色调") String tone,
            @Schema(description = "建议项") List<ActionItem> items
    ) {
    }

    @Schema(description = "普通指标")
    public record MetricItem(
            @Schema(description = "名称") String label,
            @Schema(description = "值") String value,
            @Schema(description = "说明") String detail,
            @Schema(description = "色调") String tone
    ) {
    }

    @Schema(description = "图表指标")
    public record ChartItem(
            @Schema(description = "名称") String label,
            @Schema(description = "值") long value,
            @Schema(description = "色调") String tone
    ) {
    }

    @Schema(description = "邮件链路指标")
    public record FlowItem(
            @Schema(description = "名称") String label,
            @Schema(description = "值") String value,
            @Schema(description = "说明") String detail,
            @Schema(description = "图标键") String iconKey,
            @Schema(description = "色调") String tone
    ) {
    }

    @Schema(description = "处理建议")
    public record ActionItem(
            @Schema(description = "名称") String label,
            @Schema(description = "说明") String detail,
            @Schema(description = "数量") long value,
            @Schema(description = "色调") String tone,
            @Schema(description = "图标键") String iconKey,
            @Schema(description = "跳转菜单") String targetMenu,
            @Schema(description = "工单状态筛选") String ticketStatus,
            @Schema(description = "是否仅筛选 SLA 超时") boolean slaBreachedOnly
    ) {
    }

    @Schema(description = "处理人负载")
    public record AssigneeLoad(
            @Schema(description = "处理人名称") String name,
            @Schema(description = "说明") String detail,
            @Schema(description = "待办数量") long value,
            @Schema(description = "是否有超时") boolean overdue
    ) {
    }

    @Schema(description = "数据质量检查")
    public record QualityCheck(
            @Schema(description = "名称") String label,
            @Schema(description = "说明") String detail,
            @Schema(description = "数量") long value,
            @Schema(description = "色调") String tone,
            @Schema(description = "图标键") String iconKey,
            @Schema(description = "跳转菜单") String targetMenu,
            @Schema(description = "工单状态筛选") String ticketStatus,
            @Schema(description = "是否仅筛选 SLA 超时") boolean slaBreachedOnly
    ) {
    }
}
