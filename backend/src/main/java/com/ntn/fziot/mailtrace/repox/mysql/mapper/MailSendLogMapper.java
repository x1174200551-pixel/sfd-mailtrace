package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MailSendLogMapper extends BaseMapper<MailSendLogEntity> {

    @Select("SELECT COUNT(1) FROM mt_mail_send_log WHERE is_deleted = 0")
    long countTotal();

    @Select("SELECT COUNT(1) FROM mt_mail_send_log WHERE is_deleted = 0 AND send_status = 'SUCCESS'")
    long countSuccess();

    @Select("SELECT COUNT(1) FROM mt_mail_send_log WHERE is_deleted = 0 AND send_status = 'FAILED'")
    long countFail();

    @Select("SELECT COUNT(1) FROM mt_mail_send_log WHERE is_deleted = 0 AND send_status IN ('PENDING', 'FAILED', 'RETRYING')")
    long countPending();

    @Select("SELECT * FROM mt_mail_send_log WHERE is_deleted = 0 AND send_status = 'FAILED' AND retry_count < max_retry ORDER BY id ASC LIMIT #{limit}")
    List<MailSendLogEntity> selectFailedForRetry(@Param("limit") int limit);
}
