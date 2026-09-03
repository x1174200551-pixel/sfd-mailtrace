package com.ntn.fziot.mailtrace.repox.mysql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mt_enterprise")
public class EnterpriseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String enterpriseName;

    private String contactName;

    private String contactEmail;

    private String contactPhone;

    @TableField("is_enabled")
    private Boolean enabled;

    private Boolean feishuNotifyEnabled;

    private String feishuGroupName;

    private String feishuWebhookUrl;

    private String feishuSigningSecret;

    private Integer feishuConfigVersion;

    private String feishuConnectionStatus;

    private LocalDateTime feishuLastTestAt;

    private String feishuLastError;

    private String remark;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
