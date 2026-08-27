package com.ntn.fziot.mailtrace.interfaces.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "用户数据授权详情")
public record UserDataGrantDetailVO(
        @Schema(description = "用户ID") Long userId,
        @Schema(description = "是否因管理员角色而全部数据可见") Boolean allDataVisible,
        @Schema(description = "企业和单邮箱授权明细") List<UserDataGrantVO> grants
) {
}
