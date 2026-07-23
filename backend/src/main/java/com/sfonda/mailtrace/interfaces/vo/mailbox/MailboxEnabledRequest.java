package com.sfonda.mailtrace.interfaces.vo.mailbox;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "邮箱启停请求")
public class MailboxEnabledRequest {

    @NotNull(message = "请选择启用状态")
    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;
}
