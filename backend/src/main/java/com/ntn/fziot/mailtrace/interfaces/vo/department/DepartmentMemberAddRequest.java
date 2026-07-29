package com.ntn.fziot.mailtrace.interfaces.vo.department;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "添加部门成员请求")
public class DepartmentMemberAddRequest {

    @NotEmpty(message = "请选择要添加的成员")
    @Schema(description = "用户ID列表")
    private List<Long> userIds;
}
