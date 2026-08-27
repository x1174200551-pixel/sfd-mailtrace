package com.ntn.fziot.mailtrace.interfaces.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@Schema(description = "用户企业和邮箱数据授权保存请求")
public class UserDataGrantSaveRequest {

    @Schema(description = "企业授权ID；授权企业时自动包含该企业现有及未来新增邮箱")
    private Set<Long> enterpriseIds = new LinkedHashSet<>();

    @Schema(description = "单邮箱授权ID")
    private Set<Long> mailboxIds = new LinkedHashSet<>();
}
