package com.ntn.fziot.mailtrace.interfaces.vo.department;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "移动部门请求")
public class DepartmentMoveRequest {

    @Schema(description = "新的父部门ID，移动为顶级部门时为空")
    private Long parentId;
}
