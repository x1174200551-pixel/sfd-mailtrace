package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.NotificationTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NotificationTemplateMapper extends BaseMapper<NotificationTemplateEntity> {

    @Select("SELECT template_name FROM mt_notification_template WHERE id = #{id} LIMIT 1")
    String selectHistoricalNameById(@Param("id") Long id);
}
