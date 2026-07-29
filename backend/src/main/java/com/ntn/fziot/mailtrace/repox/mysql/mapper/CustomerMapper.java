package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.dto.CustomerReadonlyRow;
import com.ntn.fziot.mailtrace.repox.mysql.entity.CustomerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface CustomerMapper extends BaseMapper<CustomerEntity> {

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM (
                <if test="allAccess">
                    SELECT email FROM mt_customer WHERE is_deleted = 0
                    UNION
                    SELECT customer_email AS email FROM mt_ticket WHERE is_deleted = 0
                </if>
                <if test="!allAccess">
                    SELECT DISTINCT customer_email AS email
                    FROM mt_ticket
                    WHERE is_deleted = 0
                      AND (
                          <choose>
                              <when test="scopeAssigneeIds != null and !scopeAssigneeIds.isEmpty()">
                                  assignee_id IN
                                  <foreach collection="scopeAssigneeIds" item="uid" open="(" separator="," close=")">#{uid}</foreach>
                                  OR assignee_id IS NULL
                              </when>
                              <otherwise>
                                  1 = 0
                              </otherwise>
                          </choose>
                      )
                </if>
            ) source
            <where>
                <if test="keyword != null and keyword != ''">
                    AND source.email LIKE CONCAT('%', #{keyword}, '%')
                </if>
            </where>
            </script>
            """)
    long countReadonlyCustomers(@Param("keyword") String keyword,
                                @Param("allAccess") boolean allAccess,
                                @Param("scopeAssigneeIds") Set<Long> scopeAssigneeIds,
                                @Param("currentUserId") Long currentUserId);

    @Select("""
            <script>
            SELECT
                MAX(c.id) AS id,
                source.email AS email,
                MAX(c.display_name) AS display_name,
                NULLIF(GREATEST(
                    COALESCE(MAX(c.last_mail_at), TIMESTAMP('1970-01-01 00:00:00')),
                    COALESCE(MAX(t.last_customer_mail_at), TIMESTAMP('1970-01-01 00:00:00')),
                    COALESCE(MAX(t.created_at), TIMESTAMP('1970-01-01 00:00:00'))
                ), TIMESTAMP('1970-01-01 00:00:00')) AS last_mail_at,
                COUNT(t.id) AS ticket_count,
                MAX(c.remark) AS remark,
                MIN(COALESCE(c.created_at, t.created_at)) AS created_at
            FROM (
                <if test="allAccess">
                    SELECT email FROM mt_customer WHERE is_deleted = 0
                    UNION
                    SELECT customer_email AS email FROM mt_ticket WHERE is_deleted = 0
                </if>
                <if test="!allAccess">
                    SELECT DISTINCT customer_email AS email
                    FROM mt_ticket
                    WHERE is_deleted = 0
                      AND (
                          <choose>
                              <when test="scopeAssigneeIds != null and !scopeAssigneeIds.isEmpty()">
                                  assignee_id IN
                                  <foreach collection="scopeAssigneeIds" item="uid" open="(" separator="," close=")">#{uid}</foreach>
                                  OR assignee_id IS NULL
                              </when>
                              <otherwise>
                                  1 = 0
                              </otherwise>
                          </choose>
                      )
                </if>
            ) source
            LEFT JOIN mt_customer c ON c.is_deleted = 0 AND c.email = source.email
            LEFT JOIN mt_ticket t ON t.is_deleted = 0 AND t.customer_email = source.email
                <if test="!allAccess">
                    AND (
                        <choose>
                            <when test="scopeAssigneeIds != null and !scopeAssigneeIds.isEmpty()">
                                t.assignee_id IN
                                <foreach collection="scopeAssigneeIds" item="uid" open="(" separator="," close=")">#{uid}</foreach>
                                OR t.assignee_id IS NULL
                            </when>
                            <otherwise>
                                1 = 0
                            </otherwise>
                        </choose>
                    )
                </if>
            <where>
                <if test="keyword != null and keyword != ''">
                    AND source.email LIKE CONCAT('%', #{keyword}, '%')
                </if>
            </where>
            GROUP BY source.email
            ORDER BY last_mail_at DESC, source.email ASC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<CustomerReadonlyRow> selectReadonlyCustomers(@Param("keyword") String keyword,
                                                      @Param("offset") long offset,
                                                      @Param("size") long size,
                                                      @Param("allAccess") boolean allAccess,
                                                      @Param("scopeAssigneeIds") Set<Long> scopeAssigneeIds,
                                                      @Param("currentUserId") Long currentUserId);

    @Select("""
            <script>
            SELECT
                MAX(c.id) AS id,
                source.email AS email,
                MAX(c.display_name) AS display_name,
                NULLIF(GREATEST(
                    COALESCE(MAX(c.last_mail_at), TIMESTAMP('1970-01-01 00:00:00')),
                    COALESCE(MAX(t.last_customer_mail_at), TIMESTAMP('1970-01-01 00:00:00')),
                    COALESCE(MAX(t.created_at), TIMESTAMP('1970-01-01 00:00:00'))
                ), TIMESTAMP('1970-01-01 00:00:00')) AS last_mail_at,
                COUNT(t.id) AS ticket_count,
                MAX(c.remark) AS remark,
                MIN(COALESCE(c.created_at, t.created_at)) AS created_at
            FROM (
                <if test="allAccess">
                    SELECT email FROM mt_customer WHERE is_deleted = 0 AND email = #{email}
                    UNION
                    SELECT customer_email AS email FROM mt_ticket WHERE is_deleted = 0 AND customer_email = #{email}
                </if>
                <if test="!allAccess">
                    SELECT customer_email AS email
                    FROM mt_ticket
                    WHERE is_deleted = 0
                      AND customer_email = #{email}
                      AND (
                          <choose>
                              <when test="scopeAssigneeIds != null and !scopeAssigneeIds.isEmpty()">
                                  assignee_id IN
                                  <foreach collection="scopeAssigneeIds" item="uid" open="(" separator="," close=")">#{uid}</foreach>
                                  OR assignee_id IS NULL
                              </when>
                              <otherwise>
                                  1 = 0
                              </otherwise>
                          </choose>
                      )
                </if>
            ) source
            LEFT JOIN mt_customer c ON c.is_deleted = 0 AND c.email = source.email
            LEFT JOIN mt_ticket t ON t.is_deleted = 0 AND t.customer_email = source.email
                <if test="!allAccess">
                    AND (
                        <choose>
                            <when test="scopeAssigneeIds != null and !scopeAssigneeIds.isEmpty()">
                                t.assignee_id IN
                                <foreach collection="scopeAssigneeIds" item="uid" open="(" separator="," close=")">#{uid}</foreach>
                                OR t.assignee_id IS NULL
                            </when>
                            <otherwise>
                                1 = 0
                            </otherwise>
                        </choose>
                    )
                </if>
            GROUP BY source.email
            LIMIT 1
            </script>
            """)
    CustomerReadonlyRow selectReadonlyCustomerByEmail(@Param("email") String email,
                                                      @Param("allAccess") boolean allAccess,
                                                      @Param("scopeAssigneeIds") Set<Long> scopeAssigneeIds,
                                                      @Param("currentUserId") Long currentUserId);
}
