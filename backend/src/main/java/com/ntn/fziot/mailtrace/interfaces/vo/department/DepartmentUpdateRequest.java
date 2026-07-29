package com.ntn.fziot.mailtrace.interfaces.vo.department;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartmentUpdateRequest {

    @NotBlank(message = "请输入部门名称")
    @Size(max = 128, message = "部门名称最多128个字符")
    @Schema(description = "部门名称", example = "客服部")
    private String deptName;

    @Size(max = 512, message = "部门说明最多512个字符")
    @Schema(description = "部门说明")
    private String deptDesc;

    @Schema(description = "负责人用户ID")
    private Long leaderUserId;

    @Schema(description = "是否启用")
    private Boolean enabled = true;

    @Schema(description = "排序值")
    private Integer sortOrder;
}
