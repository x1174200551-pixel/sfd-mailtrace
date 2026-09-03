package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.FeishuSendLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FeishuSendLogMapper extends BaseMapper<FeishuSendLogEntity> {

    @Select("SELECT * FROM mt_feishu_send_log "
            + "WHERE is_deleted = 0 AND send_status IN ('PENDING','FAILED') "
            + "AND (next_retry_at IS NULL OR next_retry_at <= #{now}) "
            + "ORDER BY id ASC LIMIT #{limit}")
    List<FeishuSendLogEntity> selectPendingForSend(@Param("now") LocalDateTime now,
                                                   @Param("limit") int limit);

    @Update("UPDATE mt_feishu_send_log SET send_status='SENDING', next_retry_at=#{leaseUntil}, updated_by='system' "
            + "WHERE id=#{id} AND is_deleted=0 AND send_status IN ('PENDING','FAILED')")
    int claimForSend(@Param("id") Long id, @Param("leaseUntil") LocalDateTime leaseUntil);

    @Update("UPDATE mt_feishu_send_log SET send_status='FAILED', next_retry_at=#{now}, "
            + "response_message='发送进程中断，任务已恢复', updated_by='system' "
            + "WHERE is_deleted=0 AND send_status='SENDING' "
            + "AND next_retry_at <= #{now}")
    int recoverStaleSending(@Param("now") LocalDateTime now);
}
