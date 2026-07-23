package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailFetchLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MailFetchLogMapper extends BaseMapper<MailFetchLogEntity> {

    /**
     * 统计总拉取次数（不含逻辑删除）。
     */
    @Select("SELECT COUNT(1) FROM mt_mail_fetch_log WHERE is_deleted = 0")
    long countTotal();

    /**
     * 统计成功拉取次数。
     */
    @Select("SELECT COUNT(1) FROM mt_mail_fetch_log WHERE is_deleted = 0 AND success = 1")
    long countSuccess();

    /**
     * 统计失败拉取次数。
     */
    @Select("SELECT COUNT(1) FROM mt_mail_fetch_log WHERE is_deleted = 0 AND success = 0")
    long countFail();

    /**
     * 统计新建工单总数。
     */
    @Select("SELECT COALESCE(SUM(created_ticket_count), 0) FROM mt_mail_fetch_log WHERE is_deleted = 0")
    long sumCreatedTicketCount();
}
