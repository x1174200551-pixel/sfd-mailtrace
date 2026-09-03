package com.ntn.fziot.mailtrace.repox.mysql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mt_sla_policy")
public class SlaPolicyEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long enterpriseId;

    private String policyName;

    @TableField("is_enabled")
    private Boolean enabled;

    @TableField("is_default")
    private Boolean defaultPolicy;

    private Integer responseHours;

    private Integer resolveHours;

    private Integer warningRemainHours;

    private Integer escalateAfterBreachHours;

    private Boolean responseWarningNotifyEnabled;

    private Boolean responseBreachNotifyEnabled;

    private Boolean responseEscalationNotifyEnabled;

    private Boolean resolveWarningNotifyEnabled;

    private Boolean resolveBreachNotifyEnabled;

    private Boolean resolveEscalationNotifyEnabled;

    private Long calendarId;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
