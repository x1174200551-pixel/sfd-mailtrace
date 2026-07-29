package com.ntn.fziot.mailtrace.interfaces.vo.department;

import com.ntn.fziot.mailtrace.interfaces.vo.user.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "部门成员分页响应")
public record DepartmentMemberPageResponse(
        @Schema(description = "成员列表") List<UserVO> records,
        @Schema(description = "总条数") long total,
        @Schema(description = "当前页") long page,
        @Schema(description = "每页条数") long size,
        @Schema(description = "总页数") long pages
) {
}
