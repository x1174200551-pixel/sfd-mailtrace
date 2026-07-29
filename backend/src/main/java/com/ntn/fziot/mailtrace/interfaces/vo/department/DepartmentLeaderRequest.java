package com.ntn.fziot.mailtrace.interfaces.vo.department;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "设置部门负责人请求")
public class DepartmentLeaderRequest {

    @NotNull(message = "请选择负责人")
    @Schema(description = "负责人用户ID")
    private Long leaderUserId;
}
