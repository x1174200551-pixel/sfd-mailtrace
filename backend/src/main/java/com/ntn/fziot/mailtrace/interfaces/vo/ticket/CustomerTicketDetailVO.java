package com.ntn.fziot.mailtrace.interfaces.vo.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "客户可见工单详情")
public record CustomerTicketDetailVO(
        @Schema(description = "工单号") String ticketNo,
        @Schema(description = "工单主题") String subject,
        @Schema(description = "状态") String status,
        @Schema(description = "状态中文") String statusLabel,
        @Schema(description = "客户邮箱") String customerEmail,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt,
        @Schema(description = "首次响应时间") LocalDateTime firstReplyAt,
        @Schema(description = "关闭时间") LocalDateTime closedAt,
        @Schema(description = "首次响应SLA截止时间") LocalDateTime slaResponseDeadline,
        @Schema(description = "解决SLA截止时间") LocalDateTime slaResolveDeadline,
        @Schema(description = "是否已SLA超时") Boolean slaBreached,
        @Schema(description = "客户访问令牌有效期") LocalDateTime customerAccessExpiresAt,
        @Schema(description = "客户可见邮件摘要") CustomerTicketEmailVO email,
        @Schema(description = "客户可见邮件消息") List<CustomerTicketMessageVO> messages,
        @Schema(description = "客户可见处理时间线") List<CustomerTicketTimelineVO> timeline
) {
    @Schema(description = "客户可见邮件摘要")
    public record CustomerTicketEmailVO(
            @Schema(description = "发件人") String fromAddress,
            @Schema(description = "收件人") String toAddress,
            @Schema(description = "主题") String subject,
            @Schema(description = "发送时间") LocalDateTime sentAt,
            @Schema(description = "纯文本正文") String contentText,
            @Schema(description = "HTML正文") String contentHtml
    ) {
    }

    @Schema(description = "客户可见邮件消息")
    public record CustomerTicketMessageVO(
            @Schema(description = "方向：INBOUND/OUTBOUND") String direction,
            @Schema(description = "发件人") String fromAddress,
            @Schema(description = "收件人") String toAddress,
            @Schema(description = "主题") String subject,
            @Schema(description = "发送时间") LocalDateTime sentAt,
            @Schema(description = "纯文本正文") String contentText,
            @Schema(description = "HTML正文") String contentHtml
    ) {
    }

    @Schema(description = "客户可见处理时间线")
    public record CustomerTicketTimelineVO(
            @Schema(description = "客户侧阶段") String stage,
            @Schema(description = "标题") String title,
            @Schema(description = "内容") String content,
            @Schema(description = "标签") String badge,
            @Schema(description = "发生时间") LocalDateTime eventAt
    ) {
    }
}
