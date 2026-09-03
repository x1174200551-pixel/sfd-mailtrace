package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.AssignmentRuleGroupEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AssignmentRuleGroupMapper extends BaseMapper<AssignmentRuleGroupEntity> {

    @Select("SELECT group_name FROM mt_assignment_rule_group WHERE id = #{id} LIMIT 1")
    String selectHistoricalNameById(@Param("id") Long id);
}
