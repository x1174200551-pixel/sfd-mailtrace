package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.MailSendLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MailSendLogMapper extends BaseMapper<MailSendLogEntity> {

    @Select("SELECT COUNT(1) FROM mt_mail_send_log WHERE is_deleted = 0")
    long countTotal();

    @Select("SELECT COUNT(1) FROM mt_mail_send_log WHERE is_deleted = 0 AND send_status = 'SUCCESS'")
    long countSuccess();

    @Select("SELECT COUNT(1) FROM mt_mail_send_log WHERE is_deleted = 0 AND send_status = 'FAILED'")
    long countFail();

    @Select("SELECT COUNT(1) FROM mt_mail_send_log WHERE is_deleted = 0 AND send_status IN ('PENDING', 'FAILED', 'SENDING', 'RETRYING', 'DELIVERY_UNKNOWN')")
    long countPending();

    @Select("SELECT * FROM mt_mail_send_log WHERE is_deleted = 0 AND send_status = 'FAILED' AND retry_count < max_retry ORDER BY id ASC LIMIT #{limit}")
    List<MailSendLogEntity> selectFailedForRetry(@Param("limit") int limit);

    @Select("SELECT * FROM mt_mail_send_log WHERE is_deleted = 0 AND send_status = 'PENDING' " +
            "AND created_at < #{before} ORDER BY id ASC LIMIT #{limit}")
    List<MailSendLogEntity> selectPendingForDispatch(@Param("before") java.time.LocalDateTime before,
                                                     @Param("limit") int limit);

    @Update("UPDATE mt_mail_send_log SET send_status = 'SENDING', updated_by = 'SYSTEM', " +
            "updated_at = CURRENT_TIMESTAMP(3) WHERE id = #{id} AND is_deleted = 0 AND send_status = 'PENDING'")
    int claimPendingForSend(@Param("id") Long id);

    @Update("UPDATE mt_mail_send_log SET send_status = 'RETRYING', retry_count = retry_count + 1, " +
            "updated_by = 'SYSTEM', updated_at = CURRENT_TIMESTAMP(3) " +
            "WHERE id = #{id} AND is_deleted = 0 AND send_status = 'FAILED' AND retry_count < max_retry")
    int claimFailedForRetry(@Param("id") Long id);

    @Update("UPDATE mt_mail_send_log SET send_status = 'SUCCESS', sent_at = #{sentAt}, " +
            "next_retry_at = NULL, error_message = NULL, updated_by = 'SYSTEM', updated_at = CURRENT_TIMESTAMP(3) " +
            "WHERE id = #{id} AND is_deleted = 0 AND send_status IN ('SENDING', 'RETRYING')")
    int markDeliverySuccess(@Param("id") Long id, @Param("sentAt") java.time.LocalDateTime sentAt);

    @Update("UPDATE mt_mail_send_log SET send_status = 'FAILED', error_message = #{errorMessage}, " +
            "updated_by = 'SYSTEM', updated_at = CURRENT_TIMESTAMP(3) " +
            "WHERE id = #{id} AND is_deleted = 0 AND send_status IN ('SENDING', 'RETRYING')")
    int markDeliveryFailed(@Param("id") Long id, @Param("errorMessage") String errorMessage);

    @Update("UPDATE mt_mail_send_log SET send_status = 'DELIVERY_UNKNOWN', error_message = #{errorMessage}, " +
            "updated_by = 'SYSTEM', updated_at = CURRENT_TIMESTAMP(3) " +
            "WHERE id = #{id} AND is_deleted = 0 AND send_status IN ('SENDING', 'RETRYING')")
    int markDeliveryUnknown(@Param("id") Long id, @Param("errorMessage") String errorMessage);

    @Update("UPDATE mt_mail_send_log SET send_status = 'CANCELLED', next_retry_at = NULL, " +
            "error_message = #{reason}, updated_by = 'SYSTEM', updated_at = CURRENT_TIMESTAMP(3) " +
            "WHERE id = #{id} AND is_deleted = 0 AND send_status IN ('PENDING', 'FAILED', 'SENDING', 'RETRYING')")
    int markDeliveryCancelled(@Param("id") Long id, @Param("reason") String reason);

    @Update("UPDATE mt_mail_send_log SET send_status = 'DELIVERY_UNKNOWN', " +
            "error_message = 'SMTP投递结果未知，已停止自动重试，请人工核实', updated_by = 'SYSTEM', " +
            "updated_at = CURRENT_TIMESTAMP(3) WHERE is_deleted = 0 " +
            "AND send_status IN ('SENDING', 'RETRYING') AND updated_at < #{before}")
    int markStaleDeliveriesUnknown(@Param("before") java.time.LocalDateTime before);

    @Select("SELECT l.* FROM mt_mail_send_log l INNER JOIN mt_ticket_message m ON m.id = l.ticket_message_id " +
            "WHERE l.is_deleted = 0 AND m.is_deleted = 0 AND l.send_status = 'SUCCESS' " +
            "AND l.send_type = 'AGENT_REPLY' AND m.send_status = 'SUCCESS' " +
            "AND m.delivery_completed_at IS NULL ORDER BY l.id ASC LIMIT #{limit}")
    List<MailSendLogEntity> selectReplyCompletionPending(@Param("limit") int limit);
}
