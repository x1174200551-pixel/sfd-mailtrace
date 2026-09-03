package com.ntn.fziot.mailtrace.repox.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntn.fziot.mailtrace.repox.mysql.entity.SlaPolicyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SlaPolicyMapper extends BaseMapper<SlaPolicyEntity> {

    @Select("SELECT policy_name FROM mt_sla_policy WHERE id = #{id} LIMIT 1")
    String selectHistoricalNameById(@Param("id") Long id);
}
