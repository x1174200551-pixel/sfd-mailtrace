package com.ntn.fziot.mailtrace.repox.mysql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mt_department")
public class DepartmentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String deptCode;

    private String deptName;

    private String deptDesc;

    private Long leaderUserId;

    private String deptPath;

    @TableField("is_enabled")
    private Boolean enabled;

    private Integer sortOrder;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
