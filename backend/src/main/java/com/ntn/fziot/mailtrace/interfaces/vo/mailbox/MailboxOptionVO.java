package com.ntn.fziot.mailtrace.interfaces.vo.mailbox;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "邮箱选项")
public record MailboxOptionVO(
        @Schema(description = "邮箱ID") Long id,
        @Schema(description = "所属企业ID") Long enterpriseId,
        @Schema(description = "邮箱名称") String mailboxName,
        @Schema(description = "邮箱地址") String emailAddress,
        @Schema(description = "是否启用") Boolean enabled
) {
}
