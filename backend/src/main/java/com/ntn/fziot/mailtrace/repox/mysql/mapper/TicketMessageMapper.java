package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TicketMessageMapper extends BaseMapper<TicketMessageEntity> {

    /**
     * 按 Message-ID 精确计数，不附加逻辑删除过滤。
     * 用于去重：软删除的记录也视为重复，避免唯一索引冲突。
     */
    @Select("SELECT COUNT(1) FROM mt_ticket_message WHERE message_id = #{messageId}")
    int countExistingByMessageId(@Param("messageId") String messageId);
}
