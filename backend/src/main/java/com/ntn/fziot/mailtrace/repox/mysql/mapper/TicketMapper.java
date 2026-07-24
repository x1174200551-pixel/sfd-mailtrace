package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TicketMapper extends BaseMapper<TicketEntity> {

    @Select("SELECT COUNT(1) FROM mt_ticket WHERE is_deleted = 0 AND status = #{status}")
    long countByStatus(String status);

    @Select("SELECT COUNT(1) FROM mt_ticket WHERE is_deleted = 0 AND sla_breached = 1")
    long countSlaOverdue();

    @Select("SELECT COUNT(1) FROM mt_ticket WHERE is_deleted = 0 AND DATE(created_at) = CURDATE() AND status = 'CLOSED'")
    long countClosedToday();
}
