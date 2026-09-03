package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.AssignmentRuleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AssignmentRuleMapper extends BaseMapper<AssignmentRuleEntity> {

    @Select("SELECT rule_name FROM mt_assignment_rule WHERE id = #{id} LIMIT 1")
    String selectHistoricalNameById(@Param("id") Long id);
}
