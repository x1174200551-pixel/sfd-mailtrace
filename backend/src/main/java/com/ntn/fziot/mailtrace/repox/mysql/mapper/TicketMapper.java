package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.TicketEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TicketMapper extends BaseMapper<TicketEntity> {

    @Select("SELECT COUNT(1) FROM mt_ticket WHERE is_deleted = 0")
    long countActiveTotal();

    @Select("SELECT COUNT(1) FROM mt_ticket WHERE is_deleted = 0 AND status = #{status}")
    long countByStatus(String status);

    @Select("SELECT COUNT(1) FROM mt_ticket WHERE is_deleted = 0 AND sla_breached = 1")
    long countSlaOverdue();

    @Select("SELECT COUNT(1) FROM mt_ticket WHERE is_deleted = 0 AND DATE(created_at) = CURDATE() AND status = 'CLOSED'")
    long countClosedToday();

    @Select("SELECT * FROM mt_ticket WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    TicketEntity selectByIdForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM mt_ticket "
            + "WHERE is_deleted = 0 AND sla_notification_suppressed = 0 "
            + "AND status <> 'CANCELLED' AND ("
            + "(sla_response_deadline IS NOT NULL AND ("
            + "(first_reply_at IS NULL AND closed_at IS NULL AND ("
            + "sla_response_warning_at IS NULL "
            + "OR (sla_response_warning_at <= #{now} AND sla_response_warning_triggered_at IS NULL) "
            + "OR (sla_response_deadline <= #{now} AND sla_response_breach_triggered_at IS NULL) "
            + "OR (sla_response_escalation_at IS NOT NULL AND sla_response_escalation_at <= #{now} "
            + "AND sla_response_breach_triggered_at IS NOT NULL AND sla_response_escalation_triggered_at IS NULL))) "
            + "OR (COALESCE(first_reply_at, closed_at) > sla_response_deadline "
            + "AND sla_response_breach_triggered_at IS NULL))) "
            + "OR (sla_resolve_deadline IS NOT NULL AND ("
            + "(closed_at IS NULL AND ("
            + "sla_resolve_warning_at IS NULL "
            + "OR (sla_resolve_warning_at <= #{now} AND sla_resolve_warning_triggered_at IS NULL) "
            + "OR (sla_resolve_deadline <= #{now} AND sla_resolve_breach_triggered_at IS NULL) "
            + "OR (sla_resolve_escalation_at IS NOT NULL AND sla_resolve_escalation_at <= #{now} "
            + "AND sla_resolve_breach_triggered_at IS NOT NULL AND sla_resolve_escalation_triggered_at IS NULL))) "
            + "OR (closed_at > sla_resolve_deadline AND sla_resolve_breach_triggered_at IS NULL)))) "
            + "ORDER BY id ASC")
    List<TicketEntity> selectSlaCandidates(@Param("now") LocalDateTime now);
}
