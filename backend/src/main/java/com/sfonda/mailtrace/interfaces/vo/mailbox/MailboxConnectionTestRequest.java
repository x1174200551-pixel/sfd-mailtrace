package com.sfonda.mailtrace.interfaces.vo.mailbox;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "邮箱连接测试请求")
public class MailboxConnectionTestRequest extends MailboxSaveRequest {

    @Schema(description = "测试类型：ALL/IMAP/SMTP", example = "ALL")
    private String testType = "ALL";
}
