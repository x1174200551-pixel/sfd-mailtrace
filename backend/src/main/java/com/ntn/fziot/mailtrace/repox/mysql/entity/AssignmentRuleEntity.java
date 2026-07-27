package com.ntn.fziot.mailtrace.repox.mysql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mt_assignment_rule")
public class AssignmentRuleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleName;

    @TableField("is_enabled")
    private Boolean enabled;

    private Integer priorityOrder;

    @TableField("is_default")
    private Boolean defaultRule;

    private String matchType;

    private String matchValue;

    private Long assigneeId;

    private Boolean notifyEnabled;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
