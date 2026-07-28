package com.ntn.fziot.mailtrace.repox.mysql.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("mt_work_calendar")
public class WorkCalendarEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String calendarName;

    private String timezone;

    private String workdays;

    private LocalTime workStartTime;

    private LocalTime workEndTime;

    @TableField("is_default")
    private Boolean defaultCalendar;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
