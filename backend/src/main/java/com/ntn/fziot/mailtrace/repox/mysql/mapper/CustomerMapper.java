package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.dto.CustomerReadonlyRow;
import com.ntn.fziot.mailtrace.repox.mysql.entity.CustomerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Mapper
public interface CustomerMapper extends BaseMapper<CustomerEntity> {

    /**
     * 收信建单时按“企业 + 邮箱”原子新增或更新客户，避免并发建单的唯一键竞争。
     * VALUES(...) 语法用于保持 MySQL 5.7 兼容。
     */
    @Insert("""
            INSERT INTO mt_customer (
              enterprise_id, email, display_name, last_mail_at, ticket_count, created_by, updated_by, is_deleted
            ) VALUES (
              #{enterpriseId}, #{email}, #{displayName}, #{lastMailAt}, 1, #{operator}, #{operator}, 0
            )
            ON DUPLICATE KEY UPDATE
              display_name = CASE
                WHEN VALUES(display_name) IS NULL OR VALUES(display_name) = '' THEN display_name
                ELSE VALUES(display_name)
              END,
              last_mail_at = CASE
                WHEN last_mail_at IS NULL OR VALUES(last_mail_at) > last_mail_at THEN VALUES(last_mail_at)
                ELSE last_mail_at
              END,
              ticket_count = COALESCE(ticket_count, 0) + 1,
              updated_by = VALUES(updated_by),
              is_deleted = 0
            """)
    int upsertIncomingCustomer(@Param("enterpriseId") Long enterpriseId,
                               @Param("email") String email,
                               @Param("displayName") String displayName,
                               @Param("lastMailAt") LocalDateTime lastMailAt,
                               @Param("operator") String operator);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT CONCAT(t.enterprise_id, ':', t.customer_email))
            FROM mt_ticket t
            WHERE t.is_deleted = 0
              AND t.mailbox_id IN
              <foreach collection="mailboxIds" item="mailboxId" open="(" separator="," close=")">#{mailboxId}</foreach>
              <if test="enterpriseId != null">AND t.enterprise_id = #{enterpriseId}</if>
              <if test="mailboxId != null">AND t.mailbox_id = #{mailboxId}</if>
              <if test="keyword != null and keyword != ''">
                  AND t.customer_email LIKE CONCAT('%', #{keyword}, '%')
              </if>
            </script>
            """)
    long countReadonlyCustomers(@Param("keyword") String keyword,
                                @Param("enterpriseId") Long enterpriseId,
                                @Param("mailboxId") Long mailboxId,
                                @Param("mailboxIds") Set<Long> mailboxIds);

    @Select("""
            <script>
            SELECT
                MAX(c.id) AS id,
                source.enterprise_id AS enterprise_id,
                source.email AS email,
                MAX(c.display_name) AS display_name,
                NULLIF(GREATEST(
                    COALESCE(MAX(c.last_mail_at), TIMESTAMP('1970-01-01 00:00:00')),
                    COALESCE(MAX(t.last_customer_mail_at), TIMESTAMP('1970-01-01 00:00:00')),
                    COALESCE(MAX(t.created_at), TIMESTAMP('1970-01-01 00:00:00'))
                ), TIMESTAMP('1970-01-01 00:00:00')) AS last_mail_at,
                COUNT(DISTINCT t.id) AS ticket_count,
                MAX(c.remark) AS remark,
                MIN(COALESCE(c.created_at, t.created_at)) AS created_at
            FROM (
                SELECT DISTINCT enterprise_id, customer_email AS email
                FROM mt_ticket
                WHERE is_deleted = 0
                  AND mailbox_id IN
                  <foreach collection="mailboxIds" item="mailboxId" open="(" separator="," close=")">#{mailboxId}</foreach>
                  <if test="enterpriseId != null">AND enterprise_id = #{enterpriseId}</if>
                  <if test="mailboxId != null">AND mailbox_id = #{mailboxId}</if>
            ) source
            LEFT JOIN mt_customer c
              ON c.is_deleted = 0
             AND c.enterprise_id = source.enterprise_id
             AND c.email = source.email
            LEFT JOIN mt_ticket t
              ON t.is_deleted = 0
             AND t.enterprise_id = source.enterprise_id
             AND t.customer_email = source.email
             AND t.mailbox_id IN
             <foreach collection="mailboxIds" item="mailboxId" open="(" separator="," close=")">#{mailboxId}</foreach>
            <where>
                <if test="keyword != null and keyword != ''">
                    AND source.email LIKE CONCAT('%', #{keyword}, '%')
                </if>
            </where>
            GROUP BY source.enterprise_id, source.email
            ORDER BY last_mail_at DESC, source.email ASC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<CustomerReadonlyRow> selectReadonlyCustomers(@Param("keyword") String keyword,
                                                      @Param("enterpriseId") Long enterpriseId,
                                                      @Param("mailboxId") Long mailboxId,
                                                      @Param("offset") long offset,
                                                      @Param("size") long size,
                                                      @Param("mailboxIds") Set<Long> mailboxIds);

    @Select("""
            <script>
            SELECT
                MAX(c.id) AS id,
                source.enterprise_id AS enterprise_id,
                source.email AS email,
                MAX(c.display_name) AS display_name,
                NULLIF(GREATEST(
                    COALESCE(MAX(c.last_mail_at), TIMESTAMP('1970-01-01 00:00:00')),
                    COALESCE(MAX(t.last_customer_mail_at), TIMESTAMP('1970-01-01 00:00:00')),
                    COALESCE(MAX(t.created_at), TIMESTAMP('1970-01-01 00:00:00'))
                ), TIMESTAMP('1970-01-01 00:00:00')) AS last_mail_at,
                COUNT(DISTINCT t.id) AS ticket_count,
                MAX(c.remark) AS remark,
                MIN(COALESCE(c.created_at, t.created_at)) AS created_at
            FROM (
                SELECT DISTINCT enterprise_id, customer_email AS email
                FROM mt_ticket
                WHERE is_deleted = 0
                  AND enterprise_id = #{enterpriseId}
                  AND customer_email = #{email}
                  AND mailbox_id IN
                  <foreach collection="mailboxIds" item="mailboxId" open="(" separator="," close=")">#{mailboxId}</foreach>
            ) source
            LEFT JOIN mt_customer c
              ON c.is_deleted = 0
             AND c.enterprise_id = source.enterprise_id
             AND c.email = source.email
            LEFT JOIN mt_ticket t
              ON t.is_deleted = 0
             AND t.enterprise_id = source.enterprise_id
             AND t.customer_email = source.email
             AND t.mailbox_id IN
             <foreach collection="mailboxIds" item="mailboxId" open="(" separator="," close=")">#{mailboxId}</foreach>
            GROUP BY source.enterprise_id, source.email
            LIMIT 1
            </script>
            """)
    CustomerReadonlyRow selectReadonlyCustomerByEmail(@Param("enterpriseId") Long enterpriseId,
                                                      @Param("email") String email,
                                                      @Param("mailboxIds") Set<Long> mailboxIds);
}
